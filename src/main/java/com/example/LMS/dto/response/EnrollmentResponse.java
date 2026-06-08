package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EnrollmentResponse {
    // Môn học
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer credits;

    // Lớp học phần
    private Long classId;
    private String classCode;

    // Lịch học
    private Integer dayOfWeek;   // Thứ mấy
    private String shiftName;    // Ca mấy
    private String roomName;     // Phòng nào

    // Trạng thái đăng ký (REGISTERED, OFFICIAL, DROPPED)
    private String status;

    private LocalDateTime enrolledAt;
}