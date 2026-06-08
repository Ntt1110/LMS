package com.example.LMS.repository;

import com.example.LMS.dto.response.DropdownResponseDto;
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

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.code = :roleCode AND u.deletedAt IS NULL")
    List<User> findAllByRoleCode(@Param("roleCode") String roleCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(u.id, p.fullName) " +
            "FROM User u " +
            "JOIN u.profile p " + // ✅ Chuẩn khít biến 'profile' trong User.java
            "JOIN u.roles r " +   // ✅ Chuẩn khít biến 'roles' trong User.java
            "WHERE r.code IN ('HEAD_OF_DEPT') AND u.isActive = true")
    List<DropdownResponseDto> findAllActiveLecturers();


    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(u.id, p.fullName) " +
            "FROM User u " +
            "JOIN u.profile p " +
            "JOIN u.roles r " +
            "JOIN u.teacherProfile tp " +
            "WHERE r.code = 'INSTRUCTOR' " +
            "AND u.isActive = true " +
            "AND tp.department.id = (SELECT c.department.id FROM Course c WHERE c.id = (SELECT cl.courseId FROM ClassEntity cl WHERE cl.id = :classId))")
    List<DropdownResponseDto> findInstructorsByClassDepartment(@Param("classId") Long classId);
}