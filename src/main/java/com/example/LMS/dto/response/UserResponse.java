package com.example.LMS.dto.response;

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

    // === Từ bảng teacher_profiles (chỉ có nếu là INSTRUCTOR) ===
    private String employeeCode;
    private String academicTitle;
    private String specialization;
    private Boolean isVisiting;
    private LocalDate hireDate;
    private Long departmentId;
    private String departmentName;

    /**
     * Map từ entity User (đã JOIN sẵn profile) sang DTO
     */
    public static UserResponse fromEntity(User user, UserProfile profile, TeacherProfile teacherProfile) {
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

        // Profile có thể null nếu user chưa có profile (phòng thủ)
        if (profile != null) {
            builder.fullName(profile.getFullName())
                    .phone(profile.getPhone())
                    .birthday(profile.getBirthday())
                    .gender(profile.getGender() != null ? profile.getGender().name() : null)
                    .avatarUrl(profile.getAvatarUrl())
                    .address(profile.getAddress());
        }
        if (teacherProfile != null) {
            builder.employeeCode(teacherProfile.getEmployeeCode())
                    .academicTitle(teacherProfile.getAcademicTitle())
                    .specialization(teacherProfile.getSpecialization())
                    .isVisiting(teacherProfile.getIsVisiting())
                    .hireDate(teacherProfile.getHireDate())
                    .departmentId(teacherProfile.getDepartment().getId())
                    .departmentName(teacherProfile.getDepartment().getName());
        }

        return builder.build();
    }
}