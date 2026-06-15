package com.example.LMS.service;

import com.example.LMS.dto.request.SemesterCreateRequest;
import com.example.LMS.dto.request.SemesterListRequest;
import com.example.LMS.dto.request.SemesterUpdateRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassEntityRepository;
import com.example.LMS.repository.SemesterRepository;
import com.example.LMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.LMS.dto.response.SemesterDetailResponse;

import java.util.List;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ClassEntityRepository classRepository;

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
                .collect(toList());
    }

    // ============================================================
    // XEM CHI TIẾT HỌC KỲ
    // ============================================================
    // XEM CHI TIẾT HỌC KỲ — trả về SemesterDetailResponse
// ============================================================
    public SemesterDetailResponse getSemesterById(Long id) {
        Semester semester = semesterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy học kỳ với id: " + id
                ));
        return SemesterDetailResponse.fromEntity(semester);
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


    // CẬP NHẬT HỌC KỲ
// PUT /api/v1/semesters/{id}
// Không cho sửa semesterCode, status
// Không cho sửa nếu đã CLOSED
// ============================================================
    @Transactional
    public SemesterDetailResponse updateSemester(Long id, SemesterUpdateRequest request) {

        Semester semester = semesterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ!"));

        // Không cho cập nhật học kỳ đã đóng
        if (semester.getStatus() == Semester.SemesterStatus.CLOSED) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Học kỳ đã CLOSED, không thể cập nhật!");
        }

        // Validate ngày
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải sau ngày bắt đầu!");
        }

        semester.setAcademicYear(request.getAcademicYear().trim());
        semester.setSemesterNumber(request.getSemesterNumber());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());

        return SemesterDetailResponse.fromEntity(semesterRepository.save(semester));
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
    @Transactional
    public void closeSemester(Long semesterId) {
        log.info("⏳ Hệ thống đang tiến hành kiểm tra để đóng học kỳ ID: {}", semesterId);

        // 1. Kiểm tra học kỳ có tồn tại trong hệ thống không
        Semester semester = semesterRepository.findByIdAndDeletedAtIsNull(semesterId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ yêu cầu hoặc học kỳ đã bị xóa!"));

        // 2. Nếu học kỳ đã đóng từ trước rồi thì không cần xử lý lại
        if (semester.getStatus() == Semester.SemesterStatus.CLOSED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Học kỳ này đã được đóng từ trước đó!");
        }

        // 3. 🚨 NGHIỆP VỤ CỐT LÕI: Đếm số lượng lớp chưa hoàn thành (Chưa thành COMPLETED hoặc CANCELED)
        List<ClassStatus> finishedStatuses = List.of(ClassStatus.COMPLETED, ClassStatus.CANCELED);
        long incompleteClassesCount = classRepository.countBySemesterIdAndStatusNotInAndDeletedAtIsNull(semesterId, finishedStatuses);

        if (incompleteClassesCount > 0) {
            log.warn("🚨 Chặn đóng học kỳ ID [{}]: Còn {} lớp học phần chưa chuyển sang trạng thái COMPLETED!", semesterId, incompleteClassesCount);
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    String.format("Không thể đóng học kỳ! Hiện tại vẫn còn %d lớp học phần đang diễn ra hoặc chưa hoàn thành nhập điểm.", incompleteClassesCount));
        }

        // 4. Nếu mọi điều kiện đều xanh mượt -> Thực hiện đóng học kỳ và chuyển trạng thái
        semester.setStatus(Semester.SemesterStatus.CLOSED);
        semesterRepository.save(semester);

        log.info("✅ Đóng thành công học kỳ mã [{}]. Toàn bộ lớp học phần đã được khóa sổ điểm an toàn.", semester.getSemesterCode());
    }

}