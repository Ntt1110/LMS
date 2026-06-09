package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamResultResponseDto {
    private Long attemptId;
    private Double score;           // Điểm thi (Thang 10)
    private Integer correctAnswers; // Số câu đúng
    private Integer totalQuestions; // Tổng số câu
    private LocalDateTime submitTime;
    private String status;          // COMPLETED
}
