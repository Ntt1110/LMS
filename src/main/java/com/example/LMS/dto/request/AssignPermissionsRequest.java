package com.example.LMS.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignPermissionsRequest {
    @NotNull(message = "RoleId không được để trống")
    private Long roleId;

    @NotEmpty(message = "Danh sách Permission IDs không được để trống")
    private Set<Long> permissionIds; // Dùng Set để tự động loại bỏ nếu Frontend vô tình gửi trùng ID
}

