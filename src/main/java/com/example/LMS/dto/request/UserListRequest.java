package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserListRequest {

    // Tìm kiếm theo username hoặc email
    private String keyword;

    // Lọc theo trạng thái (null = tất cả, true = active, false = inactive)
    private Boolean isActive;

    // Lọc theo role code (ví dụ: "ADMIN", "STUDENT")
    private String roleCode;

    // Phân trang
    private int page = 0;
    private int size = 10;

    // Sắp xếp: "createdAt", "username", "email"
    private String sortBy = "createdAt";

    // Chiều sắp xếp: "asc" hoặc "desc"
    private String sortDirection = "desc";
}