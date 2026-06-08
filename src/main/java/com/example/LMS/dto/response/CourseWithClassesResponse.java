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

    // Thông tin môn học
    private Long courseId;
    private String courseName;
    private Integer credits;

    private List<ClassInfo> classes;

    @Getter
    @Builder
    public static class ClassInfo {
        // Thông tin lớp
        private Long classId;
        private String classCode;

        // Giảng viên
        private String lecturerName;

        // Lịch học
        private Integer dayOfWeek;   // Thứ mấy (2=Thứ 2, 3=Thứ 3...)
        private String shiftName;    // Ca mấy (Ca 1, Ca 2...)
        private String roomName;     // Phòng nào

        // Sĩ số
        private Integer currentStudents;  // Sĩ số hiện tại
        private Integer maxStudents;      // Sĩ số tối đa
    }
}