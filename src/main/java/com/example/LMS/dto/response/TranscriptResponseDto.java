package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TranscriptResponseDto {

    // Tổng quan toàn khoá
    private Double gpaOverall;          // GPA tích luỹ toàn khoá (thang 10)
    private Double gpaOverall4;         // GPA tích luỹ toàn khoá (thang 4)
    private Integer totalCreditsEarned; // Tổng tín chỉ đã qua (PASS)
    private Integer totalSubjects;      // Tổng số môn đã học

    // Danh sách kết quả từng học kỳ
    private List<SemesterTranscriptDto> semesters;

    // -------------------------------------------------------

    @Getter
    @Builder
    public static class SemesterTranscriptDto {
        private Long semesterId;
        private String semesterCode;    // VD: HK1-2024-2025
        private String academicYear;    // VD: 2024-2025
        private Integer semesterNumber; // 1, 2, 3

        private Double gpaThisSemester;     // GPA học kỳ này (thang 10)
        private Double gpaThisSemester4;    // GPA học kỳ này (thang 4)
        private Integer creditsThisSemester;        // Tín chỉ đăng ký học kỳ này
        private Integer creditsEarnedThisSemester;  // Tín chỉ qua được học kỳ này

        private List<SubjectGradeDto> subjects;
    }

    // -------------------------------------------------------

    @Getter
    @Builder
    public static class SubjectGradeDto {
        private Long classId;
        private String classCode;
        private String courseCode;
        private String courseName;
        private Integer credits;

        private Double regularScore1;
        private Double regularScore2;
        private Double midtermScore;
        private Double finalScore;
        private Double totalScore;      // Điểm tổng kết (thang 10)
        private Double grade4;          // Điểm hệ 4
        private String letterGrade;     // Điểm chữ: A, B+, B, C+, C, D+, D, F

        private String status;          // PENDING / PASS / FAIL
    }
}