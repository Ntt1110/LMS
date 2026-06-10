package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserRequest {

    // === CHUNG (user_profiles) ===
    private String phone;
    private LocalDate birthday;
    private String gender;      // MALE | FEMALE | OTHER
    private String address;

    // === GIẢNG VIÊN (teacher_profiles) ===
    private String employeeCode;    // Mã GV
    private Long departmentId;      // Id khoa
    private String academicTitle;   // Học hàm học vị: ThS, TS, PGS.TS...
    private String specialization;  // Chuyên môn
    private Boolean isVisiting;     // Loại hợp đồng: false=cơ hữu, true=thỉnh giảng

    // === SINH VIÊN (student_profiles) ===
    private String studentCode;     // Mã SV
    private Integer cohort;         // Khóa học
    private Long majorId;           // Ngành học
}