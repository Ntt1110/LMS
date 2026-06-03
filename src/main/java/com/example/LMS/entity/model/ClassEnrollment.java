package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;



@Entity
@Table(name = "class_enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"class_id", "student_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Column(name = "student_id", nullable = false)
    private Long studentId; // ID của User mang Role STUDENT

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('REGISTERED', 'OFFICIAL', 'DROPPED')")
    private EnrollmentStatus status = EnrollmentStatus.REGISTERED;

    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}