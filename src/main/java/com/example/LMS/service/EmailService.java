package com.example.LMS.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // 🔥 Tiêm trực tiếp biến môi trường Email từ application.properties vào đây
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordEmail(String toEmail, String fullName, String username, String rawPassword) {
        log.info("📧 Bắt đầu tiến hành gửi mail mật khẩu ngầm cho: {}", toEmail);
        try {
            // 1. Kiểm tra biến cấu hình đầu vào
            if (fromEmail == null || fromEmail.isBlank()) {
                throw new IllegalArgumentException("Biến spring.mail.username đang bị trống! Hãy kiểm tra lại file .env");
            }

            // 2. 🔥 KHỬ ĐỘC CHUỖI (Xóa sạch dấu nháy kép, nháy đơn và khoảng trắng thừa từ file .env)
            String cleanFromEmail = fromEmail.replace("\"", "").replace("'", "").trim();
            String cleanToEmail = toEmail.trim();

            log.info("🔍 Địa chỉ email người gửi sau khi làm sạch: [{}]", cleanFromEmail);
            log.info("🔍 Địa chỉ email người nhận sau khi làm sạch: [{}]", cleanToEmail);

            // 3. Khởi tạo đối tượng tin nhắn text thuần túy
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(cleanFromEmail); // ✅ Đã bóc sạch dấu nháy, Parser sẽ không bao giờ lỗi nữa
            message.setTo(cleanToEmail);
            message.setSubject("🔑 Thông báo cấp tài khoản thành viên hệ thống LMS");

            // 4. Thiết lập nội dung bức thư
            String content = String.format(
                    "Xin chào %s,\n\n" +
                            "Tài khoản của bạn đã được khởi tạo thành công trên hệ thống LMS.\n" +
                            "Dưới đây là thông tin đăng nhập của bạn:\n" +
                            "- Tên đăng nhập (Username): %s\n" +
                            "- Mật khẩu tạm thời (Password): %s\n\n" +
                            "Vui lòng truy cập hệ thống và thực hiện ĐỔI MẬT KHẨU ngay trong lần đầu tiên đăng nhập để đảm bảo an toàn bảo mật.\n\n" +
                            "Trân trọng,\nBan Quản Trị Hệ Thống.",
                    fullName, username, rawPassword
            );

            message.setText(content);

            // 5. Bắn mail đi
            mailSender.send(message);
            log.info("✅ Đã gửi mail chứa mật khẩu thành công cho người dùng: {}", username);

        } catch (Exception e) {
            log.error("❌ Gửi mail thất bại cho {}. Lý do cụ thể: {}", toEmail, e.getMessage());
        }
    }
}