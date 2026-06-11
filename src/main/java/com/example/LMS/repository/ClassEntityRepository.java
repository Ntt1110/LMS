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

    // Lấy lớp theo môn + trạng thái + chưa xóa (dùng cho sinh viên xem đăng ký)
    List<ClassEntity> findByCourseIdAndStatusAndDeletedAtIsNull(Long courseId, ClassStatus status);

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

    List<ClassEntity> findByRegistrationPeriodIdAndStatus(Long registrationPeriodId, ClassStatus status);

    // Lấy danh sách lớp được phân công cho giảng viên
    List<ClassEntity> findByLecturerIdAndDeletedAtIsNull(Long lecturerId);

    // Lấy danh sách lớp sinh viên đang học hoặc đã hoàn thành (query thẳng từ bảng classes)
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN ClassEnrollment e ON e.classEntity.id = c.id " +
            "WHERE e.studentId = :studentId " +
            "AND e.status != com.example.LMS.entity.Enum.EnrollmentStatus.DROPPED " +
            "AND c.status IN (" +
            "   com.example.LMS.entity.Enum.ClassStatus.REGISTRATION" +
            ") " +
            "AND c.deletedAt IS NULL")
    List<ClassEntity> findActiveClassesByStudentId(@Param("studentId") Long studentId);



    // Lấy lịch học của một lớp cụ thể (dùng cho API chi tiết lớp của sinh viên)
    @Query("""
        SELECT cs FROM ClassSchedule cs
        WHERE cs.classEntity.id = :classId
        AND cs.deletedAt IS NULL
    """)
    List<com.example.LMS.entity.model.ClassSchedule> findSchedulesByClassId(@Param("classId") Long classId);

    // Lấy danh sách lớp đã đăng ký theo trạng thái (ONGOING / COMPLETED)
    @Query("""
        SELECT c FROM ClassEntity c
        JOIN ClassEnrollment e ON e.classEntity.id = c.id
        WHERE e.studentId = :studentId
        AND e.status != com.example.LMS.entity.Enum.EnrollmentStatus.DROPPED
        AND c.status = :classStatus
        AND c.deletedAt IS NULL
    """)
    List<ClassEntity> findClassesByStudentIdAndClassStatus(
            @Param("studentId") Long studentId,
            @Param("classStatus") ClassStatus classStatus
    );
}