package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseApproveRequest {

    @NotNull(message = "ID môn học không được để trống")
    private Long courseId;

    // APPROVED hoặc REJECTED
    @NotBlank(message = "Trạng thái không được để trống")
    private String action;

    // Bắt buộc khi action = REJECTED
    private String rejectReason;
}