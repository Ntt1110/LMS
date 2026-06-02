package com.example.LMS.repository;

import com.example.LMS.entity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Dùng khi đăng nhập
    Optional<User> findByUsername(String username);

    // Dùng khi xem chi tiết — chỉ lấy user chưa bị xóa mềm
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    @Query("SELECT DISTINCT u.id FROM User u JOIN u.roles r WHERE r.code IN :roleCodes")
    List<Long> findUserIdsByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}