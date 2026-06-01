package com.example.LMS.dto.response;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;

    // Thông tin cơ bản và Profile của User
    private UserInfo user;

    // Danh sách quyền hạn chi tiết (VD: ["USER_VIEW", "MAJOR_CREATE"])
    private List<String> permissions;

    // --- LỚP NỘI BỘ (Inner Class) để gom nhóm thông tin User ---






    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName; // Lấy từ bảng user_profiles
        private String avatarUrl; // Lấy từ bảng user_profiles
        private List<String> roles; // VD: ["ADMIN", "INSTRUCTOR"]
    }
}
