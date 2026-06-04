package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule,Long> {

    boolean existsByRoomIdAndDayOfWeekAndShiftId(Long roomId, Integer dayOfWeek, Long shiftId);

    boolean existsByClassEntityLecturerIdAndDayOfWeekAndShiftId(Long lecturerId, Integer dayOfWeek, Long shiftId);
}
