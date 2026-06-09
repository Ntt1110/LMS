package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private ClassEnrollment enrollment;

    @Column(name = "regular_score_1")
    private Double regularScore1;

    @Column(name = "regular_score_2")
    private Double regularScore2;

    @Column(name = "midterm_score")
    private Double midtermScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "total_score")
    private Double totalScore;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING','PASS','FAIL')")
    private GradeStatus status = GradeStatus.PENDING;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum GradeStatus {
        PENDING, PASS, FAIL
    }
}