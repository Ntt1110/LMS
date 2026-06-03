package com.example.LMS.repository;

import com.example.LMS.entity.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    @Query("SELECT sp FROM StudentProfile sp WHERE sp.user.id IN :userIds")
    List<StudentProfile> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}