package com.example.LMS.controller;


import com.example.LMS.dto.request.CreateExamRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.ExamResponseDto;
import com.example.LMS.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;

    // =========================================================================
    // 🌟 API 1: CHO GIẢNG VIÊN (Xem tất cả)
    // =========================================================================
    @GetMapping("/classes/{classId}/exams")
    @PreAuthorize("hasAuthority('EXAM_VIEW')") // 🛡️ Gác cổng mã quyền 38
    @Operation(summary = "Lấy toàn bộ danh sách bài kiểm tra của lớp (Dành cho Giảng viên)")
    public ApiResponse<List<ExamResponseDto>> getAllExams(@PathVariable Long classId) {

        return ApiResponse.<List<ExamResponseDto>>builder()
                .code(200)
                .message("Tải danh sách bài kiểm tra thành công!")
                .data(examService.getAllExamsByClass(classId))
                .build();
    }

    // =========================================================================
    // 🌟 API 2: CHO SINH VIÊN (Chỉ xem bài khả dụng)
    // =========================================================================
    @GetMapping("/classes/{classId}/exams/active")
    @PreAuthorize("hasAuthority('EXAM_VIEW')")
    @Operation(summary = "Lấy danh sách bài kiểm tra ĐÃ XUẤT BẢN/KẾT THÚC (Dành cho Sinh viên)")
    public ApiResponse<List<ExamResponseDto>> getActiveExamsForStudent(@PathVariable Long classId) {

        return ApiResponse.<List<ExamResponseDto>>builder()
                .code(200)
                .message("Tải danh sách bài kiểm tra của sinh viên thành công!")
                .data(examService.getActiveExamsForStudent(classId))
                .build();
    }

    @PostMapping("/classes/{classId}/exams")
    @PreAuthorize("hasAuthority('EXAM_CREATE')") // Quyền số 39
    @Operation(summary = "Tạo bài kiểm tra mới kèm danh sách câu hỏi và đáp án")
    public ApiResponse<String> createExam(
            @PathVariable Long classId,
            @Valid @RequestBody CreateExamRequestDto dto) {

        examService.createExamWithQuestions(classId, dto);

        return ApiResponse.<String>builder()
                .code(201)
                .message("Tạo bài kiểm tra thành công!")
                .data("EXAM_CREATED")
                .build();
    }
}