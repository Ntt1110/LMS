package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule,Long> {

    boolean existsByRoomIdAndDayOfWeekAndShiftId(Long roomId, Integer dayOfWeek, Long shiftId);

    boolean existsByClassEntityLecturerIdAndDayOfWeekAndShiftId(Long lecturerId, Integer dayOfWeek, Long shiftId);

    // Lấy lịch học theo classId
    @Query("""
        SELECT cs FROM ClassSchedule cs
        WHERE cs.classEntity.id = :classId
        AND cs.deletedAt IS NULL
    """)
    List<ClassSchedule> findByClassId(@Param("classId") Long classId);
}
