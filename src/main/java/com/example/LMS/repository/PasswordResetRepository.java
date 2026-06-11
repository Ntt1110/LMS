package com.example.LMS.repository;

import com.example.LMS.entity.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    // 🔍 Tìm bản ghi đặt lại mật khẩu dựa trên chuỗi Token còn sống
    Optional<PasswordReset> findByToken(String token);
}
