package com.example.LMS.controller;

import com.example.LMS.dto.request.LoginRequest;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.dto.response.UserProfileResponse;
import com.example.LMS.security.JwtService;


import com.example.LMS.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuthService authService;




    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        // 1. Giao cho AuthService xử lý logic và lấy kết quả
        AuthResponse responseData = authService.login(request);

        // 2. Bọc kết quả vào ApiResponse và trả về cho Frontend
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .message("Đăng nhập thành công!")
                .data(responseData)
                .build();
    }

    @GetMapping("/profile/me")
    @Operation(summary = "Lấy thông tin hồ sơ của chính người dùng đang đăng nhập", description = "Tự động nhận diện User qua Token gán ở Header")
    public ApiResponse<UserProfileResponse> getCurrentProfile() {
        // Gọi Service xử lý trích xuất SecurityContext
        UserProfileResponse myProfile = authService.getCurrentUserProfile();

        return ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Lấy hồ sơ cá nhân thành công!")
                .data(myProfile)
                .build();
    }
}