package com.example.LMS.entity.model;

import com.example.LMS.entity.model.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    @Column(name = "password_hash")
    private String password;

    @Column(name = "is_active")
    @Builder.Default // Giữ nguyên giá trị mặc định là true khi dùng @Builder
    private Boolean isActive = true;

    @Column(name = "lock_reason")
    private String lockReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles", // Tên bảng trung gian trong Database của team
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    // Quan hệ 1-1 với user_profiles
    // mappedBy = "user" vì UserProfile là bên sở hữu FK (user_profiles.user_id)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    // Quan hệ 1-1 với student_profiles (chỉ có nếu user là STUDENT)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private StudentProfile studentProfile;

    // Quan hệ 1-1 với teacher_profiles (chỉ có nếu user là INSTRUCTOR)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private TeacherProfile teacherProfile;
    /**
     * Trả về cả ROLE_ prefix lẫn từng permission code.
     * Ví dụ user ADMIN sẽ có:
     *   - ROLE_ADMIN
     *   - USER_VIEW, USER_CREATE, USER_ASSIGN_ROLE, ... (tất cả permission của ADMIN)
     *
     * Nhờ đó @PreAuthorize("hasAuthority('USER_VIEW')") trong Controller mới hoạt động.
     */

    // CẬP NHẬT LẠI HÀM NÀY:
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.roles == null || this.roles.isEmpty()) {
            return List.of();
        }
        // Duyệt qua danh sách Role của user và chuyển thành quyền của Spring Security
        List<SimpleGrantedAuthority> roleAuthorities = this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .toList();

        // 2. Thu thập thêm tất cả các Permission chi tiết từ các Role đó
        List<SimpleGrantedAuthority> permissionAuthorities = this.roles.stream()
                .filter(role -> role.getPermissions() != null) // Tránh lỗi NullPointerException nếu role chưa có quyền
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getCode())) // Đổi sang SimpleGrantedAuthority (VD: "USER_VIEW")
                .toList();

        // 3. Gộp cả hai danh sách (Role + Permission) lại làm một và trả về
        List<SimpleGrantedAuthority> totalAuthorities = new ArrayList<>();
        totalAuthorities.addAll(roleAuthorities);
        totalAuthorities.addAll(permissionAuthorities);

        return totalAuthorities;
    }
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Trả về true để tài khoản không bị hết hạn
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Trả về true để tài khoản không bị khóa
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Trạng thái kích hoạt của tài khoản
    }
}