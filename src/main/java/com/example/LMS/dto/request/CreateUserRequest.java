package com.example.LMS.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8, max = 32, message = "Password phải từ 8 đến 32 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password phải bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt (@$!%*?&)"
    )
    private String password;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotEmpty(message = "Phải gán ít nhất 1 Vai trò (Role) cho người dùng")
    private Set<Long> roleIds; // Danh sách ID các Role muốn gán (VD: Giảng viên, Sinh viên...)
}