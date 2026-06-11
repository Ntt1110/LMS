package com.example.LMS.dto.response;

import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.Course;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassInPeriodResponse {

    private Long id;

    // Mã lớp
    private String code;

    // Tên môn học
    private String courseName;

    // Sĩ số đã đăng ký / Sĩ số tối đa
    private Integer enrolledCount;
    private Integer maxStudents;

    // Tên giảng viên
    private String lecturerName;

    public static ClassInPeriodResponse fromEntity(
            ClassEntity c,
            Course course,
            int enrolledCount,
            String lecturerName
    ) {
        return ClassInPeriodResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .courseName(course != null ? course.getName() : null)
                .enrolledCount(enrolledCount)
                .maxStudents(c.getMaxStudents())
                .lecturerName(lecturerName)
                .build();
    }
}