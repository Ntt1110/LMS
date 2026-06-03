package com.example.LMS.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SemesterCreateRequest {

    @NotBlank(message = "Mã học kỳ không được để trống")
    private String semesterCode;

    @NotBlank(message = "Năm học không được để trống")
    private String academicYear;

    @NotNull(message = "Số thứ tự học kỳ không được để trống")
    @Min(value = 1, message = "Số thứ tự học kỳ phải >= 1")
    @Max(value = 3, message = "Số thứ tự học kỳ phải <= 3")
    private Integer semesterNumber;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;
}