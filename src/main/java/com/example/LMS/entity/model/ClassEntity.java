package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.ClassStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "classes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"semester_id", "code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "registration_period_id")
    private Long registrationPeriodId; // Tạm thời để Long nếu chưa dựng bảng Period

    @Column(name = "course_id", nullable = false)
    private Long courseId; // Tạm thời để Long nếu chưa dựng bảng Course

    @Column(name = "manager_id", nullable = false)
    private Long managerId; // Liên kết tới User quản lý lớp (Trưởng bộ môn)

    @Column(name = "lecturer_id")
    private Long lecturerId; // Liên kết tới User giảng viên dạy lớp

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents = 40;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING', 'REGISTRATION', 'ONGOING', 'COMPLETED', 'CANCELED')")
    private ClassStatus status = ClassStatus.PENDING;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Lịch học của lớp này
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
    private List<ClassSchedule> schedules;

    // Danh sách sinh viên đăng ký lớp này
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
    private List<ClassEnrollment> enrollments;
}
