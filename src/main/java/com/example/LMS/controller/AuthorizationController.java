package com.example.LMS.controller;

import com.example.LMS.dto.request.AssignPermissionsRequest;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.PermissionResponse;
import com.example.LMS.dto.response.RoleResponse;
import com.example.LMS.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authorizations")
@RequiredArgsConstructor
// 🔥 KHAI BÁO NHÓM SWAGGER TẠI ĐÂY
@Tag(name = "Phân Quyền Hệ Thống", description = "Các API quản lý, cấu hình, và gán vai trò/quyền hạn (RBAC)")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    // Thêm mô tả ngắn cho từng API nếu muốn xịn hơn
    @Operation(summary = "Lấy danh sách tất cả các Vai trò (Roles)", description = " quyền truy cập")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .code(200)
                .message("Lấy danh sách vai trò hệ thống thành công!")
                .data(authorizationService.getAllRoles())
                .build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'ACADEMIC_DEPT')")
    @Operation(summary = "Lấy danh sách tất cả các Quyền hạn chi tiết (Permissions)", description = "Chỉ tài khoản có quyền truy cập")
    public ApiResponse<List<PermissionResponse>> getPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .code(200)
                .message("Lấy danh sách quyền hạn chi tiết thành công!")
                .data(authorizationService.getAllPermissions())
                .build();
    }

    @PutMapping("/roles/assign-permissions")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    @Operation(summary = "Gán danh sách các quyền hạn chi tiết cho một Vai trò", description = "Chỉ tài khoản ADMIN mới có quyền thực thi")
    public ApiResponse<String> assignPermissions(@Valid @RequestBody AssignPermissionsRequest request) {

        // Chạy qua tầng nghiệp vụ để ghi đè dữ liệu
        authorizationService.assignPermissionsToRole(request);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Cập nhật và gán quyền hạn cho vai trò thành công!")
                .data("Cấu hình thành công")
                .build();
    }
}