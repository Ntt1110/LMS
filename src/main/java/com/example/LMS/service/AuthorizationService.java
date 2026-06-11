package com.example.LMS.service;

import com.example.LMS.dto.request.AssignPermissionsRequest;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.dto.response.PermissionResponse;
import com.example.LMS.dto.response.RoleResponse;
import com.example.LMS.entity.model.Permission;
import com.example.LMS.entity.model.Role;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.PermissionRepository;
import com.example.LMS.repository.RoleRepository;
import com.example.LMS.repository.UserRepository;
import com.example.LMS.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    // =========================================================================
    // 1. LẤY TẤT CẢ VAI TRÒ (ROLES) KÈM THEO DANH SÁCH MÃ QUYỀN CỦA NÓ
    // =========================================================================
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        log.info("⏳ Đang lấy toàn bộ danh sách vai trò (Roles) từ hệ thống...");

        return roleRepository.findAll().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .code(role.getCode())
                        .name(role.getName())
                        .description(role.getDescription())
                        // Map danh sách Permission Object sang mảng String Code cho nhẹ JSON
                        .permissionCodes(role.getPermissions().stream()
                                .map(Permission::getCode)
                                .collect(Collectors.toSet()))
                        .build())
                .toList();
    }

    // =========================================================================
    // 2. LẤY TẤT CẢ QUYỀN HẠN CHI TIẾT (PERMISSIONS)
    // =========================================================================
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        log.info("⏳ Đang lấy toàn bộ danh sách quyền hạn (Permissions) từ hệ thống...");

        return permissionRepository.findAll().stream()
                .map(permission -> PermissionResponse.builder()
                        .id(permission.getId())
                        .code(permission.getCode())
                        .name(permission.getName())
                        .module(permission.getModule())
                        .description(permission.getDescription())
                        .build())
                .toList();
    }

    @Transactional
    public void assignPermissionsToRole(AssignPermissionsRequest request) {
        log.info("⏳ Đang tiến hành cập nhật quyền hạn cho Role ID: {}", request.getRoleId());

        // 1. Tìm Role cần gán quyền trong DB, nếu không thấy lập tức biệt phái lỗi 404
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò (Role) yêu cầu!"));

        // 2. Truy vấn toàn bộ các Permission Object thực tế từ DB dựa theo mảng ID gửi lên
        List<Permission> checkPermissions = permissionRepository.findAllById(request.getPermissionIds());

        // Kiểm tra xem số lượng ID gửi lên có khớp với số lượng tìm thấy trong DB không (tránh ID ma do Frontend gửi bậy)
        if (checkPermissions.size() != request.getPermissionIds().size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Có chứa ID quyền hạn không tồn tại trong hệ thống!");
        }

        // 3. Đè danh sách quyền mới vào Role
        // Hibernate sẽ tự động lo liệu việc xóa các bản ghi cũ và insert các bản ghi mới vào bảng trung gian role_permissions
        role.setPermissions(new HashSet<>(checkPermissions));

        // 4. Lưu lại vào Database
        roleRepository.save(role);
        log.info("✅ Cập nhật thành công! Role [{}] hiện tại đang sở hữu {} quyền.", role.getCode(), checkPermissions.size());
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        // 1. Kiểm tra xem Refresh Token gửi lên có trống không
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Refresh Token không được để trống!");
        }

        try {
            // 2. Giải mã và trích xuất thông tin từ Refresh Token cũ
            String username = jwtService.extractUsername(refreshToken);

            // 3. Tìm kiếm User trong hệ thống để đảm bảo tài khoản không bị khóa hoặc xóa
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại hoặc không hợp lệ!"));

            // 4. Kiểm tra xem Refresh Token có thực sự hợp lệ và còn hạn không
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "Refresh Token đã hết hạn hoặc không hợp lệ! Vui lòng đăng nhập lại.");
            }

            // 5. Nếu mọi thứ xanh mượt -> Tiến hành sinh cặp Token mới tinh
            Claims claims = jwtService.extractAllClaimsPublic(refreshToken);
            String newAccessToken = jwtService.generateTokenFromClaims(claims, username);
            String newRefreshToken = jwtService.generateRefreshToken(user); // Sinh thêm cả Refresh mới để xoay vòng bảo mật (Token Rotation)

            log.info("🔄 [JWT REFRESH] Tái cấp Access Token thành công cho tài khoản: {}", username);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            log.error("🚨 [JWT REFRESH ERROR] Lỗi giải mã Refresh Token: {}", e.getMessage());
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ! Vui lòng thực hiện đăng nhập lại.");
        }
    }
}