package com.example.LMS.repository;

import com.example.LMS.entity.model.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {
    // Có thể thêm các hàm tìm kiếm material theo lesson_id sau này
    List<LessonMaterial> findByLessonIdAndDeletedAtIsNull(Long lessonId);
}
