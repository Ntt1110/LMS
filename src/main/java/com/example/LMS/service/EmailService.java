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

    @Async
    public void sendResetPasswordEmail(String toEmail, String token) {
        log.info("📧 Bắt đầu tiến hành gửi mail phục hồi mật khẩu ngầm cho: {}", toEmail);
        try {
            if (fromEmail == null || fromEmail.isBlank()) {
                throw new IllegalArgumentException("Biến spring.mail.username đang bị trống!");
            }

            // Làm sạch email giống hàm trên
            String cleanFromEmail = fromEmail.replace("\"", "").replace("'", "").trim();
            String cleanToEmail = toEmail.trim();

            // Cấu hình đường dẫn Link Reset trỏ về giao diện Vue 3 của ông
            // Sau này lên production ông chỉ cần thay localhost thành domain thật là xong
            String resetLink = "http://localhost:5173/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(cleanFromEmail);
            message.setTo(cleanToEmail);
            message.setSubject("🔑 [LMS] Yêu cầu đặt lại mật khẩu tài khoản");

            // Thiết lập nội dung hướng dẫn sinh viên/giảng viên click đổi pass
            String content = String.format(
                    "Xin chào thành viên,\n\n" +
                            "Hệ thống LMS đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với Email này.\n" +
                            "Vui lòng click vào đường dẫn dưới đây để tiến hành thiết lập mật khẩu mới:\n" +
                            "%s\n\n" +
                            "⏰ Lưu ý: Đường dẫn này chỉ có hiệu lực trong vòng 15 phút.\n" +
                            "Nếu bạn không đưa ra yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với Giáo vụ để được hỗ trợ.\n\n" +
                            "Trân trọng,\nBan Quản Trị Hệ Thống.",
                    resetLink
            );

            message.setText(content);

            // Bắn mail đi
            mailSender.send(message);
            log.info("✅ Đã gửi mail chứa link reset mật khẩu thành công tới: {}", cleanToEmail);

        } catch (Exception e) {
            log.error("❌ Gửi mail phục hồi mật khẩu thất bại cho {}. Lý do: {}", toEmail, e.getMessage());
        }
    }


}