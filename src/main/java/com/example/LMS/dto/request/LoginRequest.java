package com.example.LMS.dto.request;

public class LoginRequest {

    private String username;
    private String password;

    // Getter và Setter (Nếu bạn dùng Lombok thì chỉ cần thêm @Data ở trên cùng)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
