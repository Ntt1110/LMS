package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.RegistrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "registration_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String type = "NORMAL";

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // ✅ ĐÃ CHUYỂN VỀ STRING THUẦN: Nhường việc ép kiểu JSON lại cho tầng Service xử lý
    @Column(name = "target_cohorts", nullable = false, columnDefinition = "json")
    private String targetCohorts;

    @Column(name = "target_departments", nullable = false, columnDefinition = "json")
    private String targetDepartments;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING', 'ACTIVE', 'CLOSED')")
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}