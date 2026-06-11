package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMajorRequest {

    @NotNull(message = "Khoa không được để trống!")
    private Long departmentId;

    @NotBlank(message = "Mã ngành không được để trống!")
    private String code;

    @NotBlank(message = "Tên ngành không được để trống!")
    private String name;

    @NotNull(message = "Số tín chỉ tối thiểu không được để trống!")
    private Integer requiredMinimumCredits;

    private String description;
}