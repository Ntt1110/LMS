package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> users.id  (LỖI CŨ: dùng @OneToMany sai, phải là @OneToOne vì 1 user chỉ có 1 student_profile)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK -> majors.id  (LỖI CŨ: dùng @OneToOne sai, phải là @ManyToOne vì nhiều SV cùng 1 ngành)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @Column(name = "student_code", unique = true, nullable = false)
    private String studentCode;

    private Integer cohort;

    // LỖI CŨ: import jakarta.transaction.Status sai, phải dùng enum nội bộ
    // LỖI CŨ: enum Status không khớp DB (ACTIVE/INACTIVE không có trong DB)
    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // FK -> users.id (giảng viên cố vấn, có thể null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id")
    private User advisor;

    // Đúng với DB: enum('STUDYING','RESERVED','SUSPENDED','GRADUATED','DROPPED_OUT')
    public enum Status {
        STUDYING, RESERVED, SUSPENDED, GRADUATED, DROPPED_OUT
    }
}