package com.example.LMS.service;

import com.example.LMS.dto.request.CreateUserRequest;
import com.example.LMS.dto.request.UserListRequest;
import com.example.LMS.dto.response.UserResponse;
import com.example.LMS.entity.model.Role;
import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.UserProfile;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.RoleRepository;
import com.example.LMS.repository.UserProfileRepository;
import com.example.LMS.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.LMS.entity.model.TeacherProfile;
import com.example.LMS.repository.TeacherProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.HashSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ============================================================
    // DANH SÁCH NGƯỜI DÙNG
    // ============================================================
    public Page<UserResponse> getUsers(UserListRequest request) {

        // Tính danh sách id cần ẩn theo người đang login
        User currentUser = getCurrentUser();
        request.setExcludeUserIds(getExcludedUserIds(currentUser));

        // 1. Tạo Pageable (phân trang + sắp xếp)
        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 2. Xây dựng Specification (dynamic filter)
        Specification<User> spec = buildSpecification(request);

        // 3. Query users (phân trang)
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // 4. Lấy tất cả profile của trang hiện tại trong 1 query (tránh N+1)
        List<Long> userIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getId(),
                        p -> p
                ));
        // Lấy teacher profiles của trang hiện tại trong 1 query (tránh N+1)
        Map<Long, TeacherProfile> teacherProfileMap = teacherProfileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        tp -> tp.getUser().getId(),
                        tp -> tp
                ));
        // 5. Map sang DTO, kết hợp user + profile
        return userPage.map(user -> UserResponse.fromEntity(
                user,
                profileMap.get(user.getId()),
                teacherProfileMap.get(user.getId())
        ));
    }
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng hiện tại"));
    }

    private List<Long> getExcludedUserIds(User currentUser) {
        Set<String> myRoles = currentUser.getRoles().stream()
                .map(Role::getCode).collect(Collectors.toSet());

        Set<String> rolesToHide = new HashSet<>();

        if (myRoles.contains("PRINCIPAL")) {
            rolesToHide.add("ADMIN");
        }
        if (myRoles.contains("HR")) {
            rolesToHide.add("ADMIN");
            rolesToHide.add("PRINCIPAL");
        }
        if (myRoles.contains("TRAINING_DEPT")) {
            rolesToHide.add("ADMIN");
            rolesToHide.add("PRINCIPAL");
            rolesToHide.add("HR");
        }
        if (myRoles.contains("HEAD_OF_DEPT")) {
            rolesToHide.addAll(List.of("ADMIN", "PRINCIPAL", "HR", "TRAINING_DEPT"));
        }
        // ADMIN không cần ẩn role nào, chỉ ẩn bản thân

        List<Long> excludedIds = new ArrayList<>();
        excludedIds.add(currentUser.getId()); // luôn ẩn bản thân

        if (!rolesToHide.isEmpty()) {
            excludedIds.addAll(userRepository.findUserIdsByRoleCodes(new ArrayList<>(rolesToHide)));
        }

        return excludedIds;
    }

    // ============================================================
    // XEM CHI TIẾT NGƯỜI DÙNG
    // ============================================================
    public UserResponse getUserById(Long id) {

        // 1. Tìm user theo id, chưa bị xóa mềm
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng với id: " + id
                ));

        // 2. Lấy profile (có thể null nếu chưa có)
        UserProfile profile = userProfileRepository.findByUserId(id).orElse(null);

        // 3. Map sang DTO
        return UserResponse.fromEntity(user, profile, user.getTeacherProfile());
    }

    // Danh sách trưởng khoa (cho dropdown tạo lớp học phần)
    public List<UserResponse> getHeadOfDepts() {
        return userRepository.findAllByRoleCode("HEAD_OF_DEPT")
                .stream()
                .map(u -> UserResponse.fromEntity(u, u.getProfile(), u.getTeacherProfile()))
                .collect(Collectors.toList());
    }

    // Danh sách giảng viên (cho dropdown tạo lớp học phần)
    public List<UserResponse> getInstructors() {
        return userRepository.findAllByRoleCode("INSTRUCTOR")
                .stream()
                .map(u -> UserResponse.fromEntity(u, u.getProfile(), u.getTeacherProfile()))
                .collect(Collectors.toList());
    }
    // ============================================================
    // SPECIFICATION (dynamic filter cho danh sách)
    // ============================================================
    private Specification<User> buildSpecification(UserListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Filter theo keyword: username, email, full_name
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Join<Object, Object> profileJoin = root.join("profile", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(profileJoin.get("fullName")), pattern)
                ));
            }

            // Filter theo isActive
            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            // Filter theo roleCode
            if (request.getRoleCode() != null && !request.getRoleCode().isBlank()) {
                Join<Object, Object> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(rolesJoin.get("code"), request.getRoleCode().toUpperCase()));
            }

            // Filter theo majorId (JOIN: users -> student_profiles -> majors)
            // Chỉ có ý nghĩa với STUDENT vì chỉ student_profiles mới có major_id
            if (request.getMajorId() != null) {
                Join<Object, Object> studentProfileJoin = root.join("studentProfile", JoinType.INNER);
                predicates.add(cb.equal(studentProfileJoin.get("major").get("id"), request.getMajorId()));
            }

            // Filter theo departmentId — 2 nhánh JOIN khác nhau tùy role:
