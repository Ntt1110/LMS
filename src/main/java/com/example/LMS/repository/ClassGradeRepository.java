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

    // [TRANSCRIPT] Lấy toàn bộ điểm của một sinh viên — chỉ lấy học kỳ đã CLOSED
    @Query("SELECT g FROM ClassGrade g " +
            "WHERE g.enrollment.studentId = :studentId " +
            "AND g.enrollment.status != com.example.LMS.entity.Enum.EnrollmentStatus.DROPPED " +
            "AND g.enrollment.classEntity.semester.status = com.example.LMS.entity.model.Semester.SemesterStatus.CLOSED " +
            "ORDER BY g.enrollment.classEntity.semester.startDate ASC, " +
            "         g.enrollment.classEntity.courseId ASC")
    List<ClassGrade> findAllByStudentId(@Param("studentId") Long studentId);

    // Kiểm tra còn sinh viên nào có status PENDING trong lớp không
    @Query("SELECT COUNT(g) > 0 FROM ClassGrade g " +
            "WHERE g.enrollment.classEntity.id = :classId " +
            "AND g.status = com.example.LMS.entity.model.ClassGrade.GradeStatus.PENDING")
    boolean existsPendingByClassId(@Param("classId") Long classId);

    // Đếm số enrollment (không DROPPED) chưa có bảng điểm trong lớp
    @Query("SELECT COUNT(e) FROM ClassEnrollment e " +
            "WHERE e.classEntity.id = :classId " +
            "AND e.status != com.example.LMS.entity.Enum.EnrollmentStatus.DROPPED " +
            "AND NOT EXISTS (SELECT g FROM ClassGrade g WHERE g.enrollment.id = e.id)")
    long countEnrollmentsWithoutGrade(@Param("classId") Long classId);
}