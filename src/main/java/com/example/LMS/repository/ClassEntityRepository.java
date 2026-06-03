package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassEntityRepository extends JpaRepository<ClassEntity,Long> {
    long countBySemesterIdAndCourseId(Long semesterId, Long courseId);

}
