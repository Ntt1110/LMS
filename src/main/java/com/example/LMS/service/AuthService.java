package com.example.LMS.service;

import com.example.LMS.dto.request.LoginRequest;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.entity.model.Permission;
import com.example.LMS.entity.model.Role;
import com.example.LMS.entity.model.User;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.UserRepository;
import com.example.LMS.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    // TODO: Tiêm thêm UserProfileRepository và PermissionRepository vào đây ở các bước sau

    public AuthResponse login(LoginRequest request) {
        log.info("⏳ Bắt đầu xử lý đăng nhập cho user: {}", request.getUsername());

        // 1. Xác thực Username & Password qua Spring Security
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.error("❌ Đăng nhập thất bại: Sai tài khoản hoặc mật khẩu - {}", request.getUsername());
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác!");
        }

        // 2. Lấy thông tin User hiện tại từ Database
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng!"));

        // Kiểm tra thêm trạng thái tài khoản (nếu cần)
        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin!");
        }

        // 3. Sinh JWT Token thực tế (Truyền thẳng user vào vì user implements UserDetails)
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Trích xuất danh sách Roles từ Set<Role> sang List<String>
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .toList();

        // 5. Lấy thông tin UserProfile
        // (Hiện tại đang mock dữ liệu. Sau khi ông tạo UserProfileRepository thì gọi: profileRepo.findByUserId(user.getId()))
        String fullName = "Thành viên " + user.getUsername();
        String avatarUrl = "";

        // 6. Lấy danh sách Permissions
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .toList();

        // 7. Đóng gói dữ liệu UserInfo
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .roles(roleCodes)
                .build();

        log.info("✅ Đăng nhập thành công: {}", request.getUsername());

        // 8. Trả về LoginResponse hoàn chỉnh
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .permissions(permissions)
                .build();
    }
}
