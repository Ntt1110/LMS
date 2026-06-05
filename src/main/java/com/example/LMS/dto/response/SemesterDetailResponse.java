package com.example.LMS.dto.response;

import com.example.LMS.entity.model.Semester;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SemesterDetailResponse {

    private Long id;
    private String semesterCode;
    private String academicYear;
    private Integer semesterNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SemesterDetailResponse fromEntity(Semester semester) {
        return SemesterDetailResponse.builder()
                .id(semester.getId())
                .semesterCode(semester.getSemesterCode())
                .academicYear(semester.getAcademicYear())
                .semesterNumber(semester.getSemesterNumber())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .status(semester.getStatus() != null ? semester.getStatus().name() : null)
                .createdAt(semester.getCreatedAt())
                .updatedAt(semester.getUpdatedAt())
                .build();
    }
}