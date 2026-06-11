package com.example.LMS.service;

import com.example.LMS.dto.response.EnrollmentResponse;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.model.ClassEnrollment;
import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.ClassSchedule;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CourseRepository courseRepository;

    // ============================================================
    // ĐĂNG KÝ HỌC PHẦN
    // ============================================================
    @Transactional
    public EnrollmentResponse register(Long classId) {

        // 1. Lấy sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Kiểm tra lớp tồn tại
        ClassEntity classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        // 3. Kiểm tra lớp đang mở đăng ký
        if (classEntity.getStatus() != ClassStatus.REGISTRATION) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lớp học phần này hiện không mở đăng ký!");
        }

        // 4. Kiểm tra đã đăng ký lớp này chưa
        if (enrollmentRepository.existsByClassEntityIdAndStudentId(classId, student.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "Bạn đã đăng ký lớp học phần này rồi!");
        }

        // 5. Kiểm tra trùng môn học trong cùng học kỳ
        Long semesterId = classEntity.getSemester().getId();
        Long courseId   = classEntity.getCourseId();

        if (enrollmentRepository.existsBySameCourseSameSemester(student.getId(), courseId, semesterId)) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "Bạn đã đăng ký một lớp của môn học này trong học kỳ hiện tại rồi!");
        }

        // 6. Kiểm tra trùng lịch học trong cùng học kỳ
        List<Long> registeredClassIds = enrollmentRepository
                .findRegisteredClassIdsBySemester(student.getId(), semesterId);

        if (!registeredClassIds.isEmpty()) {
            // Lấy lịch của tất cả lớp đã đăng ký
            List<ClassSchedule> existingSchedules = classScheduleRepository
                    .findByClassIds(registeredClassIds);

            // Lấy lịch của lớp muốn đăng ký
            List<ClassSchedule> newSchedules = classScheduleRepository.findByClassId(classId);

            for (ClassSchedule newSlot : newSchedules) {
                for (ClassSchedule existingSlot : existingSchedules) {
                    if (newSlot.getDayOfWeek().equals(existingSlot.getDayOfWeek())
                            && newSlot.getShift().getId().equals(existingSlot.getShift().getId())) {
                        throw new CustomException(HttpStatus.CONFLICT,
                                "Lịch học bị trùng với lớp " + existingSlot.getClassEntity().getCode()
                                        + " (Thứ " + existingSlot.getDayOfWeek()
                                        + ", Ca " + existingSlot.getShift().getName() + ")!");
                    }
                }
            }
        }

        // 7. Kiểm tra còn chỗ không
        int enrolled = classEntityRepository.countEnrollmentsByClassId(classId);
        if (enrolled >= classEntity.getMaxStudents()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lớp học phần đã đầy, không thể đăng ký!");
        }

        // 8. Lưu đăng ký
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .classEntity(classEntity)
                .studentId(student.getId())
                .status(EnrollmentStatus.REGISTERED)
                .enrolledAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("✅ Sinh viên {} đã đăng ký lớp {} thành công!", username, classEntity.getCode());

        return buildResponse(saved, classEntity);
    }

    // ============================================================
    // XEM DANH SÁCH LỚP ĐÃ ĐĂNG KÝ
    // ============================================================
    public List<EnrollmentResponse> getMyEnrollments() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        return classEntityRepository.findActiveClassesByStudentId(student.getId())
                .stream()
                .map(c -> {
                    var enrollment = enrollmentRepository
                            .findByClassEntityIdAndStudentId(c.getId(), student.getId())
                            .orElse(null);
                    return buildResponse(enrollment, c);
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // XEM DANH SÁCH LỚP ĐÃ ĐĂNG KÝ (ONGOING / COMPLETED)
    // ============================================================
    public List<EnrollmentResponse> getMyEnrollmentsByStatus(String statusParam) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        ClassStatus classStatus;
        try {
            classStatus = ClassStatus.valueOf(statusParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ! Chỉ chấp nhận: ONGOING, COMPLETED");
        }

        if (classStatus != ClassStatus.ONGOING && classStatus != ClassStatus.COMPLETED) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ! Chỉ chấp nhận: ONGOING, COMPLETED");
        }

        return classEntityRepository
                .findClassesByStudentIdAndClassStatus(student.getId(), classStatus)
                .stream()
                .map(c -> {
                    var enrollment = enrollmentRepository
                            .findByClassEntityIdAndStudentId(c.getId(), student.getId())
                            .orElse(null);
                    return buildResponse(enrollment, c);
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // HỦY ĐĂNG KÝ HỌC PHẦN
    // ============================================================
    @Transactional
    public void cancel(Long classId) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        ClassEnrollment enrollment = enrollmentRepository
                .findByClassEntityIdAndStudentId(classId, student.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Bạn chưa đăng ký lớp học phần này!"));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lớp học phần này đã được hủy rồi!");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        log.info("🔴 Sinh viên {} đã hủy đăng ký lớp ID: {}", username, classId);
    }

    // ============================================================
    // HELPER
    // ============================================================
    private EnrollmentResponse buildResponse(ClassEnrollment enrollment, ClassEntity classEntity) {
        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        var schedules = classScheduleRepository.findByClassId(classEntity.getId());
        Integer dayOfWeek = null;
        String shiftName  = null;
        String roomName   = null;
        LocalTime startTimeShilf = null;
        LocalTime endTimeShilf = null;
        if (!schedules.isEmpty()) {
            var s    = schedules.get(0);
            dayOfWeek = s.getDayOfWeek();
            shiftName = s.getShift() != null ? s.getShift().getName() : null;
            startTimeShilf = s.getShift().getStartTime();
            endTimeShilf = s.getShift().getEndTime();
            roomName  = s.getRoom()  != null ? s.getRoom().getName()  : null;
        }

        return EnrollmentResponse.builder()
                .courseId(course != null ? course.getId() : null)
                .courseCode(course != null ? course.getCode() : "N/A")
                .courseName(course != null ? course.getName() : "N/A")
                .credits(course != null ? course.getCredits() : null)
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .dayOfWeek(dayOfWeek)
                .shiftName(shiftName)
                .startTimeShilf(startTimeShilf)
                .endTimeShilf(endTimeShilf)
                .roomName(roomName)
                .status(classEntity.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

}