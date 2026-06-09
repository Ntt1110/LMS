package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassGradeRepository extends JpaRepository<ClassGrade, Long> {

    // Lấy bảng điểm theo enrollmentId
    Optional<ClassGrade> findByEnrollmentId(Long enrollmentId);

    // Lấy bảng điểm theo classId + studentId (dành cho sinh viên)
    @Query("SELECT g FROM ClassGrade g " +
            "WHERE g.enrollment.classEntity.id = :classId " +
            "AND g.enrollment.studentId = :studentId")
    Optional<ClassGrade> findByClassIdAndStudentId(@Param("classId") Long classId,
                                                   @Param("studentId") Long studentId);

    // Lấy toàn bộ bảng điểm của lớp (dành cho giảng viên)
    @Query("SELECT g FROM ClassGrade g " +
            "WHERE g.enrollment.classEntity.id = :classId " +
            "ORDER BY g.enrollment.studentId ASC")
    List<ClassGrade> findAllByClassId(@Param("classId") Long classId);
}