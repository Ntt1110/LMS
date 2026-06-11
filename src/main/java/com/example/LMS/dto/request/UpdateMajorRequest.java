package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMajorRequest {

    private Long departmentId;

    private String name;

    private Integer requiredMinimumCredits;

    private String description;

    private Boolean isActive;

    private String lockReason;
}