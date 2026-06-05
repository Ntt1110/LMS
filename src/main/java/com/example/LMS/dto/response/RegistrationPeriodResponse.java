package com.example.LMS.dto.response;

import com.example.LMS.entity.model.RegistrationPeriod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RegistrationPeriodResponse {

    private Long id;
    private String name;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String targetCohorts;
    private String targetDepartments;

    // Thông tin học kỳ
    private Long semesterId;
    private String semesterCode;
    private String academicYear;
    private Integer semesterNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RegistrationPeriodResponse fromEntity(RegistrationPeriod p) {
        return RegistrationPeriodResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .targetCohorts(p.getTargetCohorts())
                .targetDepartments(p.getTargetDepartments())
                .semesterId(p.getSemester().getId())
                .semesterCode(p.getSemester().getSemesterCode())
                .academicYear(p.getSemester().getAcademicYear())
                .semesterNumber(p.getSemester().getSemesterNumber())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}