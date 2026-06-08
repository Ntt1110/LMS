package com.example.LMS.service.File;

import com.example.LMS.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    // Thư mục lưu trữ local (Sẽ tự động tạo folder 'uploads' ở thư mục gốc project)
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public LocalFileStorageServiceImpl() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // Làm sạch tên file và đính kèm UUID để chống trùng lặp tên/ghi đè file
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            if (originalFileName.contains("..")) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Tên file chứa ký tự không hợp lệ!");
            }
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn để Frontend có thể lấy file (Tí nữa sẽ mapping cái /uploads/ này)
            return "/uploads/" + uniqueFileName;
        } catch (IOException ex) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file. Vui lòng thử lại!");
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        // Logic xóa file trên ổ đĩa sẽ ráp vào sau khi làm chức năng Delete Bài học
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            // Tìm chính xác đường dẫn file trong thư mục uploads
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy hoặc không thể đọc file: " + fileName);
            }
        } catch (MalformedURLException ex) {
            // 🌟 ĐÃ SỬA: Chỉ truyền 2 tham số là HttpStatus và Message
            throw new CustomException(HttpStatus.NOT_FOUND, "Lỗi đường dẫn file: " + fileName);
        }
    }
}
