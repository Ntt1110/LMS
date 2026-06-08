package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🌟 KHÓA NGOẠI: Nối về câu hỏi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Nội dung đáp án (VD: 4, 5, 6...)

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect; // Đánh dấu đáp án đúng (true/false)

    @Column(name = "order_index")
    private Integer orderIndex; // Thứ tự đáp án (A=1, B=2...)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}