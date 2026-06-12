package com.example.LMS.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {

    // Cho phép đổi sang khoa khác (tuỳ chọn)
    private Long departmentId;

    // Không cho sửa code (mã môn học là định danh duy nhất, không đổi)

    private String name;

    @Min(value = 1, message = "Số tín chỉ phải lớn hơn 0")
    private Integer credits;

    @Min(value = 0, message = "Số tiết lý thuyết không được âm")
    private Integer theoreticalHours;

    @Min(value = 0, message = "Số tiết thực hành không được âm")
    private Integer practicalHours;

    private String description;
}