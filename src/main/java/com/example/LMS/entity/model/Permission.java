package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ví dụ: USER_VIEW, USER_CREATE, MAJOR_VIEW, ...
    @Column(unique = true, nullable = false)
    private String code;

    // Ví dụ: "Xem danh sách và chi tiết người dùng"
    private String name;

    // Ví dụ: USER_MGT, COURSE_MGT, ...
    private String module;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}