package com.example.LMS.service;

import com.example.LMS.dto.request.CourseApproveRequest;
import com.example.LMS.dto.request.CourseListRequest;
import com.example.LMS.dto.request.CourseProposalRequest;
import com.example.LMS.dto.response.CourseResponse;
import com.example.LMS.entity.model.Course;
import com.example.LMS.entity.model.Department;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.CourseRepository;
import com.example.LMS.repository.DepartmentRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    // ============================================================
    // DANH SÁCH MÔN HỌC
    // ============================================================
    public Page<CourseResponse> getCourses(CourseListRequest request) {

        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Specification<Course> spec = buildSpecification(request);

        return courseRepository.findAll(spec, pageable)
                .map(CourseResponse::fromEntity);
    }

    // ============================================================
    // XEM CHI TIẾT MÔN HỌC
    // ============================================================
    public CourseResponse getCourseById(Long id) {

        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học với id: " + id
                ));

        return CourseResponse.fromEntity(course);
    }

    // ============================================================
    // ĐỀ XUẤT MÔN HỌC (COURSE_PROPOSE)
    // Tạo môn học mới với status = PENDING
    // ============================================================
    public CourseResponse proposeCourse(CourseProposalRequest request) {

        // 1. Kiểm tra khoa tồn tại
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khoa với id: " + request.getDepartmentId()
                ));

        // 2. Kiểm tra mã môn học đã tồn tại chưa
        if (courseRepository.existsByCode(request.getCode())) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "Mã môn học '" + request.getCode() + "' đã tồn tại"
            );
        }

        // 3. Tạo môn học mới với status PENDING
        Course course = Course.builder()
                .department(department)
                .code(request.getCode().toUpperCase().trim())
                .name(request.getName().trim())
                .credits(request.getCredits())
                .theoreticalHours(request.getTheoreticalHours())
                .practicalHours(request.getPracticalHours())
                .description(request.getDescription())
                .status(Course.Status.PENDING)
                .build();

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // DANH SÁCH CHỜ DUYỆT (COURSE_APPROVE_LIST)
    // Lấy danh sách môn học có status = PENDING
    // ============================================================
    public Page<CourseResponse> getPendingCourses(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Course> spec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), Course.Status.PENDING),
                        cb.isNull(root.get("deletedAt"))
                );

        return courseRepository.findAll(spec, pageable)
                .map(CourseResponse::fromEntity);
    }

    // ============================================================
    // DUYỆT / TỪ CHỐI MÔN HỌC (COURSE_APPROVE)
    // ============================================================
    public CourseResponse approveCourse(CourseApproveRequest request) {

        // 1. Tìm môn học
        Course course = courseRepository.findByIdAndDeletedAtIsNull(request.getCourseId())
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học với id: " + request.getCourseId()
                ));

        // 2. Chỉ duyệt được môn đang PENDING
        if (course.getStatus() != Course.Status.PENDING) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể duyệt môn học đang ở trạng thái PENDING"
            );
        }

        // 3. Xử lý action
        String action = request.getAction().toUpperCase();

        if (action.equals("APPROVED")) {
            course.setStatus(Course.Status.APPROVED);
            course.setRejectReason(null);

        } else if (action.equals("REJECTED")) {
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "Lý do từ chối không được để trống"
                );
            }
            course.setStatus(Course.Status.REJECTED);
            course.setRejectReason(request.getRejectReason().trim());

        } else {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "Action không hợp lệ. Chỉ chấp nhận: APPROVED hoặc REJECTED"
            );
        }

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // SPECIFICATION (dynamic filter)
    // ============================================================
    private Specification<Course> buildSpecification(CourseListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Tìm theo code hoặc name
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            // Lọc theo status
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    Course.Status status = Course.Status.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {}
            }

            // Lọc theo khoa
            if (request.getDepartmentId() != null) {
                Join<Object, Object> deptJoin = root.join("department", JoinType.INNER);
                predicates.add(cb.equal(deptJoin.get("id"), request.getDepartmentId()));
            }

            // Chỉ lấy môn chưa bị xóa mềm
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}