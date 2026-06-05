package com.example.LMS.dto.response;

import com.example.LMS.entity.model.Semester;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SemesterResponse {

    private Long id;
    private String semesterCode;
    private String academicYear;

    public static SemesterResponse fromEntity(Semester semester) {
        return SemesterResponse.builder()
                .id(semester.getId())
                .semesterCode(semester.getSemesterCode())
                .academicYear(semester.getAcademicYear())
                .build();
    }
}