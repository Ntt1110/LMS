package com.example.LMS.repository;

import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.model.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    // Kiểm tra sinh viên đã đăng ký lớp này chưa
    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);

    // Lấy đăng ký cụ thể của 1 sinh viên trong 1 lớp
    Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classId, Long studentId);

    // Lấy toàn bộ lớp đã đăng ký của sinh viên (không bị DROPPED)
    List<ClassEnrollment> findByStudentIdAndStatusNot(Long studentId, EnrollmentStatus status);

    // Lấy danh sách sinh viên của một lớp (không bị DROPPED) — dùng cho giảng viên xem
    List<ClassEnrollment> findByClassEntityIdAndStatusNot(Long classId, EnrollmentStatus status);
    @Modifying
    @Query("UPDATE ClassEnrollment e SET e.status = :newStatus, e.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE e.classEntity.id = :classId AND e.status = :oldStatus")
    int updateStatusByClassId(
            @Param("classId") Long classId,
            @Param("oldStatus") EnrollmentStatus oldStatus,
            @Param("newStatus") EnrollmentStatus newStatus);
}