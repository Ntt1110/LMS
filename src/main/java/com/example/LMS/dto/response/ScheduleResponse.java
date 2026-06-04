package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ScheduleResponse {
    // Thông tin lớp
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;

    // Thông tin lịch học
    private Integer dayOfWeek;     // 2=Thứ 2, 3=Thứ 3...
    private String shiftName;      // Ca 1 (Sáng)...
    private LocalTime startTime;
    private LocalTime endTime;
    private String roomName;
    private String roomType;

    // Thông tin giảng viên & học kỳ
    private String lecturerName;
    private String semesterCode;
}