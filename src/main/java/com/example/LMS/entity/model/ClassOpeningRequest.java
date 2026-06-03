package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.ClassOpenningStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_opening_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassOpeningRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "course_id", nullable = false)
    private Long courseId; // Giữ Long nếu chưa map bảng Course, hoặc map @ManyToOne nếu đã có

    @Column(name = "requester_id", nullable = false)
    private Long requesterId; // ID của giảng viên/trưởng bộ môn làm đơn đề xuất

    @Column(name = "expected_students", nullable = false)
    private Integer expectedStudents = 40;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING', 'APPROVED', 'REJECTED')")
    private ClassOpenningStatus status = ClassOpenningStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


}