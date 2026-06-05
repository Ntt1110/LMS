package com.example.LMS.controller;

import com.example.LMS.dto.request.SemesterCreateRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
@Tag(name = "Semester Management", description = "Quản lý học kỳ")
@SecurityRequirement(name = "bearerAuth")
public class SemesterController {

    private final SemesterService semesterService;

    // ============================================================
    // GET /api/v1/semesters
    // Danh sách học kỳ dùng cho dropdown (id, semesterCode, academicYear)
    // ============================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Danh sách học kỳ (dropdown)",
            description = "Trả về danh sách tất cả học kỳ gồm id, semesterCode, academicYear. Dùng cho dropdown."
    )
    public ResponseEntity<List<SemesterResponse>> getSemesters() {
        return ResponseEntity.ok(semesterService.getSemesters());
    }

    // ============================================================
    // GET /api/v1/semesters/{id}
    // Chi tiết học kỳ
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết học kỳ")
    public ResponseEntity<SemesterResponse> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getSemesterById(id));
    }

    // ============================================================
    // POST /api/v1/semesters
    // Tạo học kỳ mới
    // ============================================================
    @PostMapping
    @PreAuthorize("hasAuthority('SEMESTER_CREATE')")
    @Operation(summary = "Tạo học kỳ mới")
    public ResponseEntity<SemesterResponse> createSemester(@Valid @RequestBody SemesterCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(semesterService.createSemester(request));
    }
}