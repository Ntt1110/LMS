package com.example.LMS.service;

import com.example.LMS.dto.request.CourseApproveRequest;
import com.example.LMS.dto.request.CourseListRequest;
import com.example.LMS.dto.request.CourseProposalRequest;
import com.example.LMS.dto.request.CourseRejectRequest;
import com.example.LMS.dto.request.UpdateCourseRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.criteria.JoinType;

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
    // DANH SÁCH MÔN HỌC THEO TRƯỞNG KHOA (đang login)
    // ============================================================
    public Page<CourseResponse> getCoursesByHeadOfDept(int page, int size, String sortBy, String sortDirection) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Course> spec = (root, query, cb) -> {
            query.distinct(true);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            var deptJoin = root.join("department", JoinType.INNER);
            predicates.add(cb.equal(deptJoin.get("manager").get("username"), username));
            predicates.add(cb.equal(root.get("status"), Course.Status.APPROVED));
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return courseRepository.findAll(spec, pageable).map(CourseResponse::fromEntity);
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
    // ============================================================
    public CourseResponse proposeCourse(CourseProposalRequest request) {

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khoa với id: " + request.getDepartmentId()
                ));

        if (courseRepository.existsByCode(request.getCode())) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "Mã môn học '" + request.getCode() + "' đã tồn tại"
            );
        }

        Course course = Course.builder()
                .department(department)
                .code(request.getCode().toUpperCase().trim())
                .name(request.getName().trim())
                .credits(request.getCredits())
                .theoreticalHours(request.getTheoreticalHours() != null ? request.getTheoreticalHours() : 0)
                .practicalHours(request.getPracticalHours() != null ? request.getPracticalHours() : 0)
                .description(request.getDescription())
                .status(Course.Status.PENDING)
                .build();

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // DUYỆT MÔN HỌC (COURSE_APPROVE)
    // ============================================================
    public CourseResponse approveCourse(CourseApproveRequest request) {

        Course course = findPendingCourse(request.getCourseId());
        course.setStatus(Course.Status.APPROVED);
        course.setRejectReason(null);

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // TỪ CHỐI MÔN HỌC (COURSE_APPROVE)
    // ============================================================
    public CourseResponse rejectCourse(CourseRejectRequest request) {

        Course course = findPendingCourse(request.getCourseId());
        course.setStatus(Course.Status.REJECTED);
        course.setRejectReason(request.getRejectReason().trim());

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // SỬA MÔN HỌC (COURSE_EDIT)
    // Chỉ cho sửa khi môn học đang ở trạng thái PENDING hoặc APPROVED
    // Không cho đổi mã (code) vì là định danh nghiệp vụ
    // ============================================================
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {

        // 1. Tìm môn học, chưa bị xóa mềm
        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học với id: " + id
                ));

        // 2. Không cho sửa môn học đã REJECTED
        if (course.getStatus() == Course.Status.REJECTED) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể sửa môn học đã bị từ chối. Vui lòng đề xuất lại."
            );
        }

        // 3. Cập nhật khoa (nếu có)
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new CustomException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy khoa với id: " + request.getDepartmentId()
                    ));
            course.setDepartment(department);
        }

        // 4. Cập nhật các trường được gửi lên (null = giữ nguyên)
        if (request.getName() != null && !request.getName().isBlank())
            course.setName(request.getName().trim());

        if (request.getCredits() != null)
            course.setCredits(request.getCredits());

        if (request.getTheoreticalHours() != null)
            course.setTheoreticalHours(request.getTheoreticalHours());

        if (request.getPracticalHours() != null)
            course.setPracticalHours(request.getPracticalHours());

        if (request.getDescription() != null)
            course.setDescription(request.getDescription().trim());

        return CourseResponse.fromEntity(courseRepository.save(course));
    }

    // ============================================================
    // XÓA MỀM MÔN HỌC (COURSE_DELETE)
    // Set deletedAt = now(), không xóa vật lý khỏi DB
    // ============================================================
    public void deleteCourse(Long id) {

        // 1. Tìm môn học, chưa bị xóa mềm
        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học với id: " + id
                ));

        // 2. Set thời điểm xóa mềm
        course.setDeletedAt(java.time.LocalDateTime.now());
        courseRepository.save(course);
    }

    // ============================================================
    // HELPER: Tìm môn học PENDING, tránh lặp code
    // ============================================================
    private Course findPendingCourse(Long id) {

        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy môn học với id: " + id
                ));

        if (course.getStatus() != Course.Status.PENDING) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể duyệt/từ chối môn học đang ở trạng thái PENDING"
            );
        }

        return course;
    }

    // ============================================================
    // SPECIFICATION (dynamic filter)
    // ============================================================
    private Specification<Course> buildSpecification(CourseListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    Course.Status status = Course.Status.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                    // status không hợp lệ thì bỏ qua
                }
            }
            if (request.getDepartmentId() != null) {
                Join<Object, Object> deptJoin = root.join("department", JoinType.INNER);
                predicates.add(cb.equal(deptJoin.get("id"), request.getDepartmentId()));
            }

            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}