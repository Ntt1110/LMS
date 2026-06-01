package com.example.LMS.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =================================================================================
    // 1. Xử lý CustomException do chính chúng ta chủ động ném ra
    // (VD: Sai mật khẩu, Không tìm thấy User, Email đã tồn tại...)
    // =================================================================================
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", ex.getStatus().value());
        errorResponse.put("error", ex.getStatus().getReasonPhrase());
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    // =================================================================================
    // 2. Xử lý lỗi Validate dữ liệu đầu vào (VD: @NotBlank, @Email gửi từ Frontend bị sai)
    // =================================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Gom tất cả các trường bị lỗi và tin nhắn lỗi tương ứng
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Error");
        errorResponse.put("message", errors); // Trả về danh sách chi tiết các field bị lỗi

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // =================================================================================
    // 3. Trạm gác cuối cùng: Bắt toàn bộ các lỗi chưa lường trước được (Lỗi 500)
    // (VD: NullPointerException, đứt cáp Database...)
    // =================================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");

        // Lưu ý: Trong môi trường Production thực tế, không nên in ex.getMessage() ra ngoài để bảo mật.
        // Nhưng lúc Dev thì cứ in ra để dễ debug.
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}