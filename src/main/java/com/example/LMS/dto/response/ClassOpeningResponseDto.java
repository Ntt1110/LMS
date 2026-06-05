package com.example.LMS.dto.response;

import com.example.LMS.entity.Enum.ClassOpenningStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassOpeningResponseDto {
    private Long requestId;
    private String semesterCode;     // Ví dụ: HK1-2026
    private Long courseId;
    private String courseName;       // Tên môn học lấy từ bảng courses bổ sung sau
    private Long requesterId;
    private String requesterName;    // Họ tên người đề xuất (Lấy từ user_profiles)
    private Integer expectedStudents;
    private String note;
    private ClassOpenningStatus status;
    private LocalDateTime createdAt;

}