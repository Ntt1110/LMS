package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ClassResponse {
    private Long id;
    private String code;
    private String status;
    private Integer maxStudents;
    private LocalDateTime createdAt;
    private Long semesterId;
    private String semesterCode;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long managerId;
    private String managerName;
    private Long lecturerId;
    private String lecturerName;


    private String room;
}