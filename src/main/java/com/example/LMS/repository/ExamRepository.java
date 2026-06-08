package com.example.LMS.repository;


import com.example.LMS.entity.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    // DÀNH CHO GIẢNG VIÊN: Lấy toàn bộ bài kiểm tra (Chưa xóa) của một lớp
    @Query("SELECT e FROM Exam e " +
            "WHERE e.classEntity.id = :classId " +
            "ORDER BY e.createdAt DESC")
    List<Exam> findAllExamsByClassId(@Param("classId") Long classId);

    // DÀNH CHO SINH VIÊN: Lấy các bài kiểm tra KHÁC trạng thái DRAFT (Tức là PUBLISHED hoặc CLOSED)
    @Query("SELECT e FROM Exam e " +
            "WHERE e.classEntity.id = :classId " +
            "AND e.status != 'DRAFT' " +
            "AND e.deletedAt IS NULL " +
            "ORDER BY e.createdAt DESC")
    List<Exam> findActiveExamsForStudent(@Param("classId") Long classId);
}