package com.example.LMS.repository;

import com.example.LMS.entity.StudentExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentExamAnswerRepository extends JpaRepository<StudentExamAnswer, Long> {

    // 🔍 Dùng cho luồng Auto-save: Tìm xem câu hỏi này trong phiên này đã có đáp án chưa để ghi đè
    Optional<StudentExamAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    // 🔍 Dùng cho luồng Chấm điểm (Nộp bài): Gom hết tất cả đáp án sinh viên đã đánh lụi để so khớp kết quả
    List<StudentExamAnswer> findByAttemptId(Long attemptId);


}