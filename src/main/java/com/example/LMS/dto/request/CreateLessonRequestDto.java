package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateLessonRequestDto {

    @NotBlank(message = "Tiêu đề bài học không được để trống!")
    private String title;

    private String description;

    private Integer orderIndex;

    private Boolean isPublished = false;

    // 🌟 KHAY HỨNG FILE: Nhận 1 hoặc nhiều file cùng lúc từ Vue 3 truyền lên
    private List<MultipartFile> files;
}
