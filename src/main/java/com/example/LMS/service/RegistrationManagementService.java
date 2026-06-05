package com.example.LMS.service;

import com.example.LMS.dto.request.RegistrationPeriodListRequest;
import com.example.LMS.dto.request.RegistrationPeriodRequestDto;
import com.example.LMS.dto.response.ClassPendingResponse;
import com.example.LMS.dto.response.RegistrationPeriodResponse;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.Enum.RegistrationStatus;
import com.example.LMS.entity.model.RegistrationPeriod;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassEntityRepository;
import com.example.LMS.repository.CourseRepository;
import com.example.LMS.repository.RegistrationPeriodRepository;
import com.example.LMS.repository.SemesterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationManagementService {

    private final RegistrationPeriodRepository periodRepository;
    private final ClassEntityRepository classRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper;
    // ============================================================
    // DANH SÁCH ĐỢT ĐĂNG KÝ
    // ============================================================
    public Page<RegistrationPeriodResponse> getRegistrationPeriods(RegistrationPeriodListRequest request) {

        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<RegistrationPeriod> spec = (root, query, cb) -> {
            query.distinct(true);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (request.getSemesterId() != null) {
                predicates.add(cb.equal(root.get("semester").get("id"), request.getSemesterId()));
            }

            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    RegistrationStatus status = RegistrationStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {}
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return periodRepository.findAll(spec, pageable)
                .map(RegistrationPeriodResponse::fromEntity);
    }

    // ============================================================
    // DANH SÁCH HỌC PHẦN PENDING THEO HỌC KỲ VÀ KHOA
    // ============================================================
    public List<ClassPendingResponse> getPendingClassesBySemesterAndDepartment(
            Long semesterId, Long departmentId) {

        return classRepository.findPendingBySemesterAndDepartment(semesterId, departmentId)
                .stream()
                .map(c -> {
                    var course = courseRepository.findById(c.getCourseId()).orElse(null);
                    return ClassPendingResponse.fromEntity(c, course);
                })
                .collect(Collectors.toList());
    }

    // ============================================================
// TẠO ĐỢT ĐĂNG KÝ
// ============================================================
    @Transactional
    public RegistrationPeriodResponse createRegistrationPeriod(RegistrationPeriodRequestDto dto) {
        log.info("⏳ Tạo đợt đăng ký: {}", dto.getName());

        // Validate thời gian
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu không thể nằm sau thời gian kết thúc!");
        }

        // Kiểm tra học kỳ tồn tại
        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Học kỳ áp dụng không tồn tại!"));

        // Tính trạng thái tự động
        LocalDateTime now = LocalDateTime.now();
        RegistrationStatus calculatedStatus = RegistrationStatus.PENDING;
        if (now.isAfter(dto.getStartTime()) && now.isBefore(dto.getEndTime())) {
            calculatedStatus = RegistrationStatus.ACTIVE;
        } else if (now.isAfter(dto.getEndTime())) {
            calculatedStatus = RegistrationStatus.CLOSED;
        }

        // Convert list sang JSON
        String cohortsJson = "[]";
        String departmentsJson = "[]";
        try {
            cohortsJson = objectMapper.writeValueAsString(dto.getTargetCohorts());
            departmentsJson = objectMapper.writeValueAsString(dto.getTargetDepartments());
        } catch (JsonProcessingException e) {
            log.error("Lỗi convert JSON: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi hệ thống khi xử lý dữ liệu Khóa/Khoa!");
        }

        // Lưu đợt đăng ký
        RegistrationPeriod period = RegistrationPeriod.builder()
                .semester(semester)
                .name(dto.getName())
                .type(dto.getType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .targetCohorts(cohortsJson)
                .targetDepartments(departmentsJson)
                .status(calculatedStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RegistrationPeriod saved = periodRepository.save(period);

        // Nếu ACTIVE thì kích hoạt các lớp PENDING sang REGISTRATION
        if (calculatedStatus == RegistrationStatus.ACTIVE) {
            activateClassesForRegistration(semester.getId(), saved.getId());
        }

        log.info("✅ Tạo đợt đăng ký thành công: {}", saved.getId());
        return RegistrationPeriodResponse.fromEntity(saved);
    }

    // ============================================================
// HELPER: Kích hoạt lớp học phần sang REGISTRATION
// ============================================================
    private void activateClassesForRegistration(Long semesterId, Long periodId) {
        var pendingClasses = classRepository.findBySemesterIdAndStatus(semesterId, ClassStatus.PENDING);
        if (pendingClasses != null && !pendingClasses.isEmpty()) {
            for (var clazz : pendingClasses) {
                clazz.setStatus(ClassStatus.REGISTRATION);
                clazz.setRegistrationPeriodId(periodId);
                clazz.setUpdatedAt(LocalDateTime.now());
            }
            classRepository.saveAll(pendingClasses);
            log.info("✅ Kích hoạt {} lớp học phần sang REGISTRATION!", pendingClasses.size());
        }
    }
}