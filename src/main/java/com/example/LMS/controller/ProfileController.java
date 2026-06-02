package com.example.LMS.controller;

import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "Các API quản lý thông tin và hồ sơ cá nhân")
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROFILE_UPDATE')") // 🛡️ Yêu cầu mã quyền số 19 chi tiết của Trung
    @Operation(summary = "Cập nhật ảnh đại diện (Avatar) cho người dùng đang đăng nhập")
    public ApiResponse<String> updateAvatar(@RequestParam("file") MultipartFile file) {

        String newAvatarUrl = profileService.uploadAvatar(file);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Cập nhật ảnh đại diện thành công!")
                .data(newAvatarUrl) // Trả URL về để Frontend đổi ảnh hiển thị ngay tại chỗ
                .build();
    }
}