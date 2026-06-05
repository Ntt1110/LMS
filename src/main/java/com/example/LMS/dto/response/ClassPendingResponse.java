package com.example.LMS.dto.response;

import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.Course;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassPendingResponse {

    private Long id;
    private String code;
    private Integer maxStudents;
    private String status;

    // Thông tin môn học
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer credits;

    // Thông tin khoa
    private Long departmentId;
    private String departmentName;

    public static ClassPendingResponse fromEntity(ClassEntity c, Course course) {
        return ClassPendingResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .maxStudents(c.getMaxStudents())
                .status(c.getStatus().name())
                .courseId(course != null ? course.getId() : null)
                .courseCode(course != null ? course.getCode() : null)
                .courseName(course != null ? course.getName() : null)
                .credits(course != null ? course.getCredits() : null)
                .departmentId(course != null && course.getDepartment() != null
                        ? course.getDepartment().getId() : null)
                .departmentName(course != null && course.getDepartment() != null
                        ? course.getDepartment().getName() : null)
                .build();
    }
}