package com.example.LMS.repository;

import com.example.LMS.entity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Hàm này rất quan trọng để tìm User lúc đăng nhập
    Optional<User> findByUsername(String username);
}