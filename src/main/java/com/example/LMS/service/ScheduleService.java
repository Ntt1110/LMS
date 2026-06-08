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

    public List<ScheduleResponse> getMySchedule() {

        // 1. Lấy sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Lấy toàn bộ lịch học của sinh viên
        List<ClassSchedule> schedules = scheduleRepositoryStudent
                .findSchedulesByStudentId(student.getId());

        // 3. Map sang DTO
        return schedules.stream().map(cs -> {
            var classEntity = cs.getClassEntity();

            String courseName = courseRepository.findNameById(classEntity.getCourseId())
                    .orElse("N/A");
            String lecturerName = classEntity.getLecturerId() != null
                    ? userProfileRepository.findByUserId(classEntity.getLecturerId())
                    .map(p -> p.getFullName()).orElse("Chưa phân công")
                    : "Chưa phân công";

            return ScheduleResponse.builder()
                    .courseName(courseName)
                    .classCode(classEntity.getCode())
                    .roomName(cs.getRoom().getName())
                    .dayOfWeek(cs.getDayOfWeek())
                    .shiftName(cs.getShift().getName())
                    .startTime(cs.getShift().getStartTime())
                    .endTime(cs.getShift().getEndTime())
                    .lecturerName(lecturerName)
                    .build();
        }).collect(Collectors.toList());
    }

    public List<ScheduleResponse> getLecturerSchedule() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // Lấy lịch dạy theo lecturerId
        List<ClassSchedule> schedules = scheduleRepositoryStudent
                .findSchedulesByLecturerId(lecturer.getId());

        return schedules.stream().map(cs -> {
            var classEntity = cs.getClassEntity();

            String courseName = courseRepository.findNameById(classEntity.getCourseId())
                    .orElse("N/A");

            return ScheduleResponse.builder()
                    .courseName(courseName)
                    .classCode(classEntity.getCode())
                    .roomName(cs.getRoom().getName())
                    .dayOfWeek(cs.getDayOfWeek())
                    .shiftName(cs.getShift().getName())
                    .startTime(cs.getShift().getStartTime())
                    .endTime(cs.getShift().getEndTime())
                    .lecturerName(null) // không hiện tên giảng viên
                    .build();
        }).collect(Collectors.toList());
    }
}