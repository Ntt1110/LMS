package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
// Chi tiết lớp học phần dành cho sinh viên xem trước khi đăng ký
public class ClassDetailForStudentResponse {

    private Long classId;
    private String classCode;
    private String status;
    private Integer maxStudents;
    private Integer currentStudents;   // Số SV đã đăng ký
    private String semesterCode;

    // Môn học
    private String courseCode;
    private String courseName;
    private Integer credits;

    // Giảng viên
    private String lecturerName;

    // Lịch học (thứ, ca, phòng)
    private List<ScheduleInfo> schedules;

    @Getter
    @Builder
    public static class ScheduleInfo {
        private Integer dayOfWeek;   // 2=Thứ 2, 3=Thứ 3...
        private String shiftName;    // Ca 1 (Sáng)...
        private LocalTime startTime;
        private LocalTime endTime;
        private String roomName;
        private String roomType;
    }
}