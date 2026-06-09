package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class ExamAttemptResponseDto {
        private Long attemptId;
        private Long examId;
        private LocalDateTime startTime;
        private String status;
    }

