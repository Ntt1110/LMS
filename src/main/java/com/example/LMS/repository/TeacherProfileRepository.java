package com.example.LMS.repository;

import com.example.LMS.entity.model.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    List<TeacherProfile> findAllByUserIdIn(List<Long> userIds);
}