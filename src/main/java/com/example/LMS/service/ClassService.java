    package com.example.LMS.service;

    import com.example.LMS.dto.request.ClassListRequest;
    import com.example.LMS.dto.response.ClassDetailResponse;
    import com.example.LMS.entity.Enum.ClassStatus;
    import com.example.LMS.entity.model.ClassEntity;
    import com.example.LMS.entity.model.Course;
    import com.example.LMS.entity.model.User;
    import com.example.LMS.exception.CustomException;
    import com.example.LMS.repository.*;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.*;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.http.HttpStatus;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Service;
    import com.example.LMS.entity.model.Course;
    import jakarta.persistence.criteria.Subquery;
    import jakarta.persistence.criteria.Predicate;
    import jakarta.persistence.criteria.Root;
    import jakarta.persistence.criteria.*;

    @Service
    @RequiredArgsConstructor
    public class ClassService {

        private final ClassEntityRepository classRepository;
        private final CourseRepository courseRepository;
        private final UserRepository userRepository;

        public Page<ClassDetailResponse> getClasses(ClassListRequest request) {

            // Lấy thông tin user đang đăng nhập
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản!"));

            boolean isHeadOfDept = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_HEAD_OF_DEPT"));

            // Nếu là Trưởng khoa → chỉ xem lớp mình quản lý
            if (isHeadOfDept) {
                request.setManagerId(currentUser.getId());
            }

            Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                    ? Sort.by(request.getSortBy()).ascending()
                    : Sort.by(request.getSortBy()).descending();

            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

            return classRepository.findAll(buildSpec(request), pageable)
                    .map(this::toResponse);
        }

        private Specification<ClassEntity> buildSpec(ClassListRequest req) {
            return (root, query, cb) -> {
                query.distinct(true);
                var predicates = new java.util.ArrayList<Predicate>();

                // Tìm theo mã lớp
                if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                    String pattern = "%" + req.getKeyword().trim().toLowerCase() + "%";

                    // Tìm courseId khớp tên môn học qua subquery
                    Subquery<Long> courseSubquery = query.subquery(Long.class);
                    Root<Course> courseRoot = courseSubquery.from(Course.class);
                    courseSubquery.select(courseRoot.get("id"))
                            .where(cb.like(cb.lower(courseRoot.get("name")), pattern));

                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("code")), pattern),         // tìm mã lớp
                            root.get("courseId").in(courseSubquery)               // tìm tên môn
                    ));
                }

                // Lọc theo học kỳ
                if (req.getSemesterId() != null) {
                    predicates.add(cb.equal(root.get("semester").get("id"), req.getSemesterId()));
                }

                // Lọc theo trạng thái
                if (req.getStatus() != null && !req.getStatus().isBlank()) {
                    try {
                        ClassStatus status = ClassStatus.valueOf(req.getStatus().toUpperCase());
                        predicates.add(cb.equal(root.get("status"), status));
                    } catch (IllegalArgumentException ignored) {}
                }

                // Lọc theo khoa qua subquery Course → Department
                if (req.getDepartmentId() != null) {
                    Subquery<Long> deptSubquery = query.subquery(Long.class);
                    Root<Course> courseRoot = deptSubquery.from(Course.class);
                    deptSubquery.select(courseRoot.get("id"))
                            .where(cb.equal(courseRoot.get("department").get("id"), req.getDepartmentId()));
                    predicates.add(root.get("courseId").in(deptSubquery));
                }

                // Lọc theo managerId (Trưởng khoa chỉ xem lớp mình)
                if (req.getManagerId() != null) {
                    predicates.add(cb.equal(root.get("managerId"), req.getManagerId()));
                }

                // Chỉ lấy chưa xóa mềm
                predicates.add(cb.isNull(root.get("deletedAt")));

                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }

        private ClassDetailResponse toResponse(ClassEntity c) {
            // Lấy thông tin Course
            Course course = courseRepository.findByIdAndDeletedAtIsNull(c.getCourseId()).orElse(null);

            // Lấy tên manager
            String managerName = userRepository.findByIdAndDeletedAtIsNull(c.getManagerId())
                    .map(u -> u.getProfile() != null ? u.getProfile().getFullName() : u.getUsername())
                    .orElse(null);

            // Lấy tên lecturer
            String lecturerName = c.getLecturerId() != null
                    ? userRepository.findByIdAndDeletedAtIsNull(c.getLecturerId())
                    .map(u -> u.getProfile() != null ? u.getProfile().getFullName() : u.getUsername())
                    .orElse(null)
                    : null;

            return ClassDetailResponse.builder()
                    .id(c.getId())
                    .code(c.getCode())
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .maxStudents(c.getMaxStudents())
                    .semesterId(c.getSemester().getId())
                    .semesterCode(c.getSemester().getSemesterCode())
                    .academicYear(c.getSemester().getAcademicYear())
                    .courseId(course != null ? course.getId() : null)
                    .courseCode(course != null ? course.getCode() : null)
                    .courseName(course != null ? course.getName() : null)
                    .departmentId(course != null ? course.getDepartment().getId() : null)
                    .departmentName(course != null ? course.getDepartment().getName() : null)
                    .managerId(c.getManagerId())
                    .managerName(managerName)
                    .lecturerId(c.getLecturerId())
                    .lecturerName(lecturerName)
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }