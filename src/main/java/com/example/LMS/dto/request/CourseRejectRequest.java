package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseRejectRequest {

    @NotNull(message = "ID môn học không được để trống")
    private Long courseId;

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectReason;
}