package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// Chi tiết lớp + danh sách sinh viên dành cho giảng viên
@Getter
@Builder
public class LecturerClassDetailResponse {

    // Thông tin lớp
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;
    private String semesterCode;
    private String status;
    private Integer maxStudents;
    private Integer currentStudents;

    // Lịch học
    private Integer dayOfWeek;
    private String shiftName;
    private String roomName;

    // Danh sách sinh viên đã đăng ký
    private List<StudentInfo> students;

    @Getter
    @Builder
    public static class StudentInfo {
        private Long studentId;
        private String fullName;
        private String studentCode;  // Mã số sinh viên
        private String email;
        private String enrollmentStatus; // REGISTERED, OFFICIAL, DROPPED
        private LocalDateTime enrolledAt;
    }
}