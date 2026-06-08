package com.example.LMS.repository;

import com.example.LMS.entity.model.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository  extends JpaRepository<ExamQuestion,Long> {
}