//   STUDENT:    users -> student_profiles -> majors -> departments
//   INSTRUCTOR: users -> teacher_profiles -> departments
            if (request.getDepartmentId() != null) {
                // Nhánh STUDENT
                var studentProfileJoin = root.join("studentProfile", JoinType.LEFT);
                var majorJoin = studentProfileJoin.join("major", JoinType.LEFT);
                var studentDeptPredicate = cb.equal(
                        majorJoin.get("department").get("id"),
                        request.getDepartmentId()
                );

                // Nhánh INSTRUCTOR
                var teacherProfileJoin = root.join("teacherProfile", JoinType.LEFT);
                var teacherDeptPredicate = cb.equal(
                        teacherProfileJoin.get("department").get("id"),
                        request.getDepartmentId()
                );

                // Thỏa một trong hai nhánh là match
                predicates.add(cb.or(studentDeptPredicate, teacherDeptPredicate));
            }

            // Ẩn bản thân + các role cấp trên tùy theo người đang login
            if (request.getExcludeUserIds() != null && !request.getExcludeUserIds().isEmpty()) {
                predicates.add(cb.not(root.get("id").in(request.getExcludeUserIds())));
            }

            // Chỉ lấy user chưa bị xóa mềm
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
    @Transactional
    public void createUserWithRoles(CreateUserRequest request) {
        log.info("⏳ Đang tiến hành tạo tài khoản người dùng mới: {}", request.getUsername());

        // 1. Kiểm tra trùng lặp trùng tên đăng nhập hoặc email trong hệ thống
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Tên tài khoản này đã tồn tại trên hệ thống!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email này đã được đăng ký bởi tài khoản khác!");
        }

        // 2. Lấy danh sách các Role thực tế từ DB dựa trên list ID gửi lên
        List<Role> checkRoles = roleRepository.findAllById(request.getRoleIds());
        if (checkRoles.size() != request.getRoleIds().size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Có chứa ID vai trò không tồn tại trong hệ thống!");
        }
        // 3. 🔥 HÀNG RÀO PHÂN CẤP BẰNG ROLE CODE BỌC TRONG TRY-CATCH
        try {
            // Lấy thông tin tài khoản đang thực hiện request bấm nút trên giao diện
            String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người thực hiện!"));

            // Trích xuất toàn bộ mã code vai trò của người thực hiện (Ví dụ: ["HR"], ["ACADEMIC_DEPT"])
            java.util.Set<String> currentUserRoleCodes = currentUser.getRoles().stream()
                    .map(Role::getCode)
                    .collect(java.util.stream.Collectors.toSet());

            // ⛔ Duyệt qua từng vai trò định gán cho user mới để kiểm tra hành vi leo quyền
            for (Role targetRole : checkRoles) {
                String targetCode = targetRole.getCode();

                // TRƯỜNG HỢP 1: Người thực hiện là nhân sự phòng HR
                if (currentUserRoleCodes.contains("HR")) {
                    // HR tuyệt đối không được phép tạo hoặc gán vai trò ADMIN hoặc RECTOR (Hiệu trưởng)
                    if (targetCode.equals("ADMIN") || targetCode.equals("RECTOR")) {
                        throw new IllegalArgumentException("Nhân sự phòng HR không được phép gán vai trò cấp cao: " + targetRole.getName());
                    }
                }

                // TRƯỜNG HỢP 2: Người thực hiện là Phòng Đào tạo (ACADEMIC_DEPT)
                if (currentUserRoleCodes.contains("ACADEMIC_DEPT")) {
                    // Phòng đào tạo không được phép gán vai trò cho cả 3 bên trên (ADMIN, RECTOR, HR)
                    if (targetCode.equals("ADMIN") || targetCode.equals("RECTOR") || targetCode.equals("HR")) {
                        throw new IllegalArgumentException("Nhân sự Phòng Đào tạo không được phép gán vai trò cấp trên: " + targetRole.getName());
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            // Bắt trọn vẹn lỗi vi phạm quy tắc phân cấp bằng code chuỗi và chuyển đổi thành lỗi 403 Forbidden
            log.warn("🚨 CẢNH BÁO VI PHẠM PHÂN CẤP: {}", e.getMessage());
            throw new CustomException(HttpStatus.FORBIDDEN, e.getMessage());
        }


        // 3. Khởi tạo thực thể User và mã hóa mật khẩu an toàn
        String inputPassword = request.getPassword();
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(inputPassword) )// Mã hóa BCrypt!
                .email(request.getEmail())
                .isActive(true) // Mặc định tài khoản mới sẽ được kích hoạt luôn
                .roles(new HashSet<>(checkRoles)) // Gán danh sách vai trò vào bảng trung gian user_roles
                .build();

        // 4. Lưu User vào Database trước để sinh ra được userId
        User savedUser = userRepository.save(newUser);

        // 5. Đồng bộ khởi tạo luôn bản ghi bên bảng user_profiles
        // Việc này giúp luồng đăng nhập và xem thông tin sau này luôn có dữ liệu sạch
        UserProfile newProfile = UserProfile.builder()
                .user(savedUser) // Link khóa ngoại 1-1 sang bảng users
                .fullName(request.getFullName())
                .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + savedUser.getUsername()) // Tạo avatar mặc định ngẫu nhiên cho đẹp
                .build();

        userProfileRepository.save(newProfile);
        emailService.sendPasswordEmail(savedUser.getEmail(), request.getFullName(), savedUser.getUsername(),inputPassword);

        log.info("✅ Đã tạo tài khoản và kích hoạt lệnh gửi mail ngầm thành công.");
    }

}