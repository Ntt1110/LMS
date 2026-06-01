package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MajorListRequest {

    // Tìm kiếm theo tên hoặc mã ngành
    private String keyword;

    // Lọc theo trạng thái (null = tất cả)
    private Boolean isActive;

    // Lọc theo khoa (department id)
    private Long departmentId;

    // Phân trang
    private int page = 0;
    private int size = 10;

    // Sắp xếp
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}