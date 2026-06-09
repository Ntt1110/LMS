package com.example.LMS.entity;

import com.example.LMS.entity.model.ExamQuestion;
import com.example.LMS.entity.model.QuestionOption;
import com.example.LMS.entity.model.StudentExamAttempt;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_exam_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //

    // 🌟 Mối quan hệ: Nhiều câu trả lời thuộc về 1 phiên làm bài (Attempt)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false) //
    private StudentExamAttempt attempt;

    // 🌟 Mối quan hệ: Nhiều câu trả lời map về cùng 1 câu hỏi gốc
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false) //
    private ExamQuestion question;

    // 🌟 Mối quan hệ: Nhiều câu trả lời có thể chọn chung 1 Option (đáp án)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id", nullable = false) //
    private QuestionOption selectedOption;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false) //
    private LocalDateTime createdAt;
}