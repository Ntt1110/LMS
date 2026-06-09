package com.example.LMS.repository;

import com.example.LMS.entity.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption,Long> {

    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);

}
