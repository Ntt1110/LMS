package com.example.LMS.service;

import com.example.LMS.dto.response.NotificationResponse;
import com.example.LMS.entity.model.Notification;
import com.example.LMS.entity.model.StudentProfile;
import com.example.LMS.entity.model.User;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.NotificationRepository;
import com.example.LMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // 🌟 1. DÀNH CHO GIÁO VỤ: TẠO VÀ PHÁT HÀNH THÔNG BÁO MỞ LỚP
    // =========================================================================
    @Transactional
    public void createClassOpeningNotification(String title, String message, Long departmentId, Long periodId) {
        log.info("📢 Giáo vụ đang phát hành thông báo mở lớp mới: {}", title);

        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .departmentId(departmentId)
                .periodId(periodId)
                .build();

        notificationRepository.save(notification);
        log.info("✅ Đã phát hành thông báo thành công (ID: {})", notification.getId());
    }

    // =========================================================================
    // 2. DÀNH CHO SINH VIÊN: TỰ ĐỘNG BỐC THÔNG BÁO PHÙ HỢP THEO KHOA/NGÀNH
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        // Lấy thông tin tài khoản đang đăng nhập từ Token
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ!"));

        // Đi thẳng sang StudentProfile để lấy thông tin ngành/khoa học tập
        StudentProfile studentProfile = user.getStudentProfile();

        Long studentDepartmentId = null;
        // Nếu là sinh viên, ta bốc departmentId từ Major liên kết của sinh viên đó
        if (studentProfile != null && studentProfile.getMajor() != null) {
            studentDepartmentId = studentProfile.getMajor().getDepartment().getId();
        }

        log.info("🎓 Sinh viên [{}] thuộc Khoa ID [{}] đang tải bảng thông báo...", currentUsername, studentDepartmentId);

        List<Notification> entities = notificationRepository.findActiveNotificationsForStudent(studentDepartmentId);
        return entities.stream().map(n ->
                NotificationResponse.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .departmentId(n.getDepartmentId())
                        .periodId(n.getPeriodId())
                        .createdAt(n.getCreatedAt())
                        .build() // 🌟 Không dùng dấu chấm phẩy ở đây, tự hiểu là return
        ).toList();
    }
}


