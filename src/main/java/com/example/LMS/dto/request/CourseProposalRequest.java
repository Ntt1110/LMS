package com.example.LMS.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseProposalRequest {

    @NotNull(message = "Khoa không được để trống")
    private Long departmentId;

    /**
     * Mã môn học: chữ hoa + số, 3-10 ký tự
     * Ví dụ hợp lệ  : SE001, CNTT101, ENG01
     * Ví dụ không hợp lệ: se001 (thường), SE 01 (có space), SE@01 (ký tự đặc biệt)
     */
    @NotBlank(message = "Mã môn học không được để trống")
    @Pattern(
            regexp = "^[A-Z0-9]{2,10}$",
            message = "Mã môn học chỉ được chứa chữ IN HOA và số, từ 2 đến 10 ký tự (ví dụ: SE001, CNTT101)"
    )
    private String code;

    /**
     * Tên môn học: chữ cái (bao gồm tiếng Việt), số, dấu ngoặc, dấu gạch ngang, khoảng trắng
     * Dài 2–150 ký tự. Không bắt đầu/kết thúc bằng khoảng trắng.
     */
    @NotBlank(message = "Tên môn học không được để trống")
    @Size(min = 2, max = 150, message = "Tên môn học phải từ 2 đến 150 ký tự")
    @Pattern(
            regexp = "^[\\p{L}0-9()\\-\\s]+$",
            message = "Tên môn học không được chứa ký tự đặc biệt (chỉ cho phép chữ, số, dấu ngoặc, dấu gạch ngang)"
    )
    private String name;

    @NotNull(message = "Số tín chỉ không được để trống")
    @Min(value = 1, message = "Số tín chỉ phải ít nhất là 1")
    @Max(value = 10, message = "Số tín chỉ không được vượt quá 10")
    private Integer credits;

    @Min(value = 0, message = "Số tiết lý thuyết không được âm")
    @Max(value = 300, message = "Số tiết lý thuyết không được vượt quá 300")
    private Integer theoreticalHours = 0;

    @Min(value = 0, message = "Số tiết thực hành không được âm")
    @Max(value = 300, message = "Số tiết thực hành không được vượt quá 300")
    private Integer practicalHours = 0;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}