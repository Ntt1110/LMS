package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống!")
    private String username;
    @NotBlank(message = "Mật khẩu không được để trống!")
    private String password;

    // Getter và Setter (Nếu bạn dùng Lombok thì chỉ cần thêm @Data ở trên cùng)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
