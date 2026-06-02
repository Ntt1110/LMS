package com.example.LMS.repository;

import com.example.LMS.entity.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // Xem chi tiết theo id, chưa bị xóa mềm
    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    // Kiểm tra mã môn học đã tồn tại chưa (dùng khi đề xuất môn mới)
    boolean existsByCode(String code);
}