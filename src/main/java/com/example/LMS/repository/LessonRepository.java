package com.example.LMS.repository;

import com.example.LMS.entity.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByClassEntityIdAndDeletedAtIsNullOrderByOrderIndexAsc(Long classId);
    // 🌟 Dành riêng cho Sinh Viên: Chỉ lấy bài học đã Published (isPublished = true)
    @Query("SELECT l FROM Lesson l " +
            "WHERE l.classEntity.id = :classId " +
            "AND l.isPublished = true " +
            "AND l.deletedAt IS NULL " +
            "ORDER BY l.orderIndex ASC")
    List<Lesson> findPublishedLessonsByClassId(@Param("classId") Long classId);

}
