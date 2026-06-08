package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentOfClassResponse {
    private Long studentId;
    private String fullName;
    private String avatarUrl;
    private String studentCode;
    private String email;
    private String enrollmentStatus; // REGISTERED, OFFICIAL, DROPPED
}