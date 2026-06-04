package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassEntityRepository extends JpaRepository<ClassEntity,Long> {
    long countBySemesterIdAndCourseId(Long semesterId, Long courseId);


    List<ClassEntity> findByCourseIdAndDeletedAtIsNull(Long courseId);

    //  phần xem danh sách môn học và lớp học cho Sinh viên,  Thêm query đếm số SV đã đăng ký:
    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.classEntity.id = :classId")
    int countEnrollmentsByClassId(@Param("classId") Long classId);
}
