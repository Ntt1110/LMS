package com.example.LMS.dto.response;

import com.example.LMS.entity.model.Course;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CourseResponse {
    // === Từ bảng courses ===
    private Long id;
    private String code;
    private String name;
    private Integer credits;
    private Integer theoreticalHours;
    private Integer practicalHours;
    private String description;
    private String status; //pending, approved, rejected
    private String rejectReason;
    private String lockReason;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime updateAt;

    // === Từ bảng departments (JOIN) ===
    private Long departmentId;
    private String departmentCode;
    private String departmentName;

    public static CourseResponse fromEntity(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .credits(course.getCredits())
                .theoreticalHours(course.getTheoreticalHours())
                .practicalHours(course.getPracticalHours())
                .description(course.getDescription())
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .rejectReason(course.getRejectReason())
                .lockReason(course.getLockReason())
                .createdAt(course.getCreatedAt())
                .updateAt(course.getUpdatedAt())
                // Thông tin khoa
                .departmentId(course.getDepartment().getId())
                .departmentCode(course.getDepartment().getCode())
                .departmentName(course.getDepartment().getName())
                .build();
    }


}
