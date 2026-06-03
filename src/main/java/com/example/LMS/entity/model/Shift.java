package com.example.LMS.entity.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // Ví dụ: Ca 1 (Sáng), Ca 2 (Sáng)...

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // Thời gian bắt đầu (Ví dụ: 07:00:00)

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // Thời gian kết thúc (Ví dụ: 09:15:00)
}