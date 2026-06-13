package com.example.LMS.controller;

import com.example.LMS.dto.request.LoginRequest;
import com.example.LMS.dto.request.ResetPasswordDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.dto.response.UserProfileResponse;
import com.example.LMS.entity.model.PasswordReset;
import com.example.LMS.entity.model.User;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.PasswordResetRepository;
import com.example.LMS.repository.UserRepository;
import com.example.LMS.security.JwtService;


import com.example.LMS.service.AuthService;
import com.example.LMS.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuthService authService;

    private final AuthorizationService authorizationService;





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
    @PostMapping("/refresh")
    @Operation(summary = "Đổi mã Refresh Token để lấy cặp Access Token mới khi mã cũ hết hạn")
    public ApiResponse<AuthResponse> refreshToken(@RequestParam String refreshToken) {

        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .message("Cấp mới Access Token thành công!")
                .data(authorizationService.refreshAccessToken(refreshToken))
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
    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu mới sử dụng Token xác thực từ Email")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {

        authService.resetPassword(dto);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Đặt lại mật khẩu mới thành công! Bạn có thể dùng mật khẩu này để đăng nhập.")
                .data("PASSWORD_RESET_SUCCESS")
                .build();
    }


    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản (Yêu cầu nhập mật khẩu cũ và mới)")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody com.example.LMS.dto.request.ChangePasswordDto dto) {

        authService.changePassword(dto);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Thay đổi mật khẩu tài khoản thành công!")
                .data("PASSWORD_CHANGED_SUCCESS")
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Nhập Email để hệ thống sinh Token và gửi link đặt lại mật khẩu")
    public ApiResponse<String> forgotPassword(
            @jakarta.validation.Valid @RequestBody com.example.LMS.dto.request.ForgotPasswordDto dto) {

        authService.processForgotPassword(dto);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Hệ thống đã gửi mã xác thực về Email của bạn. Vui lòng kiểm tra hòm thư!")
                .data("FORGOT_PASSWORD_SUCCESS")
                .build();
    }
}