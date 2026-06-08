package com.example.LMS.dto.request;

import com.example.LMS.entity.Enum.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateExamRequestDto {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Loại bài kiểm tra không được để trống")
    private ExamType examType;

    @NotNull(message = "Thời gian làm bài không được để trống")
    private Integer timeLimit;

    // 🌟 Danh sách câu hỏi kèm theo
    private List<QuestionDto> questions;

    // --- CÁC CLASS CON BÊN TRONG ---
    @Data
    public static class QuestionDto {
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        private String content;

        private Integer orderIndex;

        // 🌟 Danh sách đáp án của câu hỏi này
        private List<OptionDto> options;
    }

    @Data
    public static class OptionDto {
        @NotBlank(message = "Nội dung đáp án không được để trống")
        private String content;

        @NotNull(message = "Phải xác định đáp án đúng/sai")
        private Boolean isCorrect;

        private Integer orderIndex;
    }
}