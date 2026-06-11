package com.example.LMS.controller;

import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.EnrollmentResponse;
import com.example.LMS.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "Đăng ký học phần")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{classId}")
    @PreAuthorize("hasAuthority('ENROLLMENT_REGISTER')")
    @Operation(summary = "Đăng ký học phần")
    public ApiResponse<EnrollmentResponse> register(@PathVariable Long classId) {
        return ApiResponse.<EnrollmentResponse>builder()
                .code(201)
                .message("Đăng ký học phần thành công!")
                .data(enrollmentService.register(classId))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    @Operation(summary = "Danh sách lớp đang học và đã hoàn thành của sinh viên")
    public ApiResponse<List<EnrollmentResponse>> getMyEnrollments() {
        return ApiResponse.<List<EnrollmentResponse>>builder()
                .code(200)
                .message("Tải danh sách học phần thành công!")
                .data(enrollmentService.getMyEnrollments())
                .build();
    }

    @GetMapping("/my/registered")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    @Operation(
            summary = "Danh sách lớp đã đăng ký theo trạng thái",
            description = "Lọc theo trạng thái lớp: ONGOING (đang diễn ra) hoặc COMPLETED (đã kết thúc)"
    )
    public ApiResponse<List<EnrollmentResponse>> getMyEnrollmentsByStatus(
            @RequestParam String status
    ) {
        return ApiResponse.<List<EnrollmentResponse>>builder()
                .code(200)
                .message("Tải danh sách học phần thành công!")
                .data(enrollmentService.getMyEnrollmentsByStatus(status))
                .build();
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAuthority('ENROLLMENT_CANCEL')")
    @Operation(summary = "Hủy đăng ký học phần")
    public ApiResponse<String> cancel(@PathVariable Long classId) {
        enrollmentService.cancel(classId);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Hủy đăng ký học phần thành công!")
                .data("Cancelled")
                .build();
    }
}