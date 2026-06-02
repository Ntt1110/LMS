package com.example.LMS.dto.response;

import com.example.LMS.entity.model.Department;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
@Getter
@Builder

public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //THoong tin truong khoa
    private Long managerId;
    private String managerUsername;
    private String managerFullName;

    public static DepartmentResponse fromEntity(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                // Thông tin trưởng khoa
                .managerId(department.getManager().getId())
                .managerUsername(department.getManager().getUsername())
                .managerFullName(
                        department.getManager().getProfile() != null
                                ? department.getManager().getProfile().getFullName()
                                : null
                )
                .build();

    }


}
