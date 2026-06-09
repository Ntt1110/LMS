package com.example.LMS.repository;

import com.example.LMS.entity.model.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamQuestionRepository  extends JpaRepository<ExamQuestion,Long> {
    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(Long examId);
}
