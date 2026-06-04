package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder

// phần xem danh sách môn học và lớp học cho Sinh viên
public class CourseWithClassesResponse {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private Integer theoreticalHours;
    private Integer practicalHours;
    private String departmentName;

    private List<ClassInfo> classes;

    @Getter
    @Builder
    public static class ClassInfo {
        private Long classId;
        private String classCode;
        private String status;
        private Integer maxStudents;
        private Integer currentStudents;
        private String lecturerName;
        private String semesterCode;
    }
}