package com.example.LMS.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonMaterialResponseDto {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize; // Tính bằng Byte
}