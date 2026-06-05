package com.example.LMS.controller;

import com.example.LMS.dto.request.SemesterCreateRequest;
import com.example.LMS.dto.request.SemesterListRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    // Danh sách học kỳ có filter, phân trang
    // ============================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Danh sách học kỳ",
            description = "Filter theo keyword, status (ACTIVE/CLOSED), academicYear. Hỗ trợ phân trang và sắp xếp."
    )
    public ResponseEntity<Page<SemesterResponse>> getSemesters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String academicYear,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        SemesterListRequest request = new SemesterListRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setAcademicYear(academicYear);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(semesterService.getSemesters(request));
    }

    // ============================================================
    // GET /api/v1/semesters/all
    // Danh sách học kỳ dùng cho dropdown (không phân trang)
    // ============================================================
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Danh sách học kỳ (dropdown)",
            description = "Trả về danh sách tất cả học kỳ gồm id, semesterCode, academicYear. Dùng cho dropdown."
    )
    public ResponseEntity<List<SemesterResponse>> getAllSemesters() {
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