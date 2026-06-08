package com.example.LMS.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponseDto {
    private Long id;
    private Long classId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}