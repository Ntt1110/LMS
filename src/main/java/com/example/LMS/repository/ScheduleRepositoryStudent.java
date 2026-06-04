package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepositoryStudent extends JpaRepository<ClassSchedule, Long> {

    @Query("""
        SELECT cs FROM ClassSchedule cs
        JOIN cs.classEntity c
        JOIN ClassEnrollment e ON e.classEntity.id = c.id
        WHERE e.studentId = :studentId
        AND e.status != 'DROPPED'
        AND cs.deletedAt IS NULL
    """)
    List<ClassSchedule> findSchedulesByStudentId(@Param("studentId") Long studentId);
}