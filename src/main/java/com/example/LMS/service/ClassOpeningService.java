package com.example.LMS.service;

import com.example.LMS.dto.request.*;
import com.example.LMS.dto.response.ClassOpeningResponseDto;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.model.*;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.LMS.entity.model.Course;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import com.example.LMS.dto.response.CourseWithClassesResponse;
import com.example.LMS.dto.response.ClassResponse;
import com.example.LMS.entity.Enum.ClassStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassOpeningService {

    private final ClassOpeningRequestRepository requestRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CourseRepository courseRepository;

    private final RoomRepository roomRepository;
    private final ShiftRepository shiftRepository;

     private final ClassEntityRepository classRepository;
     private final ClassScheduleRepository classScheduleRepository;

     private final DepartmentRepository departmentRepository;


     private final MajorRepository majorRepository;

    @Transactional
    public void createOpeningRequest(ClassOpeningRequestDto dto) {
        log.info("⏳ Tháo chốt kiểm tra đề xuất mở lớp học phần mới...");

        // 1. Kiểm tra học kỳ gửi lên có tồn tại trong hệ thống hay không
        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Học kỳ được chọn không tồn tại!"));

        if (semester.getStatus() == Semester.SemesterStatus.CLOSED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Không thể đề xuất mở lớp cho học kỳ đã đóng!");
        }

        // 2. Lấy thông tin giảng viên/trưởng bộ môn đang thao tác từ Security Context
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tài khoản người đề xuất!"));

        // 3. Tiến hành đóng gói dữ liệu và lưu đơn ở trạng thái PENDING
        ClassOpeningRequest openingRequest = ClassOpeningRequest.builder()
                .semester(semester)
                .courseId(dto.getCourseId())
                .requesterId(user.getId())
                .expectedStudents(dto.getExpectedStudents())
                .note(dto.getNote())
                .status(ClassOpenningStatus.PENDING) // Luôn luôn là chờ duyệt
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestRepository.save(openingRequest);
        log.info("✅ Giảng viên {} đã gửi đề xuất mở lớp thành công, chờ Phòng Đào tạo duyệt.", currentUsername);
    }

    public List<DropdownResponseDto> getCoursesDropdownForDean(String username) {
        log.info("🔍 Trưởng khoa username [{}] đang yêu cầu tải danh sách Môn học thuộc khoa quản lý...", username);

        // 1. Tìm thực thể User từ username để lấy id của Trưởng khoa
        User deanUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản Trưởng khoa hợp lệ!"));

        Long deanUserId = deanUser.getId();

        // 2. Tìm ID Khoa (Department) mà thầy này đang gán làm Quản lý (manager)
        Long managedDepartmentId = departmentRepository.findIdByManagerId(deanUserId)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "Tài khoản này chưa được phân quyền quản lý Khoa nào!"));

        // 3. Gọi câu lệnh JPQL sạch sẽ ở Bước 1 để hốt toàn bộ môn học thuộc khoa đó
        return courseRepository.findCoursesByDepartmentId(managedDepartmentId);
    }

    public List<DropdownResponseDto> getMajorsDropdown() {
        log.info("🔍 Đang tải danh sách Ngành học phục vụ đề xuất lớp...");
        return majorRepository.findAllMajorsDropdown();
    }

    public Page<ClassOpeningResponseDto> getPagingRequests(RequestFilterDto filter) {
        int pageIndex = filter.getPage() > 0 ? filter.getPage() - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, filter.getSize());

        Page<ClassOpeningRequest> entityPage = requestRepository.findAll(
                ClassOpeningSpecification.filterRequests(filter), pageable
        );

        return entityPage.map(entity -> {
            // 1. Map các trường có sẵn từ Entity sang DTO
            ClassOpeningResponseDto.ClassOpeningResponseDtoBuilder builder = ClassOpeningResponseDto.builder()
                    .requestId(entity.getId())
                    .expectedStudents(entity.getExpectedStudents())
                    .note(entity.getNote())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt());

            // 2. Xử lý Học kỳ (Giả định trường này ông map đối tượng Semester thành công)
            if (entity.getSemester() != null) {
                builder.semesterCode(entity.getSemester().getSemesterCode());
            }

            // 3. 🌟 XỬ LÝ MÔN HỌC (Lấy tên từ CourseRepository thông qua ID thô)
            // Hãy thay "getCourseId()" bằng đúng tên biến kiểu Long chứa ID môn học trong Entity của ông
            if (entity.getCourseId() != null) {
                Long cId = entity.getCourseId();
                builder.courseId(cId);

                // Tìm tên môn học dưới DB đắp vào DTO
                courseRepository.findById(cId).ifPresent(course -> builder.courseName(course.getName()));
            }

            // 4. 🌟 XỬ LÝ NGƯỜI ĐỀ XUẤT (Lấy tên thông qua ID thô)
            // Hãy thay "getRequesterId()" bằng đúng tên biến kiểu Long chứa ID người tạo trong Entity của ông
            if (entity.getRequesterId() != null) {
                Long rId = entity.getRequesterId();
                builder.requesterId(rId);

                // Tìm hồ sơ người dùng để hốt họ tên (fullName) đắp vào DTO
                userProfileRepository.findById(rId).ifPresent(profile -> builder.requesterName(profile.getFullName()));
            }

            return builder.build();
        });
    }
    public List<ClassOpeningResponseDto> getPendingOpeningRequests() {
        log.info("🔍 Phòng Đào tạo đang truy vấn danh sách lớp học phần chờ duyệt...");

        // 1. Tìm toàn bộ các đơn đề xuất có status là PENDING
        List<ClassOpeningRequest> pendingRequests =
                requestRepository.findByStatusOrderByCreatedAtDesc(ClassOpenningStatus.PENDING);

        // 2. Map danh sách thực thể sang danh sách DTO để gửi về
        return pendingRequests.stream().map(request -> {

            // 🔥 BIẾN HÌNH: 1 dòng duy nhất thay thế toàn bộ đống try-catch và if-else phức tạp!
            String UserSend = userProfileRepository.findByUserId(request.getRequesterId())
                    .map(UserProfile::getFullName) // Nếu có profile, trích xuất lấy FullName
                    .orElse("phòng đào tạo"); // Nếu không có, mặc định gán chuỗi này
            String courseName = courseRepository.findNameById(request.getCourseId())
                    .orElse("Môn học không tồn tại");

            return ClassOpeningResponseDto.builder()
                    .requestId(request.getId())
                    .semesterCode(request.getSemester().getSemesterCode())
                    .courseId(request.getCourseId())
                    .courseName(courseName)
                    .requesterId(request.getRequesterId())
                    .requesterName(UserSend) // Trả về tên hiển thị sạch sẽ
                    .expectedStudents(request.getExpectedStudents())
                    .note(request.getNote())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .build();
        }).collect(toList());
    }

    // 1. Lấy thông tin chi tiết của đơn đề xuất để đổ dữ liệu tĩnh lên Form
    public ClassOpeningResponseDto getRequestDetail(Long requestId) {
        ClassOpeningRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất!"));

        String teacherName = userProfileRepository.findByUserId(request.getRequesterId())
                .map(UserProfile::getFullName) //
                .orElse("Giảng viên hệ thống");

        String courseName = courseRepository.findNameById(request.getCourseId())
                .orElse("Môn học không tồn tại");

        return ClassOpeningResponseDto.builder()
                .requestId(request.getId())
                .semesterCode(request.getSemester().getSemesterCode())
                .courseId(request.getCourseId())
                .courseName(courseName)
                .requesterId(request.getRequesterId())
                .requesterName(teacherName) //
                .expectedStudents(request.getExpectedStudents())
                .note(request.getNote())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    // 2. Lấy danh sách Trưởng khoa / Giảng viên phục vụ Dropdown gán quản lý lớp
    public List<DropdownResponseDto> getLecturersDropdown() {
        return userRepository.findAllActiveLecturers();
    }

    // 3. Lấy danh sách phòng học phục vụ Dropdown xếp lịch
    public List<DropdownResponseDto> getRoomsDropdown() {
        return roomRepository.findAllRoomsForDropdown();
    }

    // 4. Lấy danh sách ca học phục vụ Dropdown xếp lịch
    public List<DropdownResponseDto> getShiftsDropdown() {
        return shiftRepository.findAllShiftsForDropdown();
    }

    @Transactional
    public void rejectOpeningRequest(Long requestId, RejectClassRequestDto dto) {
        log.info("🔴 Xử lý hành chính: Từ chối đơn đề xuất mở lớp ID: {}", requestId);

        ClassOpeningRequest openingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất mở lớp!"));

        if (openingRequest.getStatus() != ClassOpenningStatus.PENDING) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Đơn đề xuất này đã được xử lý rồi, không thể từ chối!");
        }

        openingRequest.setStatus(ClassOpenningStatus.REJECTED);
        openingRequest.setRejectReason(dto.getRejectReason());
        openingRequest.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(openingRequest);
        log.info("✅ Đã chuyển trạng thái đơn ID: {} sang REJECTED.", requestId);
    }

    // =========================================================================
    // 🌟 API 1B: PHÒNG ĐÀO TẠO CHỈ BẤM NÚT DUYỆT ĐƠN HÀNH CHÍNH (CHƯA SINH LỚP)
    // =========================================================================
    @Transactional
    public void approveOpeningRequest(Long requestId) {
        log.info("🟢 Xử lý hành chính: Phê duyệt đơn đề xuất mở lớp ID: {}", requestId);

        ClassOpeningRequest openingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất mở lớp!"));

        if (openingRequest.getStatus() != ClassOpenningStatus.PENDING) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Đơn đề xuất này đã được xử lý rồi, không thể phê duyệt!");
        }

        openingRequest.setStatus(ClassOpenningStatus.APPROVED);
        openingRequest.setRejectReason(null); // Xóa sạch lý do từ chối cũ nếu có
        openingRequest.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(openingRequest);
        log.info("✅ Đã đóng nhãn APPROVED thành công cho đơn ID: {}", requestId);
    }
    @Transactional
    public void generateClassesFromRequest(Long requestId, GenerateClassRequestDto dto) {
        log.info("🔄 Bắt đầu quy trình khởi tạo loạt lớp thực tế từ đơn đề xuất ID: {}", requestId);

        ClassOpeningRequest openingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất mở lớp!"));

        if (openingRequest.getStatus() != ClassOpenningStatus.APPROVED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Đơn đề xuất này chưa được Phòng Đào tạo duyệt sang nhãn APPROVED!");
        }

        // Kiểm tra sự tồn tại của Trưởng khoa được chọn
        User managerUser = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Tài khoản Trưởng khoa được chọn không tồn tại!"));

        // 🛡️ CHỐT CHẶN BẢO MẬT: Kiểm tra xem tài khoản được gán có quyền CLASS_CREATE hay không
        boolean hasClassCreatePermission = managerUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> "CLASS_CREATE".equals(permission.getCode()));

        if (!hasClassCreatePermission) {
            log.warn("🚨 Khước từ khởi tạo lớp: Tài khoản ID [{}] không sở hữu quyền CLASS_CREATE!", dto.getManagerId());
            throw new CustomException(HttpStatus.BAD_REQUEST, "Không thể gán! Người quản lý được chọn không có quyền mở lớp (CLASS_CREATE).");
        }

        // Kiểm tra xung đột ca/phòng học
        boolean isRoomOccupied = classScheduleRepository.existsByRoomIdAndDayOfWeekAndShiftId(
                dto.getRoomId(), dto.getDayOfWeek(), dto.getShiftId()
        );
        if (isRoomOccupied) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Xung đột lịch học! Phòng học này vào thời gian chọn đã có lớp khác sử dụng.");
        }

        String courseCode = courseRepository.findById(openingRequest.getCourseId())
                .map(Course::getCode).orElse("MONHOC");
        String semesterCode = openingRequest.getSemester().getSemesterCode();
        Long semesterId = openingRequest.getSemester().getId();

        Room cleanRoom = roomRepository.findById(dto.getRoomId()).orElseThrow();
        Shift cleanShift = shiftRepository.findById(dto.getShiftId()).orElseThrow();

        int totalClassesToCreate = dto.getNumberOfClasses();
        long currentClassCount = classRepository.countBySemesterIdAndCourseId(semesterId, openingRequest.getCourseId());

        // Vòng lặp sinh loạt lớp trống, giảng viên mặc định để null (Chuẩn phương án 1)
        for (int i = 0; i < totalClassesToCreate; i++) {
            String autoClassCode = String.format("%s-%s-L%02d", courseCode, semesterCode, currentClassCount + 1 + i);

            ClassEntity officialClass = ClassEntity.builder()
                    .semester(openingRequest.getSemester())
                    .courseId(openingRequest.getCourseId())
                    .managerId(dto.getManagerId())
                    .lecturerId(null)
                    .code(autoClassCode)
                    .maxStudents(openingRequest.getExpectedStudents())
                    .status(ClassStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            ClassEntity savedClass = classRepository.save(officialClass);

            // X xếp Thời khóa biểu ban đầu dính chặt vào lớp đầu tiên (L01)
            if (i == 0) {
                ClassSchedule schedule = ClassSchedule.builder()
                        .classEntity(savedClass)
                        .room(cleanRoom)
                        .shift(cleanShift)
                        .dayOfWeek(dto.getDayOfWeek())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                classScheduleRepository.save(schedule);
                log.info("📌 Ghim TKB lớp đầu tiên thành công: {}", autoClassCode);
            } else {
                log.info("📌 Khởi tạo thành công lớp bổ sung trống: {}", autoClassCode);
            }
        }
        log.info("✅ Quy trình hoàn tất! Tổng số lớp trống đã tạo: {}", totalClassesToCreate);
    }

    public List<DropdownResponseDto> getInstructorsDropdown(Long classId) {
        log.info("🔍 Trưởng khoa đang lấy danh sách giảng viên thuộc khoa để phân công cho lớp ID: {}", classId);

        // Gọi câu Query lọc thông minh từ userRepository của ông
        return userRepository.findInstructorsByClassDepartment(classId);
    }
    // 2. LOGIC XỬ LÝ: GÁN GIẢNG VIÊN VÀO LỚP HỌC PHẦN (CÓ CHECK TRÙNG LỊCH DẠY)
    // =========================================================================
    @Transactional
    public void assignLecturerToClass(Long classId, AssignLecturerDto dto) {
        log.info("⚡ Tiến hành phân công giảng viên ID: {} vào lớp học phần ID: {}", dto.getLecturerId(), classId);

        // 1. Kiểm tra lớp học phần có tồn tại hay không
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần yêu cầu!"));

        // 2. Kiểm tra xem giảng viên được chọn có tồn tại và đúng Role INSTRUCTOR không
        User instructor = userRepository.findById(dto.getLecturerId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Giảng viên được chọn không tồn tại!"));

        boolean isInstructor = instructor.getRoles().stream().anyMatch(r -> r.getCode().equals("INSTRUCTOR"));
        if (!isInstructor) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Tài khoản được chọn không phải là Giảng viên!");
        }

        // 3. 🛡️ CHỐT CHẶN NÂNG CAO: Kiểm tra trùng lịch dạy của Giảng viên (Tránh một thầy bị phân dạy 2 nơi cùng ca)
        // Lấy lịch học (Thứ, Ca) hiện tại của lớp học phần này từ bảng class_schedules
        var schedules = classEntity.getSchedules();
        if (schedules != null && !schedules.isEmpty()) {
            for (var schedule : schedules) {
                // Đếm xem thầy giáo này vào Thứ đó, Ca đó có đang bị bận ở lớp nào khác đang ONGOING hoặc PENDING không
                boolean isTeacherBusy = classScheduleRepository.existsByClassEntityLecturerIdAndDayOfWeekAndShiftId(
                        dto.getLecturerId(), schedule.getDayOfWeek(), schedule.getShift().getId()
                );
                if (isTeacherBusy) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Giảng viên " + instructor.getProfile().getFullName() + " đã có lịch dạy lớp khác vào Thứ "
                                    + schedule.getDayOfWeek() + " - " + schedule.getShift().getName() + "!");
                }
            }
        }

        // 4. Đạt điều kiện -> Tiến hành cập nhật lecturer_id vào bảng classes
        classEntity.setLecturerId(dto.getLecturerId());
        classEntity.setUpdatedAt(LocalDateTime.now());
        classRepository.save(classEntity);

        log.info("✅ Phân công thành công Giảng viên {} phụ trách lớp học phần {}",
                instructor.getProfile().getFullName(), classEntity.getCode());
    }

    // phần xem danh sách môn học và lớp học cho Sinh viên
    public Page<CourseWithClassesResponse> getCoursesWithClasses(
            String keyword, Long departmentId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Course> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            predicates.add(cb.equal(root.get("status"), Course.Status.APPROVED));
            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return courseRepository.findAll(spec, pageable).map(course -> {

            List<ClassEntity> classes = classRepository.findByCourseIdAndDeletedAtIsNull(course.getId());

            List<CourseWithClassesResponse.ClassInfo> classSummaries = classes.stream().map(c -> {
                String lecturerName = c.getLecturerId() != null
                        ? userProfileRepository.findByUserId(c.getLecturerId())
                        .map(UserProfile::getFullName).orElse("Chưa phân công")
                        : "Chưa phân công";
                int enrolled = classRepository.countEnrollmentsByClassId(c.getId());

                return CourseWithClassesResponse.ClassInfo.builder()
                        .classId(c.getId())
                        .classCode(c.getCode())
                        .status(c.getStatus() != null ? c.getStatus().name() : null)
                        .maxStudents(c.getMaxStudents())
                        .currentStudents(enrolled)
                        .lecturerName(lecturerName)
                        .semesterCode(c.getSemester().getSemesterCode())
                        .build();
            }).collect(toList());

            return CourseWithClassesResponse.builder()
                    .courseId(course.getId())
                    .courseCode(course.getCode())
                    .courseName(course.getName())
                    .credits(course.getCredits())
                    .theoreticalHours(course.getTheoreticalHours())
                    .practicalHours(course.getPracticalHours())
                    .departmentName(course.getDepartment().getName())
                    .classes(classSummaries)
                    .build();
        });
    }
    // - Tìm kiếm theo mã lớp học phần hoặc tên môn học
    //- Lọc theo học kỳ
    //- Lọc theo trạng thái
    //- Lọc theo khoa
    //- Phân trang
    public Page<ClassResponse> getClasses(ClassListRequest request) {

        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<ClassEntity> spec = (root, query, cb) -> {
            query.distinct(true);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Tìm kiếm theo mã lớp hoặc tên môn học
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                // Tìm courseId theo tên môn trước
                List<Long> courseIds = courseRepository.findIdsByKeyword(request.getKeyword());
                if (!courseIds.isEmpty()) {
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("code")), pattern),
                            root.get("courseId").in(courseIds)
                    ));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("code")), pattern));
                }
            }

            // Lọc theo học kỳ
            if (request.getSemesterId() != null) {
                Join<Object, Object> semJoin = root.join("semester", JoinType.INNER);
                predicates.add(cb.equal(semJoin.get("id"), request.getSemesterId()));
            }

            // Lọc theo trạng thái
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    ClassStatus status = ClassStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {}
            }

            // Lọc theo khoa
            if (request.getDepartmentId() != null) {
                List<Long> courseIds = courseRepository.findIdsByDepartmentId(request.getDepartmentId());
                if (!courseIds.isEmpty()) {
                    predicates.add(root.get("courseId").in(courseIds));
                } else {
                    predicates.add(cb.disjunction());
                }
            }

            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return classRepository.findAll(spec, pageable).map(c -> {
            String courseName = courseRepository.findNameById(c.getCourseId()).orElse("N/A");
            String courseCode = courseRepository.findById(c.getCourseId())
                    .map(course -> course.getCode()).orElse("N/A");
            String managerName = userProfileRepository.findByUserId(c.getManagerId())
                    .map(UserProfile::getFullName).orElse("N/A");
            String lecturerName = c.getLecturerId() != null
                    ? userProfileRepository.findByUserId(c.getLecturerId())
                    .map(UserProfile::getFullName).orElse("Chưa phân công")
                    : "Chưa phân công";

            return ClassResponse.builder()
                    .id(c.getId())
                    .code(c.getCode())
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .maxStudents(c.getMaxStudents())
                    .createdAt(c.getCreatedAt())
                    .semesterId(c.getSemester().getId())
                    .semesterCode(c.getSemester().getSemesterCode())
                    .courseId(c.getCourseId())
                    .courseName(courseName)
                    .courseCode(courseCode)
                    .managerId(c.getManagerId())
                    .managerName(managerName)
                    .lecturerId(c.getLecturerId())
                    .lecturerName(lecturerName)
                    .build();
        });
    }

}