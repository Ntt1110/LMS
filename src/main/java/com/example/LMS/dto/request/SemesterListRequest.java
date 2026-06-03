package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SemesterListRequest {

    // Tìm theo mã học kỳ hoặc năm học
    private String keyword;

    // Lọc theo trạng thái: ACTIVE | CLOSED (null = tất cả)
    private String status;

    // Lọc theo năm học (ví dụ: 2025-2026)
    private String academicYear;

    // Phân trang
    private int page = 0;
    private int size = 10;

    // Sắp xếp
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}