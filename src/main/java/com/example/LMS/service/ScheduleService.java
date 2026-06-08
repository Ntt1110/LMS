package com.example.LMS.service;

import com.example.LMS.dto.response.ScheduleResponse;
import com.example.LMS.entity.model.ClassSchedule;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepositoryStudent scheduleRepositoryStudent;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CourseRepository courseRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassScheduleRepository classScheduleRepository;

    // ============================================================
    // Tính số tuần học của lớp
    // Công thức: (theoreticalHours + practicalHours) / 3 / sessionsPerWeek
    // ============================================================
    private Integer calculateTotalWeeks(Long classId, Long courseId) {
        // 1. Lấy tổng tiết của môn học
        var course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return null;

        int totalHours = (course.getTheoreticalHours() != null ? course.getTheoreticalHours() : 0)
                + (course.getPracticalHours() != null ? course.getPracticalHours() : 0);

        if (totalHours == 0) return null;

        // 2. Đếm số buổi học/tuần của lớp (số lịch trong class_schedules)
        int sessionsPerWeek = classScheduleRepository.findByClassId(classId).size();
        if (sessionsPerWeek == 0) return null;

        // 3. Tính số tuần: tổng tiết / 3 (tiết/buổi) / số buổi/tuần
        return (int) Math.ceil((double) totalHours / 3 / sessionsPerWeek);
    }

    public List<ScheduleResponse> getMySchedule() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        List<ClassSchedule> schedules = scheduleRepositoryStudent
                .findSchedulesByStudentId(student.getId());

        return schedules.stream().map(cs -> {
            var classEntity = cs.getClassEntity();

            String courseName = courseRepository.findNameById(classEntity.getCourseId())
                    .orElse("N/A");
            String lecturerName = classEntity.getLecturerId() != null
                    ? userProfileRepository.findByUserId(classEntity.getLecturerId())
                    .map(p -> p.getFullName()).orElse("Chưa phân công")
                    : "Chưa phân công";

            Integer totalWeeks = calculateTotalWeeks(classEntity.getId(), classEntity.getCourseId());

            return ScheduleResponse.builder()
                    .courseName(courseName)
                    .classCode(classEntity.getCode())
                    .roomName(cs.getRoom().getName())
                    .dayOfWeek(cs.getDayOfWeek())
                    .shiftName(cs.getShift().getName())
                    .startTime(cs.getShift().getStartTime())
                    .endTime(cs.getShift().getEndTime())
                    .lecturerName(lecturerName)
                    .totalWeeks(totalWeeks)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ScheduleResponse> getLecturerSchedule() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        List<ClassSchedule> schedules = scheduleRepositoryStudent
                .findSchedulesByLecturerId(lecturer.getId());

        return schedules.stream().map(cs -> {
            var classEntity = cs.getClassEntity();

            String courseName = courseRepository.findNameById(classEntity.getCourseId())
                    .orElse("N/A");

            Integer totalWeeks = calculateTotalWeeks(classEntity.getId(), classEntity.getCourseId());

            return ScheduleResponse.builder()
                    .courseName(courseName)
                    .classCode(classEntity.getCode())
                    .roomName(cs.getRoom().getName())
                    .dayOfWeek(cs.getDayOfWeek())
                    .shiftName(cs.getShift().getName())
                    .startTime(cs.getShift().getStartTime())
                    .endTime(cs.getShift().getEndTime())
                    .lecturerName(null) // không hiện tên giảng viên
                    .totalWeeks(totalWeeks)
                    .build();
        }).collect(Collectors.toList());
    }
}