package com.example.LMS.entity.model;

import com.example.LMS.entity.Enum.RoomType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // Ví dụ: A1, B2, LAB1...

    @Column(nullable = false)
    private Integer capacity; // Sức chứa (Ví dụ: 40, 50, 100 sinh viên)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('THEORY', 'LAB', 'HALL')")
    private RoomType type;


}