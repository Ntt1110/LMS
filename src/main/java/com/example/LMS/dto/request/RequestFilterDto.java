package com.example.LMS.dto.request;

import com.example.LMS.entity.Enum.ClassOpenningStatus;
import lombok.Data;

@Data
public class RequestFilterDto {
    // --- Các tham số phân trang (Mặc định trang 0, mỗi trang 10 bản ghi) ---
    private int page = 0;
    private int size = 10;

    // --- Các bộ lọc động ---
    private ClassOpenningStatus status; // PENDING, APPROVED, REJECTED
    private String search;        // Tìm kiếm theo tên đề xuất hoặc mã môn học
    private Long semesterId;      // Lọc theo học kỳ nếu cần
}
