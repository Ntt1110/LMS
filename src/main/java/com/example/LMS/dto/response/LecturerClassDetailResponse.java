package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

// Chi tiết lớp dành cho giảng viên
@Getter
@Builder
public class LecturerClassDetailResponse {

    // Thông tin lớp
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;
    private String status;
    private Integer maxStudents;
    private Integer currentStudents;

    // Lịch học
    private Integer dayOfWeek;
    private String shiftName;
    private String roomName;
}