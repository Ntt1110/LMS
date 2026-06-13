package com.example.LMS.controller;

import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.NotificationResponse;
import com.example.LMS.entity.model.Notification;
import com.example.LMS.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

// =========================================================================
        // 🌟 1. API DÀNH CHO GIÁO VỤ: TẠO VÀ PHÁT HÀNH THÔNG BÁO MỞ LỚP
        // =========================================================================
        @PostMapping("/create")
        @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')") // Bảo vệ gác cổng phân quyền
        @Operation(summary = "Giáo vụ tạo mới một thông báo mở lớp (Toàn trường hoặc theo Khoa)")
        public ApiResponse<String> createNotification(
                @RequestParam String title,
                @RequestParam String message,
                @RequestParam(required = false) Long departmentId,
                @RequestParam(required = false) Long periodId
        ) {
            // Gọi Service xử lý lưu Database
            notificationService.createClassOpeningNotification(title, message, departmentId, periodId);

            return ApiResponse.<String>builder()
                    .code(201)
                    .message("Phát hành thông báo mở lớp học phần thành công!")
                    .data("NOTIFICATION_CREATED_SUCCESS")
                    .build();
        }

    // 🔔 API dành cho Sinh viên xem trên Dashboard Vue 3
    @GetMapping("/my")
    @Operation(summary = "Sinh viên lấy danh sách thông báo mở lớp thuộc Khoa của mình hoặc Toàn trường")
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        return ApiResponse.<List<NotificationResponse>>builder()
                .code(200)
                .message("Tải danh sách thông báo thành công!")
                .data(notificationService.getMyNotifications())
                .build();
    }
}
