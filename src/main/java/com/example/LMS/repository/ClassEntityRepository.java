package com.example.LMS.repository;

import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, Long>, JpaSpecificationExecutor<ClassEntity> {

    long countBySemesterIdAndCourseId(Long semesterId, Long courseId);

    List<ClassEntity> findByCourseIdAndDeletedAtIsNull(Long courseId);

    // Đếm số SV đã đăng ký một lớp
    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.classEntity.id = :classId")
    int countEnrollmentsByClassId(@Param("classId") Long classId);

    // Lấy danh sách học phần PENDING theo học kỳ và khoa
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN Course co ON co.id = c.courseId " +
            "WHERE c.semester.id = :semesterId " +
            "AND co.department.id = :departmentId " +
            "AND c.status = com.example.LMS.entity.Enum.ClassStatus.PENDING " +
            "AND c.deletedAt IS NULL")
    List<ClassEntity> findPendingBySemesterAndDepartment(
            @Param("semesterId") Long semesterId,
            @Param("departmentId") Long departmentId
    );

    List<ClassEntity> findBySemesterIdAndStatus(Long semesterId, ClassStatus status);

    // Lấy lịch học của một lớp cụ thể (dùng cho API chi tiết lớp của sinh viên)
    @Query("""
        SELECT cs FROM ClassSchedule cs
        WHERE cs.classEntity.id = :classId
        AND cs.deletedAt IS NULL
    """)
    List<com.example.LMS.entity.model.ClassSchedule> findSchedulesByClassId(@Param("classId") Long classId);
}