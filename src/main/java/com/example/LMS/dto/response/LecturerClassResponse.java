package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Danh sách lớp được phân công cho giảng viên
@Getter
@Builder
public class LecturerClassResponse {
    private Long classId;
    private String classCode;
    private String courseName;
    private String semesterCode;
    private String status;
    private Integer maxStudents;
    private Integer currentStudents;

    // Lịch học
    private Integer dayOfWeek;
    private String shiftName;
    private String roomName;
}