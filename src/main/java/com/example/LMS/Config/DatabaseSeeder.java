package com.example.LMS.Config;




import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.Role;

import com.example.LMS.repository.RoleRepository;
import com.example.LMS.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Đảm bảo ghi thành công vào cả 3 bảng, nếu lỗi sẽ tự động rollback
    public void run(String... args) throws Exception {

        // ==========================================
        // BƯỚC 1: KHỞI TẠO ROLES TRƯỚC (Nếu chưa có)
        // ==========================================
        if (roleRepository.count() == 0) {
            System.out.println("⏳ Đang khởi tạo dữ liệu mẫu cho bảng ROLES...");
            roleRepository.save(createRole("ADMIN", "Quản trị viên", "Quản lý toàn hệ thống"));
            roleRepository.save(createRole("PRINCIPAL", "Hiệu trưởng", "Ban giám hiệu"));
            roleRepository.save(createRole("HR", "Phòng Nhân sự", "Quản lý nhân sự"));
            roleRepository.save(createRole("TRAINING_DEPT", "Phòng Đào tạo", "Quản lý chương trình học"));
            roleRepository.save(createRole("HEAD_OF_DEPT", "Trưởng khoa", "Quản lý khoa"));
            roleRepository.save(createRole("INSTRUCTOR", "Giảng viên", "Người dạy học"));
            roleRepository.save(createRole("STUDENT", "Sinh viên", "Người học"));
        }

        // Lấy các Role từ Database lên để chuẩn bị gắn cho User
        Role adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        Role principalRole = roleRepository.findByCode("PRINCIPAL").orElseThrow();
        Role hrRole = roleRepository.findByCode("HR").orElseThrow();
        Role trainingRole = roleRepository.findByCode("TRAINING_DEPT").orElseThrow();
        Role headRole = roleRepository.findByCode("HEAD_OF_DEPT").orElseThrow();
        Role instructorRole = roleRepository.findByCode("INSTRUCTOR").orElseThrow();
        Role studentRole = roleRepository.findByCode("STUDENT").orElseThrow();

        // ==========================================
        // BƯỚC 2: KHỞI TẠO 54 USERS VÀ GẮN ROLES
        // ==========================================
        if (userRepository.count() == 0) {
            System.out.println("⏳ Đang khởi tạo 54 USERS và liên kết bảng USER_ROLES...");
            List<User> users = new ArrayList<>();
            String defaultPassword = passwordEncoder.encode("User@123");

            // 1 Admin
            users.add(createUser("admin", defaultPassword, adminRole));
            // 1 Hiệu trưởng
            users.add(createUser("principal", defaultPassword, principalRole));
            // 1 HR
            users.add(createUser("hr", defaultPassword, hrRole));
            // 1 Phòng đào tạo
            users.add(createUser("training", defaultPassword, trainingRole));

            // 2 Trưởng khoa
            for (int i = 1; i <= 2; i++) {
                users.add(createUser("head" + i, defaultPassword, headRole));
            }

            // 8 Giảng viên
            for (int i = 1; i <= 8; i++) {
                users.add(createUser("instructor" + i, defaultPassword, instructorRole));
            }

            // 40 Sinh viên
            for (int i = 1; i <= 40; i++) {
                users.add(createUser("student" + i, defaultPassword, studentRole));
            }

            // Lưu toàn bộ danh sách xuống Database
            userRepository.saveAll(users);
            System.out.println("✅ Đã chèn thành công toàn bộ dữ liệu vào 3 bảng: roles, users, user_roles!");
        }
    }

    // ==========================================
    // CÁC HÀM PHỤ TRỢ (Giúp code chính gọn gàng)
    // ==========================================

    private Role createRole(String code, String name, String description) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return role;
    }

    private User createUser(String username, String encodedPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setIsActive(true); // Đảm bảo tài khoản được kích hoạt
        user.setRoles(Set.of(role)); // Gắn Role vào Set của User
        return user;
    }
}