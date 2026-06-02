package com.example.LMS.repository;

import com.example.LMS.entity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Dùng khi đăng nhập
    Optional<User> findByUsername(String username);

    // Dùng khi xem chi tiết — chỉ lấy user chưa bị xóa mềm
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}