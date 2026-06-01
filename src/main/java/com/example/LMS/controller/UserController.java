package com.example.LMS.controller;

import com.example.LMS.dto.request.UserListRequest;
import com.example.LMS.dto.response.UserResponse;
import com.example.LMS.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Quản lý người dùng")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ============================================================
    // GET /api/v1/users
    // Danh sách người dùng (filter + phân trang)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(
            summary = "Danh sách người dùng",
            description = "Filter theo keyword (username/email/full_name), isActive, roleCode. Hỗ trợ phân trang và sắp xếp."
    )
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        UserListRequest request = new UserListRequest();
        request.setKeyword(keyword);
        request.setIsActive(isActive);
        request.setRoleCode(roleCode);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(userService.getUsers(request));
    }

    // ============================================================
    // GET /api/v1/users/{id}
    // Xem chi tiết 1 người dùng
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(
            summary = "Chi tiết người dùng",
            description = "Lấy đầy đủ thông tin 1 người dùng theo id, bao gồm cả thông tin profile."
    )
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}