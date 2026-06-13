package com.example.LMS.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {

    // Tuỳ chọn: đổi sang khoa khác
    private Long departmentId;

    // Không cho sửa code — mã môn học là định danh nghiệp vụ

    /**
     * Tên môn học: chữ cái (bao gồm tiếng Việt), số, dấu ngoặc, dấu gạch ngang, khoảng trắng
     * Dài 2–150 ký tự. null = giữ nguyên tên cũ.
     */
    @Size(min = 2, max = 150, message = "Tên môn học phải từ 2 đến 150 ký tự")
    @Pattern(
            regexp = "^[\\p{L}0-9()\\-\\s]+$",
            message = "Tên môn học không được chứa ký tự đặc biệt (chỉ cho phép chữ, số, dấu ngoặc, dấu gạch ngang)"
    )
    private String name;

    @Min(value = 1, message = "Số tín chỉ phải ít nhất là 1")
    @Max(value = 10, message = "Số tín chỉ không được vượt quá 10")
    private Integer credits;

    @Min(value = 0, message = "Số tiết lý thuyết không được âm")
    @Max(value = 300, message = "Số tiết lý thuyết không được vượt quá 300")
    private Integer theoreticalHours;

    @Min(value = 0, message = "Số tiết thực hành không được âm")
    @Max(value = 300, message = "Số tiết thực hành không được vượt quá 300")
    private Integer practicalHours;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}