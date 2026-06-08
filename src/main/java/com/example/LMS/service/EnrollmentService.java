package com.example.LMS.service;

import com.example.LMS.dto.response.EnrollmentResponse;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.model.ClassEnrollment;
import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        // 4. Kiểm tra đã đăng ký chưa
        if (enrollmentRepository.existsByClassEntityIdAndStudentId(classId, student.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "Bạn đã đăng ký lớp học phần này rồi!");
        }

        // 5. Kiểm tra còn chỗ không
        int enrolled = classEntityRepository.countEnrollmentsByClassId(classId);
        if (enrolled >= classEntity.getMaxStudents()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lớp học phần đã đầy, không thể đăng ký!");
        }

        // 6. Lưu đăng ký
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

        return enrollmentRepository
                .findByStudentIdAndStatusNot(student.getId(), EnrollmentStatus.DROPPED)
                .stream()
                .map(e -> buildResponse(e, e.getClassEntity()))
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

        // Lấy lịch học của lớp (thứ, ca, phòng)
        var schedules = classScheduleRepository.findByClassId(classEntity.getId());
        Integer dayOfWeek = null;
        String shiftName = null;
        String roomName = null;
        if (!schedules.isEmpty()) {
            var s = schedules.get(0);
            dayOfWeek = s.getDayOfWeek();
            shiftName = s.getShift() != null ? s.getShift().getName() : null;
            roomName = s.getRoom() != null ? s.getRoom().getName() : null;
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
                .roomName(roomName)
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}