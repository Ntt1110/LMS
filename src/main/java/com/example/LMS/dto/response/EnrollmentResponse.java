package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EnrollmentResponse {
    private Long enrollmentId;
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;
    private String lecturerName;
    private String semesterCode;
    private String status;         // REGISTERED, OFFICIAL, DROPPED
    private LocalDateTime enrolledAt;
}