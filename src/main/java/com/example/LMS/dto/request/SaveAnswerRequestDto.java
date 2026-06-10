package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveAnswerRequestDto {

    @NotNull(message = "ID câu hỏi không được để trống")
    private Long questionId;

    @NotNull(message = "ID đáp án chọn không được để trống")
    private Long selectedOptionId;
}