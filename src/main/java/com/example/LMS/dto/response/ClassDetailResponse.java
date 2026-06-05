package com.example.LMS.dto.response;

import com.example.LMS.entity.model.ClassEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClassDetailResponse {

    private Long id;
    private String code;
    private String status;
    private Integer maxStudents;

    // Học kỳ
    private Long semesterId;
    private String semesterCode;
    private String academicYear;

    // Môn học
    private Long courseId;
    private String courseName;
    private String courseCode;

    // Khoa
    private Long departmentId;
    private String departmentName;

    // Quản lý & Giảng viên
    private Long managerId;
    private String managerName;
    private Long lecturerId;
    private String lecturerName;

    private LocalDateTime createdAt;
}