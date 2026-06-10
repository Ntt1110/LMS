package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GradeResponseDto {

    // Thông tin lớp / môn
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;

    // Điểm số
    private Double regularScore1;   // Điểm thường xuyên 1
    private Double regularScore2;   // Điểm thường xuyên 2
    private Double midtermScore;    // Điểm giữa kỳ
    private Double finalScore;      // Điểm cuối kỳ
    private Double totalScore;      // Điểm tổng kết (thang 10)
    private Double grade4;          // Điểm hệ 4
    private String letterGrade;     // Điểm chữ: A, B+, B, C+, C, D+, D, F

    // Kết quả
    private String status;          // PENDING / PASS / FAIL
}