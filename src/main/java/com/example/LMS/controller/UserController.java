package com.example.LMS.controller;

import com.example.LMS.dto.request.CreateUserRequest;
import com.example.LMS.dto.request.UserListRequest;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.UserResponse;
import com.example.LMS.entity.model.User;
import com.example.LMS.service.AuthService;
import com.example.LMS.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final AuthService authService;

    // ============================================================
    // GET /api/v1/users
    // Danh sách người dùng (filter + phân trang)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRINCIPAL', 'HR', 'TRAINING_DEPT', 'HEAD_OF_DEPT')")
    @Operation(
            summary = "Danh sách người dùng",
            description = """
                    Filter theo:
                    - keyword: tìm theo username / email / full_name
                    - isActive: true / false
                    - roleCode: ADMIN | STUDENT | INSTRUCTOR | ...
                    - majorId: id ngành học (chỉ có ý nghĩa với STUDENT)
                    - departmentId: id khoa (STUDENT qua major, INSTRUCTOR qua teacher_profile)
                    """
    )
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        UserListRequest request = new UserListRequest();
        request.setKeyword(keyword);
        request.setIsActive(isActive);
        request.setRoleCode(roleCode);
        request.setMajorId(majorId);
        request.setDepartmentId(departmentId);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PRINCIPAL', 'HR', 'TRAINING_DEPT', 'HEAD_OF_DEPT')")
    @Operation(
            summary = "Chi tiết người dùng",
            description = "Lấy đầy đủ thông tin 1 người dùng theo id, bao gồm cả thông tin profile."
    )
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/create-with-roles")
    @PreAuthorize("hasAuthority('USER_CREATE')") // 🛡️ Chỉ ADMIN tối cao mới có đặc quyền tự tạo tài khoản gán quyền kiểu này
    @Operation(summary = "Tạo tài khoản người dùng mới và gán Vai trò", description = "Tự động tạo kèm hồ sơ Profile trống. Chỉ ADMIN mới gọi được")
    public ApiResponse<String> createUser(@Valid @RequestBody CreateUserRequest request) {

        // Gọi Service xử lý liên kết dữ liệu đa bảng
        userService.createUserWithRoles(request);

        return ApiResponse.<String>builder()
                .code(201) // Mã 201 Created chuẩn thiết kế RESTful
                .message("Tạo tài khoản và gán vai trò thành công!")
                .data("Created Successfully")
                .build();
    }
}