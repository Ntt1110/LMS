package com.example.LMS.service;

import com.example.LMS.dto.request.SemesterCreateRequest;
import com.example.LMS.dto.request.SemesterListRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.LMS.dto.response.SemesterDetailResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;

    // ============================================================
    // DANH SÁCH HỌC KỲ có filter + phân trang
    // ============================================================
    // Đổi method có phân trang trả về SemesterDetailResponse
    public Page<SemesterDetailResponse> getSemesters(SemesterListRequest request) {
        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Specification<Semester> spec = buildSpecification(request);

        return semesterRepository.findAll(spec, pageable)
                .map(SemesterDetailResponse::fromEntity); // <-- đổi ở đây
    }

    // ============================================================
    // DANH SÁCH HỌC KỲ dùng cho dropdown (không phân trang)
    // ============================================================
    public List<SemesterResponse> getSemesters() {
        return semesterRepository.findAllByDeletedAtIsNullOrderByAcademicYearDescSemesterCodeAsc()
                .stream()
                .map(SemesterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ============================================================
    // XEM CHI TIẾT HỌC KỲ
    // ============================================================
    public SemesterResponse getSemesterById(Long id) {
        Semester semester = semesterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy học kỳ với id: " + id
                ));
        return SemesterResponse.fromEntity(semester);
    }

    // ============================================================
    // TẠO HỌC KỲ (SEMESTER_CREATE)
    // ============================================================
    @Transactional
    public SemesterResponse createSemester(SemesterCreateRequest request) {

        if (semesterRepository.existsBySemesterCode(request.getSemesterCode().toUpperCase().trim())) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "Mã học kỳ '" + request.getSemesterCode() + "' đã tồn tại");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }

        Semester semester = Semester.builder()
                .semesterCode(request.getSemesterCode().toUpperCase().trim())
                .academicYear(request.getAcademicYear().trim())
                .semesterNumber(request.getSemesterNumber())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Semester.SemesterStatus.ACTIVE)
                .build();

        return SemesterResponse.fromEntity(semesterRepository.save(semester));
    }

    // ============================================================
    // SPECIFICATION (dynamic filter)
    // ============================================================
    private Specification<Semester> buildSpecification(SemesterListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("semesterCode")), pattern),
                        cb.like(cb.lower(root.get("academicYear")), pattern)
                ));
            }

            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    Semester.SemesterStatus status =
                            Semester.SemesterStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
                predicates.add(cb.equal(root.get("academicYear"), request.getAcademicYear()));
            }

            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}