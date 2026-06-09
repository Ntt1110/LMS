package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassGradeListResponseDto {

    // Thong tin lop
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer totalStudents;

    // Danh sach diem tung sinh vien
    private List<StudentGradeDto> students;

    @Getter
    @Builder
    public static class StudentGradeDto {
        private Long studentId;
        private String studentCode;
        private String fullName;
        private String email;

        // Diem so
        private Double regularScore1;
        private Double regularScore2;
        private Double midtermScore;
        private Double finalScore;
        private Double totalScore;
        private String status;          // PENDING / PASS / FAIL
    }
}