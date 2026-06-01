package com.example.LMS.Config;

import com.example.LMS.entity.Enum.Role;
import com.example.LMS.entity.model.User;
import com.example.LMS.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring sẽ tự động inject Repository và mã hóa mật khẩu vào đây
    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Tự động tạo tài khoản ADMIN mẫu nếu chưa tồn tại trong DB
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            // Sử dụng passwordEncoder để tự băm mật khẩu thành chuẩn BCrypt trước khi lưu
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            System.out.println("✅ [Data Seeder] Đã khởi tạo tài khoản Admin thành công: admin / admin123");
        }

        // 2. Tự động tạo tài khoản STUDENT mẫu nếu chưa tồn tại trong DB
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole(Role.STUDENT);

            userRepository.save(student);
            System.out.println("✅ [Data Seeder] Đã khởi tạo tài khoản Student thành công: student / student123");
        }
    }
}
