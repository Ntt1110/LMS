package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.ExamStatus;
import com.example.LMS.entity.Enum.ExamType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    @Column(name = "time_limit")
    private Integer timeLimit;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExamStatus status = ExamStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
