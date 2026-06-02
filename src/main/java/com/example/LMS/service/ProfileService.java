package com.example.LMS.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.UserProfile;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.UserProfileRepository;
import com.example.LMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final Cloudinary cloudinary;

    @Transactional
    public String uploadAvatar(MultipartFile file) {
        // 1. Kiểm tra file trống
        if (file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Vui lòng chọn một file ảnh để upload!");
        }

        // 2. Kiểm tra dung lượng file (Ví dụ chặn > 2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Kích thước ảnh quá lớn! Vui lòng upload file dưới 2MB.");
        }

        // 3. Kiểm tra định dạng file (Chỉ cho phép jpg, jpeg, png)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Định dạng file không hợp lệ! Chỉ chấp nhận ảnh PNG hoặc JPEG.");
        }

        // 4. Lấy thông tin user đang đăng nhập từ Token
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người dùng!"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Hồ sơ người dùng không tồn tại!"));

        try {
            log.info("🚀 Đang tiến hành upload file lên Cloudinary cho user: {}", currentUsername);

            // 5. Đẩy file lên Cloudinary vào thư mục "lms_avatars", lấy tên của user làm tên file để ghi đè nếu upload lại
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "lms_avatars",
                    "public_id", "avatar_" + user.getUsername(),
                    "overwrite", true
            ));

            // 6. Trích xuất đường dẫn URL an toàn trả về từ Cloudinary
            String avatarUrl = (String) uploadResult.get("secure_url");

            // 7. Cập nhật vào DB
            profile.setAvatarUrl(avatarUrl);
            userProfileRepository.save(profile);

            log.info("✅ Cập nhật avatar thành công. URL: {}", avatarUrl);
            return avatarUrl;

        } catch (Exception e) {
            log.error("❌ Lỗi xảy ra trong quá trình upload ảnh: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể upload ảnh lên hệ thống, vui lòng thử lại sau!");
        }
    }
}
