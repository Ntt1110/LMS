package com.example.LMS.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GenerateClassRequestDto {
    @NotNull(message = "Học kỳ áp dụng không được để trống!")
    private Long semesterId;     // 🌟 THÊM MỚI THEO YÊU CẦU CỦA TRUNG

    @NotNull(message = "Môn học áp dụng không được để trống!")
    private Long courseId;       // 🌟 THÊM MỚI THEO YÊU CẦU CỦA TRUNG

    @NotNull(message = "Vui lòng chọn Trưởng khoa/Trưởng bộ môn quản lý lớp!")
    private Long managerId;     // Người quản lý chung gán cho tất cả các lớp

    @NotEmpty(message = "Danh sách lớp học phần khởi tạo không được để trống!")
    @Valid
    private List<ClassConfigItem> classes; // 🌟 DANH SÁCH CÁC LỚP DO FRONTEND GỬI VỀ

    @Data
    public static class ClassConfigItem {
        @NotNull(message = "Vui lòng chọn Phòng học!")
        private Long roomId;

        @NotNull(message = "Vui lòng chọn Ca học!")
        private Long shiftId;

        @NotNull(message = "Vui lòng chọn Thứ trong tuần!")
        private Integer dayOfWeek;

        @NotNull(message = "Vui lòng nhập số lượng sinh viên tối đa cho lớp!")
        @Min(value = 1, message = "Sĩ số lớp tối thiểu phải là 1 học viên!")
        private Integer maxStudents; // Sĩ số riêng cho từng lớp cấu hình từ Form
    }
}
