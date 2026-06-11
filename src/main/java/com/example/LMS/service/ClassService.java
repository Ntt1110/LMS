package com.example.LMS.service;

import com.example.LMS.dto.request.ClassListRequest;
import com.example.LMS.dto.response.*;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.ClassSchedule;
import com.example.LMS.entity.model.Course;
import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.UserProfile;
import com.example.LMS.entity.model.StudentProfile;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassEntityRepository classRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

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

    // ============================================================
    // Chi tiết lớp học phần dành cho sinh viên xem trước khi đăng ký
    // GET /api/v1/classes/{classId}/student-detail
    // ============================================================
    public ClassDetailForStudentResponse getClassDetailForStudent(Long classId) {

        ClassEntity c = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        if (c.getDeletedAt() != null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!");
        }

        Course course = courseRepository.findByIdAndDeletedAtIsNull(c.getCourseId()).orElse(null);

        String lecturerName = c.getLecturerId() != null
                ? userProfileRepository.findByUserId(c.getLecturerId())
                .map(p -> p.getFullName()).orElse("Chưa phân công")
                : "Chưa phân công";

        int enrolled = classRepository.countEnrollmentsByClassId(c.getId());

        List<ClassDetailForStudentResponse.ScheduleInfo> scheduleInfos =
                classRepository.findSchedulesByClassId(c.getId()).stream().map(cs ->
                        ClassDetailForStudentResponse.ScheduleInfo.builder()
                                .dayOfWeek(cs.getDayOfWeek())
                                .shiftName(cs.getShift().getName())
                                .startTime(cs.getShift().getStartTime())
                                .endTime(cs.getShift().getEndTime())
                                .roomName(cs.getRoom().getName())
                                .roomType(cs.getRoom().getType().name())
                                .build()
                ).collect(Collectors.toList());

        return ClassDetailForStudentResponse.builder()
                .classId(c.getId())
                .classCode(c.getCode())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .maxStudents(c.getMaxStudents())
                .currentStudents(enrolled)
                .semesterCode(c.getSemester().getSemesterCode())
                .courseCode(course != null ? course.getCode() : null)
                .courseName(course != null ? course.getName() : null)
                .credits(course != null ? course.getCredits() : null)
                .lecturerName(lecturerName)
                .schedules(scheduleInfos)
                .build();
    }

    // ============================================================
    // DANH SÁCH LỚP ĐƯỢC PHÂN CÔNG (Giảng viên)
    // GET /api/v1/classes/my-assigned
    // ============================================================
    public List<LecturerClassResponse> getMyAssignedClasses(String status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        List<ClassEntity> classes = classRepository.findByLecturerIdAndDeletedAtIsNull(lecturer.getId());
        if (status != null && !status.isBlank()) {
            try {
                ClassStatus classStatus = ClassStatus.valueOf(status.toUpperCase());
                classes = classes.stream()
                        .filter(c -> c.getStatus() == classStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        return classes.stream().map(c -> {
            String courseName = courseRepository.findNameById(c.getCourseId()).orElse("N/A");
            int enrolled = classRepository.countEnrollmentsByClassId(c.getId());

            var schedules = classScheduleRepository.findByClassId(c.getId());
            Integer dayOfWeek = null;
            String shiftName = null;
            String roomName = null;
            if (!schedules.isEmpty()) {
                var s = schedules.get(0);
                dayOfWeek = s.getDayOfWeek();
                shiftName = s.getShift() != null ? s.getShift().getName() : null;
                roomName = s.getRoom() != null ? s.getRoom().getName() : null;
            }

            return LecturerClassResponse.builder()
                    .classId(c.getId())
                    .classCode(c.getCode())
                    .courseName(courseName)
                    .semesterCode(c.getSemester().getSemesterCode())
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .maxStudents(c.getMaxStudents())
                    .currentStudents(enrolled)
                    .dayOfWeek(dayOfWeek)
                    .shiftName(shiftName)
                    .roomName(roomName)
                    .build();
        }).collect(Collectors.toList());
    }

    // ============================================================
    // CHI TIẾT LỚP + DANH SÁCH SINH VIÊN (Giảng viên)
    // GET /api/v1/classes/{classId}/detail-with-students
    // ============================================================
    public LecturerClassDetailResponse getAssignedClassDetail(Long classId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        ClassEntity c = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        if (!lecturer.getId().equals(c.getLecturerId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bạn không được phân công dạy lớp này!");
        }

        var course = courseRepository.findById(c.getCourseId()).orElse(null);
        int enrolled = classRepository.countEnrollmentsByClassId(c.getId());

        var scheduleList = classScheduleRepository.findByClassId(c.getId());
        Integer dayOfWeek = null;
        String shiftName = null;
        String roomName = null;
        if (!scheduleList.isEmpty()) {
            var s = scheduleList.get(0);
            dayOfWeek = s.getDayOfWeek();
            shiftName = s.getShift() != null ? s.getShift().getName() : null;
            roomName = s.getRoom() != null ? s.getRoom().getName() : null;
        }

        return LecturerClassDetailResponse.builder()
                .classId(c.getId())
                .classCode(c.getCode())
                .courseName(course != null ? course.getName() : "N/A")
                .courseCode(course != null ? course.getCode() : "N/A")
                .credits(course != null ? course.getCredits() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .maxStudents(c.getMaxStudents())
                .currentStudents(enrolled)
                .dayOfWeek(dayOfWeek)
                .shiftName(shiftName)
                .roomName(roomName)
                .build();
    }

    // ============================================================
    // DANH SÁCH SINH VIÊN CỦA LỚP (đơn giản)
    // GET /api/v1/classes/{classId}/students
    // ============================================================
    public List<StudentOfClassResponse> getStudentsOfClass(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        return enrollmentRepository.findByClassEntityIdAndStatusNot(classId, EnrollmentStatus.DROPPED)
                .stream().map(e -> {
                    var profile = userProfileRepository.findByUserId(e.getStudentId()).orElse(null);
                    var studentProfile = studentProfileRepository.findByUserId(e.getStudentId()).orElse(null);
                    var user = userRepository.findById(e.getStudentId()).orElse(null);

                    return StudentOfClassResponse.builder()
                            .studentId(e.getStudentId())
                            .fullName(profile != null ? profile.getFullName() : "N/A")
                            .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                            .studentCode(studentProfile != null ? studentProfile.getStudentCode() : "N/A")
                            .email(user != null ? user.getEmail() : "N/A")
                            .enrollmentStatus(e.getStatus().name())
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getClassesBySemester(Long semesterId) {
        log.info("🔍 Đang tải danh sách lớp học phần theo học kỳ ID: {}", semesterId);

        // 1. Kéo toàn bộ danh sách lớp thuộc kỳ này từ DB (Hàm findBySemesterId ông đã định nghĩa trong ClassEntityRepository)
        List<ClassEntity> classes = classRepository.findBySemesterId(semesterId);
        if (classes.isEmpty()) {
            return List.of();
        }

        // 2. 🚀 TỐI ƯU HIỆU NĂNG: Thu thập tất cả các ID liên quan để không bị dính lỗi N+1 Query
        List<Long> courseIds = classes.stream()
                .map(ClassEntity::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> userIds = classes.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getManagerId(), c.getLecturerId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3. Sử dụng findAllById (có sẵn của JpaRepository) để bốc toàn bộ bản ghi lên bộ nhớ tạm trong 1 câu SQL duy nhất
        Map<Long, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, course -> course));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. Duyệt danh sách lớp để map dữ liệu chuẩn đét vào ClassResponse DTO của ông
        return classes.stream().map(c -> {

            // Xử lý bốc phòng học an toàn từ danh sách schedules liên kết trong ClassEntity
            String displayRoom = "Chưa xếp phòng";
            if (c.getSchedules() != null && !c.getSchedules().isEmpty()) {
                displayRoom = c.getSchedules().get(0).getRoom().toString();
            }

            // Tìm thông tin môn học (Course) từ Map
            Course course = courseMap.get(c.getCourseId());
            String courseName = (course != null) ? course.getName() : "Không rõ môn học";
            String courseCode = (course != null) ? course.getCode() : "N/A";

            // Tìm thông tin Giáo vụ (Manager) từ Map và lấy fullName từ profile của User
            User manager = userMap.get(c.getManagerId());
            String managerName = (manager != null && manager.getProfile() != null)
                    ? manager.getProfile().getFullName() : "Hệ thống";

            // Tìm thông tin Giảng viên (Lecturer) từ Map và lấy fullName từ profile của User
            User lecturer = userMap.get(c.getLecturerId());
            String lecturerName = (lecturer != null && lecturer.getProfile() != null)
                    ? lecturer.getProfile().getFullName() : "Chưa phân công";

            // Khớp 100% với Builder của ClassResponse yêu cầu
            return ClassResponse.builder()
                    .id(c.getId())
                    .code(c.getCode())
                    .status(c.getStatus() != null ? c.getStatus().name() : "PENDING")
                    .room(displayRoom)
                    // Thông tin ID liên kết
                    .courseId(c.getCourseId())
                    .managerId(c.getManagerId())
                    .lecturerId(c.getLecturerId())
                    // Dữ liệu thật đã bốc thành công từ các bảng liên quan:
                    .courseName(courseName)
                    .courseCode(courseCode)
                 //   .managerName(managerName)
                    .lecturerName(lecturerName)
                    .semesterId(c.getSemester() != null ? c.getSemester().getId() : semesterId)
                    .semesterCode(c.getSemester() != null ? c.getSemester().getSemesterCode() : null)
                    .build();
        }).toList();
    }
}