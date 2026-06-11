package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RegistrationPeriodDetailResponse {

    // ── Thông tin đợt đăng ký ──────────────────────────────────
    private Long id;
    private String name;          // Tên đăng ký
    private LocalDateTime startTime;  // Thời gian mở
    private LocalDateTime endTime;    // Thời gian đóng
    private String status;

    // ── Thông tin học kỳ ──────────────────────────────────────
    private Long semesterId;
    private String semesterName;  // Tên học kỳ (VD: "Học kỳ 1 - 2024-2025")

    // ── Thống kê tổng hợp ─────────────────────────────────────
    private Integer totalClasses;         // Tổng số lớp mở
    private Integer totalEnrollments;     // Tổng lượt đăng ký

    // ── Danh sách lớp đang mở ─────────────────────────────────
    private List<ClassInPeriodResponse> classes;
}