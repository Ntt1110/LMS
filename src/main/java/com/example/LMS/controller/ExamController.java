package com.example.LMS.controller;


import com.example.LMS.dto.request.CreateExamRequestDto;
import com.example.LMS.dto.response.*;
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

    // =========================================================================
    // API 3: MO BAI KIEM TRA (DRAFT -> PUBLISHED)
    // =========================================================================
    @PutMapping("/classes/{classId}/exams/{examId}/open")
    @PreAuthorize("hasAuthority('EXAM_OPEN')")
    @Operation(summary = "Mo bai kiem tra - chuyen tu DRAFT sang PUBLISHED (sinh vien thay duoc)")
    public ApiResponse<ExamResponseDto> openExam(
            @PathVariable Long classId,
            @PathVariable Long examId) {

        return ApiResponse.<ExamResponseDto>builder()
                .code(200)
                .message("Mo bai kiem tra thanh cong!")
                .data(examService.openExam(classId, examId))
                .build();
    }

    // =========================================================================
    // API 4: DONG BAI KIEM TRA (PUBLISHED -> CLOSED)
    // =========================================================================
    @PutMapping("/classes/{classId}/exams/{examId}/close")
    @PreAuthorize("hasAuthority('EXAM_CLOSE')")
    @Operation(summary = "Dong bai kiem tra - chuyen tu PUBLISHED sang CLOSED (ket thuc lam bai)")
    public ApiResponse<ExamResponseDto> closeExam(
            @PathVariable Long classId,
            @PathVariable Long examId) {

        return ApiResponse.<ExamResponseDto>builder()
                .code(200)
                .message("Dong bai kiem tra thanh cong!")
                .data(examService.closeExam(classId, examId))
                .build();
    }

    // BẮT ĐẦU LÀM BÀI KIỂM TRA (Tạo phiên làm bài)

    @PostMapping("/exams/{examId}/attempts")
    @PreAuthorize("hasAuthority('EXAM_TAKE_SUBMIT')") // Chỉ sinh viên có quyền xem lớp mới được làm
    @Operation(summary = "Khởi tạo phiên làm bài kiểm tra (Bắt đầu tính giờ)")
    public ApiResponse<ExamAttemptResponseDto> startExam(@PathVariable Long examId) {

        return ApiResponse.<ExamAttemptResponseDto>builder()
                .code(201) // Mã 201 Created chuẩn RESTful
                .message("Bắt đầu làm bài thành công! Hãy chú ý thời gian.")
                .data(examService.startExamAttempt(examId))
                .build();
    }

    // TẢI ĐỀ THI CHO SINH VIÊN (BẢO MẬT ĐÁP ÁN)

    @GetMapping("/exams/{examId}/paper")
    @PreAuthorize("hasAuthority('EXAM_TAKE_SUBMIT')")
    @Operation(summary = "Tải đề thi để sinh viên làm bài (Che giấu đáp án đúng)")
    public ApiResponse<ExamPaperResponseDto> getExamPaper(@PathVariable Long examId) {

        return ApiResponse.<ExamPaperResponseDto>builder()
                .code(200)
                .message("Tải đề thi thành công! Chúc bạn làm bài tốt.")
                .data(examService.getExamPaperForStudent(examId))
                .build();
    }

    // =========================================================================
    //   NỘP BÀI CHỦ ĐỘNG (Sinh viên tự bấm nút nộp)
    // =========================================================================
    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasAuthority('EXAM_VIEW')")
    @Operation(summary = "Sinh viên chủ động nộp bài (Hệ thống sẽ check và quăng cảnh báo nếu còn câu trống)")
    public ApiResponse<ExamResultResponseDto> submitExam(
            @PathVariable Long attemptId,
            @RequestParam(defaultValue = "false") boolean acceptIncomplete) {

        return ApiResponse.<ExamResultResponseDto>builder()
                .code(200)
                .message("Nộp bài thành công!")
                .data(examService.submitExamAttempt(attemptId, acceptIncomplete)) // Luồng có check câu trống
                .build();
    }

    // =========================================================================
    //  NỘP BÀI TỰ ĐỘNG TỪ FRONTEND (Đồng hồ đếm ngược về 0)
    // =========================================================================
    @PostMapping("/attempts/{attemptId}/force-submit")
    @PreAuthorize("hasAuthority('EXAM_VIEW')")
    @Operation(summary = "Ép nộp bài khi hết giờ (Frontend gọi khi đồng hồ về 0, chấm điểm bất chấp câu trống)")
    public ApiResponse<ExamResultResponseDto> forceSubmitExam(@PathVariable Long attemptId) {

        return ApiResponse.<ExamResultResponseDto>builder()
                .code(200)
                .message("Hết giờ làm bài! Hệ thống đã tự động thu bài của bạn.")
                .data(examService.forceSubmitExamAttempt(attemptId)) // Luồng cưỡng chế, chuyển trạng thái thành FORCED
                .build();
    }
}