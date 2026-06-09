package com.example.LMS.repository;

import com.example.LMS.entity.model.StudentExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentExamAttemptRepository extends JpaRepository<StudentExamAttempt, Long> {

    // Tìm lịch sử làm bài của 1 sinh viên trong 1 bài thi
    Optional<StudentExamAttempt> findByExamIdAndStudentId(Long examId, Long studentId);
}
