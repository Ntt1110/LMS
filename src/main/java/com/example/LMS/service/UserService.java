package com.example.LMS.service;

import com.example.LMS.dto.request.CreateUserRequest;
import com.example.LMS.dto.request.UserListRequest;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.dto.response.UserResponse;
import com.example.LMS.entity.model.*;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.HashSet;
import com.example.LMS.dto.request.UpdateUserRequest;
import com.example.LMS.entity.model.Department;
import com.example.LMS.entity.model.Major;
import com.example.LMS.entity.model.StudentProfile;
import com.example.LMS.entity.model.TeacherProfile;
import com.example.LMS.repository.DepartmentRepository;
import com.example.LMS.repository.MajorRepository;
import com.example.LMS.dto.request.LockUserRequest;
import com.example.LMS.dto.response.ApiResponse;

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
    private final StudentProfileRepository studentProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;

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

        // 3. Lấy teacher/student profile riêng (tránh LazyInitializationException)
        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(id).orElse(null);
        StudentProfile studentProfile = studentProfileRepository.findByUserId(id).orElse(null);

        // 4. Map sang DTO
        return UserResponse.fromEntity(user, profile, teacherProfile, studentProfile);
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
    // Dropdown danh sách vai trò (id + name)
    public List<DropdownResponseDto> getRolesDropdown() {
        log.info("⏳ Đang lấy dropdown danh sách vai trò...");

        // Lấy role của người đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        Set<String> myRoles = new HashSet<>();
        currentUser.getRoles().forEach(r -> myRoles.add(r.getCode()));

        // Xác định các role bị ẩn theo phân cấp
        // Tất cả      → ẩn ADMIN
        // HR          → ẩn PRINCIPAL
        // TRAINING_DEPT → ẩn PRINCIPAL, HR
        // HEAD_OF_DEPT  → ẩn PRINCIPAL, HR, TRAINING_DEPT
        Set<String> rolesToHide = new HashSet<>();

        // Tất cả đều ẩn ADMIN — kể cả chính ADMIN
        rolesToHide.add("ADMIN");

        if (myRoles.contains("HR")) {
            rolesToHide.add("PRINCIPAL");
        }
        if (myRoles.contains("TRAINING_DEPT")) {
            rolesToHide.add("PRINCIPAL");
            rolesToHide.add("HR");
        }
        if (myRoles.contains("HEAD_OF_DEPT")) {
            rolesToHide.add("PRINCIPAL");
            rolesToHide.add("HR");
            rolesToHide.add("TRAINING_DEPT");
        }

        return roleRepository.findAll().stream()
                .filter(role -> !rolesToHide.contains(role.getCode()))
                .map(role -> DropdownResponseDto.builder()
                        .id(role.getId())
                        .code(role.getCode())
                        .name(role.getName())
                        .build())
                .toList();
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
    // ============================================================
// CẬP NHẬT TÀI KHOẢN (USER_UPDATE)
// PUT /api/v1/users/{id}
// ============================================================
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        // 1. Tìm user
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng với id: " + id));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        // 2. Cập nhật user_profiles (CHUNG cho tất cả)
        UserProfile profile = userProfileRepository.findByUserId(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ người dùng!"));

        if (request.getPhone() != null)
            profile.setPhone(request.getPhone().trim());

        if (request.getBirthday() != null)
            profile.setBirthday(request.getBirthday());

        if (request.getGender() != null) {
            try {
                profile.setGender(UserProfile.Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "Giới tính không hợp lệ! Chỉ chấp nhận: MALE, FEMALE, OTHER");
            }
        }

        if (request.getAddress() != null)
            profile.setAddress(request.getAddress().trim());

        userProfileRepository.save(profile);

        // 3. Nếu là INSTRUCTOR hoặc HEAD_OF_DEPT → tạo mới hoặc cập nhật teacher_profiles
        if (roles.contains("INSTRUCTOR") || roles.contains("HEAD_OF_DEPT")) {

            TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(id).orElse(null);

            if (teacherProfile == null) {
                // Chưa có hồ sơ → tạo mới, yêu cầu bắt buộc employeeCode + departmentId
                if (request.getEmployeeCode() == null || request.getEmployeeCode().isBlank()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Hồ sơ giảng viên chưa tồn tại, cần cung cấp mã giảng viên (employeeCode) để tạo mới!");
                }
                if (request.getDepartmentId() == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Hồ sơ giảng viên chưa tồn tại, cần cung cấp khoa (departmentId) để tạo mới!");
                }
                Department department = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                                "Không tìm thấy khoa với id: " + request.getDepartmentId()));

                teacherProfile = TeacherProfile.builder()
                        .user(user)
                        .employeeCode(request.getEmployeeCode().trim())
                        .department(department)
                        .academicTitle(request.getAcademicTitle() != null ? request.getAcademicTitle().trim() : null)
                        .specialization(request.getSpecialization() != null ? request.getSpecialization().trim() : null)
                        .isVisiting(request.getIsVisiting() != null ? request.getIsVisiting() : false)
                        .build();
                log.info("🆕 Tạo mới teacher_profile cho user ID: {}", id);
            } else {
                // Đã có hồ sơ → cập nhật các trường được gửi lên
                if (request.getEmployeeCode() != null && !request.getEmployeeCode().isBlank())
                    teacherProfile.setEmployeeCode(request.getEmployeeCode().trim());

                if (request.getDepartmentId() != null) {
                    Department department = departmentRepository.findById(request.getDepartmentId())
                            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                                    "Không tìm thấy khoa với id: " + request.getDepartmentId()));
                    teacherProfile.setDepartment(department);
                }

                if (request.getAcademicTitle() != null)
                    teacherProfile.setAcademicTitle(request.getAcademicTitle().trim());

                if (request.getSpecialization() != null)
                    teacherProfile.setSpecialization(request.getSpecialization().trim());

                if (request.getIsVisiting() != null)
                    teacherProfile.setIsVisiting(request.getIsVisiting());
            }

            teacherProfileRepository.save(teacherProfile);
            log.info("✅ Đã lưu teacher_profile cho user ID: {}", id);
        }

        // 4. Nếu là STUDENT → tạo mới hoặc cập nhật student_profiles
        if (roles.contains("STUDENT")) {

            StudentProfile studentProfile = studentProfileRepository.findByUserId(id).orElse(null);

            if (studentProfile == null) {
                // Chưa có hồ sơ → tạo mới, yêu cầu bắt buộc studentCode + majorId
                if (request.getStudentCode() == null || request.getStudentCode().isBlank()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Hồ sơ sinh viên chưa tồn tại, cần cung cấp mã sinh viên (studentCode) để tạo mới!");
                }
                if (request.getMajorId() == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Hồ sơ sinh viên chưa tồn tại, cần cung cấp ngành học (majorId) để tạo mới!");
                }
                Major major = majorRepository.findByIdAndDeletedAtIsNull(request.getMajorId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                                "Không tìm thấy ngành học với id: " + request.getMajorId()));

                studentProfile = StudentProfile.builder()
                        .user(user)
                        .studentCode(request.getStudentCode().trim())
                        .major(major)
                        .cohort(request.getCohort())
                        .status(StudentProfile.Status.STUDYING)
                        .build();
                log.info("🆕 Tạo mới student_profile cho user ID: {}", id);
            } else {
                // Đã có hồ sơ → cập nhật các trường được gửi lên
                if (request.getStudentCode() != null && !request.getStudentCode().isBlank())
                    studentProfile.setStudentCode(request.getStudentCode().trim());

                if (request.getCohort() != null)
                    studentProfile.setCohort(request.getCohort());

                if (request.getMajorId() != null) {
                    Major major = majorRepository.findByIdAndDeletedAtIsNull(request.getMajorId())
                            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                                    "Không tìm thấy ngành học với id: " + request.getMajorId()));
                    studentProfile.setMajor(major);
                }
            }

            studentProfileRepository.save(studentProfile);
            log.info("✅ Đã lưu student_profile cho user ID: {}", id);
        }

        log.info("✅ Cập nhật tài khoản user ID: {} thành công", id);
        return getUserById(id);
    }
    // ============================================================
    // KHÓA TÀI KHOẢN (USER_LOCK)
    // PATCH /api/v1/users/{id}/lock
    // ============================================================
    @Transactional
    public ApiResponse<Void> lockUser(Long id, LockUserRequest request) {

        // 1. Tìm user, chưa bị xóa mềm
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng với id: " + id));

        // 2. Không cho khóa tài khoản đang bị khóa rồi
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Tài khoản này đã bị khóa trước đó!");
        }

        // 3. Không cho tự khóa bản thân
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Không thể tự khóa tài khoản của chính mình!");
        }

        // 4. Thực hiện khóa
        user.setIsActive(false);
        user.setLockReason(request.getLockReason().trim());
        userRepository.save(user);

        log.info("🔒 Tài khoản [{}] đã bị khóa bởi [{}]. Lý do: {}",
                user.getUsername(), currentUsername, request.getLockReason());

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Khóa tài khoản thành công!")
                .build();
    }

    // ============================================================
    // MỞ KHÓA TÀI KHOẢN (USER_UNLOCK)
    // PATCH /api/v1/users/{id}/unlock
    // ============================================================
    @Transactional
    public ApiResponse<Void> unlockUser(Long id) {

        // 1. Tìm user, chưa bị xóa mềm
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng với id: " + id));

        // 2. Không cho mở khóa tài khoản đang hoạt động
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Tài khoản này đang hoạt động bình thường, không cần mở khóa!");
        }

        // 3. Thực hiện mở khóa
        user.setIsActive(true);
        user.setLockReason(null); // Xóa lý do khóa
        userRepository.save(user);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("🔓 Tài khoản [{}] đã được mở khóa bởi [{}].",
                user.getUsername(), currentUsername);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Mở khóa tài khoản thành công!")
                .build();
    }
}