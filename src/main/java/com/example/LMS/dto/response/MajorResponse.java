package com.example.LMS.dto.response;

import com.example.LMS.entity.model.Major;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MajorResponse {

    // === Từ bảng majors ===
    private Long id;
    private String code;
    private String name;
    private Integer requiredMinimumCredits;
    private String description;
    private Boolean isActive;
    private String lockReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Từ bảng departments (JOIN) ===
    private Long departmentId;
    private String departmentCode;
    private String departmentName;

    public static MajorResponse fromEntity(Major major) {
        return MajorResponse.builder()
                .id(major.getId())
                .code(major.getCode())
                .name(major.getName())
                .requiredMinimumCredits(major.getRequiredMinimumCredits())
                .description(major.getDescription())
                .isActive(major.getIsActive())
                .lockReason(major.getLockReason())
                .createdAt(major.getCreatedAt())
                .updatedAt(major.getUpdatedAt())
                // Thông tin khoa
                .departmentId(major.getDepartment().getId())
                .departmentCode(major.getDepartment().getCode())
                .departmentName(major.getDepartment().getName())
                .build();
    }
}