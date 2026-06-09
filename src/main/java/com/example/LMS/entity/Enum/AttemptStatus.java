package com.example.LMS.entity.Enum;

public enum AttemptStatus {
    IN_PROGRESS, // Đang làm bài
    COMPLETED,   // Đã nộp (Sinh viên chủ động bấm nút nộp bài)
    FORCED       // Tự động nộp (Hệ thống tự thu bài khi hết thời gian)
}