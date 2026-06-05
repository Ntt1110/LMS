package com.example.LMS.dto.response;

import com.example.LMS.entity.model.StudentProfile;
import com.example.LMS.entity.model.TeacherProfile;
import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserResponse {

    // === Từ bảng users ===
    private Long id;
    private String username;
    private String email;
    private Boolean isActive;
    private String lockReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Từ bảng user_profiles ===
    private String fullName;
    private String phone;
    private LocalDate birthday;
    private String gender;
    private String avatarUrl;
    private String address;

    // === Từ bảng roles (qua user_roles) ===
    private Set<String> roles; // ["ADMIN"], ["INSTRUCTOR"], ["STUDENT"], v.v.

    // === Từ bảng teacher_profiles (chỉ có nếu là INSTRUCTOR / HEAD_OF_DEPT) ===
    private String employeeCode;
    private String academicTitle;
    private String specialization;
    private Boolean isVisiting;
    private LocalDate hireDate;
    private Long departmentId;
    private String departmentName;

    // === Từ bảng student_profiles (chỉ có nếu là STUDENT) ===
    private String studentCode;
    private Integer cohort;
    private String studentStatus;   // STUDYING | RESERVED | SUSPENDED | GRADUATED | DROPPED_OUT
    private Long majorId;
    private String majorName;
    private String majorCode;
    private Long advisorId;
    private String advisorName;

    /**
     * Map từ entity User sang DTO — hỗ trợ cả TeacherProfile lẫn StudentProfile.
     * Gọi overload này khi đã có đủ cả 3 object (ví dụ: getUserById).
     */
    public static UserResponse fromEntity(User user,
                                          UserProfile profile,
                                          TeacherProfile teacherProfile,
                                          StudentProfile studentProfile) {
        UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .lockReason(user.getLockReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getRoles().stream()
                        .map(role -> role.getCode())
                        .collect(Collectors.toSet()));

        // Profile cơ bản (user_profiles)
        if (profile != null) {
            builder.fullName(profile.getFullName())
                    .phone(profile.getPhone())
                    .birthday(profile.getBirthday())
                    .gender(profile.getGender() != null ? profile.getGender().name() : null)
                    .avatarUrl(profile.getAvatarUrl())
                    .address(profile.getAddress());
        }

        // Thông tin giảng viên (teacher_profiles)
        if (teacherProfile != null) {
            builder.employeeCode(teacherProfile.getEmployeeCode())
                    .academicTitle(teacherProfile.getAcademicTitle())
                    .specialization(teacherProfile.getSpecialization())
                    .isVisiting(teacherProfile.getIsVisiting())
                    .hireDate(teacherProfile.getHireDate())
                    .departmentId(teacherProfile.getDepartment().getId())
                    .departmentName(teacherProfile.getDepartment().getName());
        }

        // Thông tin sinh viên (student_profiles)
        if (studentProfile != null) {
            builder.studentCode(studentProfile.getStudentCode())
                    .cohort(studentProfile.getCohort())
                    .studentStatus(studentProfile.getStatus() != null
                            ? studentProfile.getStatus().name() : null);
            if (studentProfile.getMajor() != null) {
                builder.majorId(studentProfile.getMajor().getId())
                        .majorName(studentProfile.getMajor().getName())
                        .majorCode(studentProfile.getMajor().getCode())
                        .advisorId(studentProfile.getAdvisor() != null
                                ? studentProfile.getAdvisor().getId() : null)
                        .advisorName(studentProfile.getAdvisor() != null
                                && studentProfile.getAdvisor().getProfile() != null
                                ? studentProfile.getAdvisor().getProfile().getFullName() : null);
            }
        }
        return builder.build();
    }

    /**
     * Overload tương thích ngược — dùng cho danh sách (list) khi chưa load StudentProfile.
     * TeacherProfile đã có sẵn trong teacherProfileMap, StudentProfile truyền null.
     */
    public static UserResponse fromEntity(User user,
                                           UserProfile profile,
                                          TeacherProfile teacherProfile) {
        // Lấy StudentProfile từ quan hệ lazy trên entity (đã JOIN sẵn hoặc null)
        return fromEntity(user, profile, teacherProfile, user.getStudentProfile());
    }
}