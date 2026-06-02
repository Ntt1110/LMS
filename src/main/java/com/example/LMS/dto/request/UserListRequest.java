package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserListRequest {

    // Tìm kiếm theo username hoặc email
    private String keyword;

    // Lọc theo trạng thái (null = tất cả, true = active, false = inactive)
    private Boolean isActive;

    // Lọc theo role code (ví dụ: "ADMIN", "STUDENT")
    private String roleCode;

    // Lọc theo ngành học (chỉ có ý nghĩa với STUDENT)
    // JOIN: users -> student_profiles -> majors
    private Long majorId;

    // Lọc theo khoa:
//   STUDENT:    users -> student_profiles -> majors -> departments
//   INSTRUCTOR: users -> teacher_profiles -> departments
    private Long departmentId;

    private List<Long> excludeUserIds;

    // Phân trang
    private int page = 0;
    private int size = 10;

    // Sắp xếp: "createdAt", "username", "email"
    private String sortBy = "createdAt";

    // Chiều sắp xếp: "asc" hoặc "desc"
    private String sortDirection = "desc";
}