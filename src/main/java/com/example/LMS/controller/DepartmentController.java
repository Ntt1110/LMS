package com.example.LMS.controller;

import com.example.LMS.dto.response.DepartmentResponse;
import com.example.LMS.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "Quản lý khoa")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    // ============================================================
    // GET /api/v1/departments
    // Danh sách khoa (dùng cho dropdown)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','TRAINING_DEPT','HR')")
    @Operation(
            summary = "Danh sách khoa",
            description = "Lấy danh sách tất cả khoa dùng cho dropdown."
    )
    public ResponseEntity<List<DepartmentResponse>> getDepartments() {
        return ResponseEntity.ok(departmentService.getDepartments());
    }

    // ============================================================
    // GET /api/v1/departments/{id}
    // Chi tiết khoa
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAJOR_VIEW')")
    @Operation(
            summary = "Chi tiết khoa",
            description = "Lấy thông tin chi tiết 1 khoa theo id, bao gồm thông tin trưởng khoa."
    )
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }
}