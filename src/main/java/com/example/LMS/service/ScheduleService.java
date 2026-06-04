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
            String courseCode = courseRepository.findById(classEntity.getCourseId())
                    .map(c -> c.getCode()).orElse("N/A");
            Integer credits = courseRepository.findById(classEntity.getCourseId())
                    .map(c -> c.getCredits()).orElse(null);
            String lecturerName = classEntity.getLecturerId() != null
                    ? userProfileRepository.findByUserId(classEntity.getLecturerId())
                    .map(p -> p.getFullName()).orElse("Chưa phân công")
                    : "Chưa phân công";

            return ScheduleResponse.builder()
                    .classId(classEntity.getId())
                    .classCode(classEntity.getCode())
                    .courseName(courseName)
                    .courseCode(courseCode)
                    .credits(credits)
                    .dayOfWeek(cs.getDayOfWeek())
                    .shiftName(cs.getShift().getName())
                    .startTime(cs.getShift().getStartTime())
                    .endTime(cs.getShift().getEndTime())
                    .roomName(cs.getRoom().getName())
                    .roomType(cs.getRoom().getType().name())
                    .lecturerName(lecturerName)
                    .semesterCode(classEntity.getSemester().getSemesterCode())
                    .build();
        }).collect(Collectors.toList());
    }
}