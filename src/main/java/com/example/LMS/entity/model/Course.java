package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //FK -> departments.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(unique = true, nullable = false)
    private String code;        // CNTT101, KTQT201

    @Column(nullable = false)
    private String name; //Tên mon hoc

    @Column(nullable = false)
    private Integer credits; //Sotin chi

    @Column(name = "theoretica_hours")
    private Integer theoreticaHours; // so tiet ly thuyet

    @Column(name = "practical_hours")
    private Integer practicalHours; // so tiet thuc hanh

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;           // PENDING, APPROVED, REJECTED

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "lock_reason")
    private String lockReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
