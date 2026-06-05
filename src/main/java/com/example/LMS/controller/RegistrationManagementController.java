package com.example.LMS.controller;

import com.example.LMS.dto.request.RegistrationPeriodListRequest;
import com.example.LMS.dto.response.ClassPendingResponse;
import com.example.LMS.dto.response.RegistrationPeriodResponse;
import com.example.LMS.service.RegistrationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.example.LMS.dto.request.RegistrationPeriodRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registration-management")
@RequiredArgsConstructor
@Tag(name = "Registration Management", description = "Quản lý đợt đăng ký học phần")
@SecurityRequirement(name = "bearerAuth")
public class RegistrationManagementController {

    private final RegistrationManagementService registrationManagementService;

    // ============================================================
    // GET /api/v1/registration-management/periods
    // Danh sách đợt đăng ký
    // ============================================================
    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('CLASS_OPEN_REG')")
    @Operation(summary = "Danh sách đợt đăng ký học phần",
            description = "Tìm kiếm theo tên, lọc theo học kỳ, trạng thái. Có phân trang.")
    public ResponseEntity<Page<RegistrationPeriodResponse>> getRegistrationPeriods(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        RegistrationPeriodListRequest request = new RegistrationPeriodListRequest();
        request.setKeyword(keyword);
        request.setSemesterId(semesterId);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(registrationManagementService.getRegistrationPeriods(request));
    }

    // ============================================================
    // GET /api/v1/registration-management/pending-classes
    // Danh sách học phần PENDING theo học kỳ và khoa
    // ============================================================
    @GetMapping("/pending-classes")
    @PreAuthorize("hasAuthority('CLASS_OPEN_REG')")
    @Operation(summary = "Danh sách học phần PENDING theo học kỳ và khoa",
            description = "Dùng khi tạo đợt đăng ký — chọn học kỳ và khoa để xem các học phần chờ mở đăng ký.")
    public ResponseEntity<List<ClassPendingResponse>> getPendingClasses(
            @RequestParam Long semesterId,
            @RequestParam Long departmentId
    ) {
        return ResponseEntity.ok(
                registrationManagementService.getPendingClassesBySemesterAndDepartment(semesterId, departmentId)
        );
    }

    // ============================================================
// POST /api/v1/registration-management/periods
// Tạo đợt đăng ký mới
// ============================================================
    @PostMapping("/periods")
    @PreAuthorize("hasAuthority('CLASS_OPEN_REG')")
    @Operation(summary = "Tạo đợt đăng ký học phần mới",
            description = "Tạo đợt đăng ký, tự động tính trạng thái theo thời gian. Nếu ACTIVE thì kích hoạt các lớp PENDING.")
    public ResponseEntity<RegistrationPeriodResponse> createRegistrationPeriod(
            @Valid @RequestBody RegistrationPeriodRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationManagementService.createRegistrationPeriod(request));
    }
}