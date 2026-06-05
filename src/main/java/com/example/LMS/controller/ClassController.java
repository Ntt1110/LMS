package com.example.LMS.controller;

import com.example.LMS.dto.request.ClassListRequest;
import com.example.LMS.dto.response.ClassDetailResponse;
import com.example.LMS.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "Class Management", description = "Quản lý lớp học phần")
@SecurityRequirement(name = "bearerAuth")
public class ClassController {

    private final ClassService classService;

    // ============================================================
    // GET /api/v1/classes
    // Danh sách lớp học phần — Phòng đào tạo thấy hết, Trưởng khoa chỉ thấy lớp mình
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TRAINING_DEPT', 'ROLE_HEAD_OF_DEPT')")
    @Operation(
            summary = "Danh sách lớp học phần",
            description = "Tìm theo mã lớp/tên môn, lọc theo học kỳ, trạng thái, khoa. " +
                    "Phòng đào tạo thấy tất cả. Trưởng khoa chỉ thấy lớp mình quản lý."
    )
    public ResponseEntity<Page<ClassDetailResponse>> getClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        ClassListRequest request = new ClassListRequest();
        request.setKeyword(keyword);
        request.setSemesterId(semesterId);
        request.setDepartmentId(departmentId);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(classService.getClasses(request));
    }
}