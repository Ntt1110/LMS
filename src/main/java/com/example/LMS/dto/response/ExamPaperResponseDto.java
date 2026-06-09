package com.example.LMS.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamPaperResponseDto {
    private Long examId;
    private String title;
    private Integer timeLimit; // Dùng để đếm ngược
    private Integer totalQuestions;

    // Mảng câu hỏi
    private List<ExamQuestionDto> questions;

    @Data
    @Builder
    public static class ExamQuestionDto {
        private Long questionId;
        private String content;
        // Mảng đáp án (Đã giấu isCorrect)
        private List<ExamOptionDto> options;
    }

    @Data
    @Builder
    public static class ExamOptionDto {
        private Long optionId;
        private String content;

    }
}