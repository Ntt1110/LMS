package com.example.LMS.repository;

import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // Xem chi tiết theo id, chưa bị xóa mềm
    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    // Kiểm tra mã môn học đã tồn tại chưa (dùng khi đề xuất môn mới)
    boolean existsByCode(String code);
    Page<Course> findByStatusAndDeletedAtIsNull(Course.Status status, Pageable pageable);

    @Query("SELECT c.id FROM Course c WHERE c.department.id = :departmentId AND c.deletedAt IS NULL")
    List<Long> findIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT c.id FROM Course c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND c.deletedAt IS NULL")
    List<Long> findIdsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c.name FROM Course c WHERE c.id = :courseId")
    Optional<String> findNameById(@Param("courseId") Long courseId);


    // 1. Hàm bốc toàn bộ môn học phục vụ dropdown chung (Đã sửa full package name)
    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(c.id, CONCAT(c.code, ' - ', c.name)) " +
            "FROM Course c")
    List<DropdownResponseDto> findAllCoursesDropdown();

    // 2. Hàm lọc thông minh: Chỉ lấy môn học thuộc Khoa mà Trưởng khoa quản lý (Khớp 100% với Course.java)
    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(c.id, CONCAT(c.code, ' - ', c.name)) " +
            "FROM Course c " +
            "WHERE c.department.id = :departmentId")
    List<DropdownResponseDto> findCoursesByDepartmentId(@Param("departmentId") Long departmentId);
}