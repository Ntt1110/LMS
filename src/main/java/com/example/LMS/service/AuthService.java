package com.example.LMS.service;

import com.example.LMS.dto.request.CreateUserRequest;
import com.example.LMS.dto.request.LoginRequest;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.dto.response.UserProfileResponse;
import com.example.LMS.entity.model.*;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import com.example.LMS.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserProfileRepository userProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
   ;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;

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
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        String fullName = (profile != null) ? profile.getFullName() : "Thành viên " + user.getUsername();
        String avatarUrl = (profile != null) ? profile.getAvatarUrl() : "";

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


    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        // 1. Lấy thông tin username của người dùng đang đăng nhập từ SecurityContext của Spring Security
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        log.info("⏳ Đang lấy hồ sơ cá nhân cho tài khoản đang đăng nhập: {}", currentUsername);

        // 2. Tìm User trong DB, nếu không có ném lỗi 404
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tài khoản!"));

        // 3. Tìm UserProfile tương ứng
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Hồ sơ người dùng không tồn tại!"));

        UserProfileResponse.UserProfileResponseBuilder responseBuilder = UserProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .birthday(profile.getBirthday() != null ? profile.getBirthday().toString() : null)
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .avatarUrl(profile.getAvatarUrl())
                .address(profile.getAddress());
        List < String > authorities = user.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList();

        if (authorities.contains("ROLE_STUDENT")) {
            responseBuilder.role("STUDENT");
            // Đi thẳng từ Object User sang StudentProfile nhờ quan hệ hai chiều đã cấu hình
            StudentProfile student = user.getStudentProfile();
            if (student != null) {
                responseBuilder.studentCode(student.getStudentCode());
                responseBuilder.cohort(student.getCohort());
                if (student.getMajor() != null) {
                    responseBuilder.majorName(student.getMajor().getName()); // Lấy tên ngành học
                }
            }
        } else if (authorities.contains("ROLE_INSTRUCTOR") || authorities.contains("ROLE_TEACHER")) {
            responseBuilder.role("TEACHER");
            // Đi thẳng từ Object User sang TeacherProfile
            TeacherProfile teacher = user.getTeacherProfile();
            if (teacher != null) {
                responseBuilder.employeeCode(teacher.getEmployeeCode());
                responseBuilder.academicTitle(teacher.getAcademicTitle());
                responseBuilder.specialization(teacher.getSpecialization());
                if (teacher.getDepartment() != null) {
                    responseBuilder.departmentName(teacher.getDepartment().getName()); // Lấy tên khoa/bộ môn
                }
            }
        } else {
            responseBuilder.role("USER");
        }

        return responseBuilder.build();
    }

    @Transactional
    public void resetPassword(com.example.LMS.dto.request.ResetPasswordDto dto) {
        log.info("⏳ Hệ thống đang kiểm tra Token để tiến hành đổi mật khẩu mới...");

        // 1. Kiểm tra Token có tồn tại trong bảng password_resets không
        PasswordReset passwordReset = passwordResetRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Mã xác thực (Token) không hợp lệ!"));

        // 2. Kiểm tra xem Token này đã từng được sử dụng trước đó chưa
        if (passwordReset.getIsUsed()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Mã xác thực này đã được sử dụng! Vui lòng gửi lại yêu cầu mới.");
        }

        // 3. Kiểm tra xem Token đã bị quá hạn 15 phút chưa
        if (passwordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn sử dụng! Vui lòng gửi lại yêu cầu mới.");
        }

        // 4. Tìm kiếm User sở hữu Token này để cập nhật mật khẩu
        User user = userRepository.findById(passwordReset.getUserId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Tài khoản liên kết không tồn tại trên hệ thống!"));

        // 5. Tiến hành mã hóa BCrypt mật khẩu mới và ghi đè vào bảng users
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // 6. Đóng hòm Token: Đánh dấu đã sử dụng để không ai dùng lại được nữa
        passwordReset.setIsUsed(true);
        passwordResetRepository.save(passwordReset);

        log.info("✅ Đổi mật khẩu thành công cho tài khoản có Username: {}", user.getUsername());
    }
}


