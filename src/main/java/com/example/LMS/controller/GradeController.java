package com.example.LMS.controller;

import com.example.LMS.dto.request.FinalizeGradeRequest;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.ClassGradeListResponseDto;
import com.example.LMS.dto.response.GradeResponseDto;
import com.example.LMS.dto.response.TranscriptResponseDto;
import com.example.LMS.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Grade", description = "Bảng điểm")
@SecurityRequirement(name = "bearerAuth")
public class GradeController {

    private final GradeService gradeService;

    // ============================================================
    // XEM BẢNG ĐIỂM CỦA LỚP (Sinh viên)
    // GET /api/v1/classes/{classId}/my-grade
    // Quyền: GRADE_VIEW_SUBJECT (id = 45)
    // ============================================================
    @GetMapping("/api/v1/classes/{classId}/my-grade")
    @PreAuthorize("hasAuthority('GRADE_VIEW_SUBJECT')")
    @Operation(summary = "Xem bảng điểm của lớp học phần (Dành cho sinh viên)")
    public ApiResponse<GradeResponseDto> getMyGrade(@PathVariable Long classId) {
        return ApiResponse.<GradeResponseDto>builder()
                .code(200)
                .message("Tải bảng điểm thành công!")
                .data(gradeService.getMyGrade(classId))
                .build();
    }

    // ============================================================
    // XEM DANH SÁCH BẢNG ĐIỂM SINH VIÊN CỦA LỚP (Giảng viên)
    // GET /api/v1/classes/{classId}/grades
    // Quyền: GRADE_LOCK (id = 52)
    // ============================================================
    @GetMapping("/api/v1/classes/{classId}/grades")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Xem danh sách bảng điểm sinh viên của lớp (Dành cho giảng viên)")
    public ApiResponse<ClassGradeListResponseDto> getClassGrades(@PathVariable Long classId) {
        return ApiResponse.<ClassGradeListResponseDto>builder()
                .code(200)
                .message("Tải bảng điểm lớp học phần thành công!")
                .data(gradeService.getClassGrades(classId))
                .build();
    }
    // ============================================================
    // CHỐT ĐIỂM (Giảng viên)
    // POST /api/v1/classes/{classId}/grades/finalize
    // Nhập đủ 4 cột → tính totalScore + status → lưu DB
    // Chỉ GV được phân công mới được chốt
    // ============================================================
    @PostMapping("/api/v1/classes/{classId}/grades/finalize")
    @PreAuthorize("hasAuthority('GRADE_LOCK')")
    @Operation(
            summary = "Chốt điểm sinh viên (Giảng viên)",
            description = "Nhập đủ 4 cột điểm (TX1, TX2, GK, CK) cho 1 sinh viên. " +
                    "Hệ thống tự tính totalScore và status (PASS/FAIL). " +
                    "Chỉ giảng viên được phân công dạy lớp mới có quyền thực hiện."
    )
    public ApiResponse<ClassGradeListResponseDto.StudentGradeDto> finalizeGrade(
            @PathVariable Long classId,
            @Valid @RequestBody FinalizeGradeRequest request
    ) {
        return ApiResponse.<ClassGradeListResponseDto.StudentGradeDto>builder()
                .code(200)
                .message("Chốt điểm thành công!")
                .data(gradeService.finalizeGrade(classId, request))
                .build();
    }
    // ============================================================
    // XEM BẢNG ĐIỂM TOÀN KHOÁ (Sinh viên)
    // GET /api/v1/grades/my-transcript
    // Trả về toàn bộ học kỳ đã học, từng học kỳ có danh sách môn + điểm.
    // Quyền: GRADE_VIEW_SUBJECT (id = 45)
    // ============================================================
    @GetMapping("/api/v1/grades/my-transcript")
    @PreAuthorize("hasAuthority('GRADE_VIEW_SUBJECT')")
    @Operation(summary = "Xem bảng điểm toàn khoá của sinh viên (gom theo từng học kỳ)")
    public ApiResponse<TranscriptResponseDto> getMyTranscript() {
        return ApiResponse.<TranscriptResponseDto>builder()
                .code(200)
                .message("Tải bảng điểm toàn khoá thành công!")
                .data(gradeService.getMyTranscript())
                .build();
    }
}