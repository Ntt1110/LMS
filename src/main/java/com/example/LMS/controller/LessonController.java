package com.example.LMS.controller;


import com.example.LMS.dto.request.CreateLessonRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.LessonResponseDto;
import com.example.LMS.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/{classId}/lessons")
    @PreAuthorize("hasAuthority('LESSON_VIEW')") // Giảng viên, sinh viên có quyền xem lớp đều gọi được
    @Operation(summary = "Lấy danh sách bài học của một lớp học phần cụ thể (Xem theo từng lớp)")
    public ApiResponse<List<LessonResponseDto>> getLessonsByClass(@PathVariable Long classId) {

        List<LessonResponseDto> lessons = lessonService.getLessonsByClass(classId);

        return ApiResponse.<List<LessonResponseDto>>builder()
                .code(200)
                .message("Tải danh sách bài học của lớp học phần thành công!")
                .data(lessons)
                .build();
    }

    @PostMapping(value = "/classes/{classId}/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('LESSON_CREATE')")
    @Operation(summary = "Tạo mới một bài học kèm file tài liệu (Hỗ trợ multipart/form-data)")
    public ApiResponse<String> createLesson(
            @PathVariable Long classId,
            @Valid @ModelAttribute CreateLessonRequestDto dto) {

        lessonService.createLessonWithFiles(classId, dto);

        return ApiResponse.<String>builder()
                .code(201)
                .message("Tạo bài học mới và đính kèm tài liệu thành công!")
                .data("CREATED")
                .build();
    }
}