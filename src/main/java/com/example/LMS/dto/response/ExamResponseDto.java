package com.example.LMS.dto.response;

import com.example.LMS.entity.Enum.ExamStatus;
import com.example.LMS.entity.Enum.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResponseDto {
    private Long id;
    private Long classId;
    private String title;
    private String description;
    private ExamType examType;       // REGULAR, MIDTERM, FINAL
    private Integer timeLimit;       // Thời gian làm bài (Phút)
    private Integer totalQuestions;  // Tổng số câu hỏi
    private ExamStatus status;       // DRAFT, PUBLISHED, CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}