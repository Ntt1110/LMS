package com.example.LMS.controller;

import com.example.LMS.dto.request.MajorListRequest;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.dto.response.MajorResponse;
import com.example.LMS.service.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/majors")
@RequiredArgsConstructor
@Tag(name = "Major Management", description = "Quản lý ngành học")
@SecurityRequirement(name = "bearerAuth")
public class MajorController {

    private final MajorService majorService;

    // ============================================================
    // GET /api/v1/majors/dropdown
    // Danh sách ngành học dùng cho dropdown (chỉ trả id + name)
    // Có thể lọc theo departmentId để chỉ lấy ngành của 1 khoa cụ thể
    // ============================================================
    @GetMapping("/dropdown")
    @PreAuthorize("hasAuthority('MAJOR_VIEW')")
    @Operation(
            summary = "Dropdown ngành học",
            description = "Trả về danh sách {id, name} dùng cho select/dropdown. " +
                    "Truyền departmentId để lọc theo khoa (chỉ lấy ngành đang active)."
    )
    public ResponseEntity<List<DropdownResponseDto>> getMajorsDropdown(
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(majorService.getMajorsDropdown(departmentId));
    }

    // ============================================================
    // GET /api/v1/majors
    // Danh sách ngành học (filter + phân trang)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAuthority('MAJOR_VIEW')")
    @Operation(
            summary = "Danh sách ngành học",
            description = "Filter theo keyword (code/name), isActive, departmentId. Hỗ trợ phân trang và sắp xếp."
    )
    public ResponseEntity<Page<MajorResponse>> getMajors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        MajorListRequest request = new MajorListRequest();
        request.setKeyword(keyword);
        request.setIsActive(isActive);
        request.setDepartmentId(departmentId);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(majorService.getMajors(request));
    }

    // ============================================================
    // GET /api/v1/majors/{id}
    // Xem chi tiết ngành học
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAJOR_VIEW')")
    @Operation(
            summary = "Chi tiết ngành học",
            description = "Lấy đầy đủ thông tin 1 ngành học theo id, bao gồm thông tin khoa."
    )
    public ResponseEntity<MajorResponse> getMajorById(@PathVariable Long id) {
        return ResponseEntity.ok(majorService.getMajorById(id));
    }
}