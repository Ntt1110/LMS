package com.example.LMS.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseProposalRequest {

    @NotNull(message = "Khoa không được để trống")
    private Long departmentId;

    @NotBlank(message = "Mã môn học không được để trống")
    private String code;

    @NotBlank(message = "Tên môn học không được để trống")
    private String name;

    @NotNull(message = "Số tín chỉ không được để trống")
    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    private Integer credits;

    @Min(value = 0, message = "Số tiết lý thuyết không được âm")
    private Integer theoreticalHours = 0;

    @Min(value = 0, message = "Số tiết thực hành không được âm")
    private Integer practicalHours = 0;

    private String description;
}