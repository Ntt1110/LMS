package com.example.LMS.dto.request;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RegistrationPeriodRequestDto {

    @NotNull(message = "Vui lòng chọn học kỳ áp dụng!")
    private Long semesterId;

    @NotBlank(message = "Vui lòng nhập tên đợt đăng ký (Ví dụ: Đợt 1 - Đăng ký chính thức)!")
    private String name;

    private String type = "NORMAL"; // Mặc định là đợt đăng ký bình thường, hoặc đợt Đăng ký bổ sung (LATE)

    @NotNull(message = "Thời gian bắt đầu không được để trống!")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống!")
    @Future(message = "Thời gian kết thúc phải là một thời điểm trong tương lai!")
    private LocalDateTime endTime;

    @NotEmpty(message = "Vui lòng chọn ít nhất một Khóa sinh viên được phép đăng ký!")
    private List<Integer> targetCohorts; // Frontend gửi mảng số: [2024, 2025]

    @NotEmpty(message = "Vui lòng chọn ít nhất một Khoa được phép đăng ký!")
    private List<Long> targetDepartments; // Frontend gửi mảng ID khoa: [1, 2]
}
