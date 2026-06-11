package com.example.LMS.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;          // ID của bản ghi Profile
    private Long userId;      // ID của User liên kết (Khóa ngoại)
    private String fullName;  // Họ và tên người dùng
    private String phone;     // Số điện thoại

    // Sử dụng String cho ngày sinh để Frontend dễ định dạng hiển thị (hoặc dùng LocalDate)
    private String birthday;

    private String gender;    // Giới tính (MALE, FEMALE, OTHER) dưới dạng chuỗi chữ
    private String avatarUrl; // Đường dẫn ảnh đại diện
    private String address;   // Địa chỉ cư trú

    private String role; // 🌟 Thêm trường này để Frontend dễ phân loại UI

    // 2. Thông tin bổ sung riêng cho STUDENT
    private String studentCode;
    private Integer cohort;
    private String majorName;

    // 3. Thông tin bổ sung riêng cho TEACHER
    private String employeeCode;
    private String academicTitle;
    private String specialization;
    private String departmentName;
}