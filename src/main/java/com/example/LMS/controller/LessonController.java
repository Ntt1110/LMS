package com.example.LMS.controller;


import com.example.LMS.dto.request.CreateLessonRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.LessonResponseDto;
import com.example.LMS.entity.model.LessonMaterial;
import com.example.LMS.service.File.FileStorageService;
import com.example.LMS.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class LessonController {

    private final LessonService lessonService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{classId}/lessons")
    @PreAuthorize("hasAuthority('LESSON_VIEW')") // Giảng viên, sinh viên có quyền xem lớp đều gọi được
    @Operation(summary = "Lấy danh sách bài học của một lớp học phần cụ thể (Xem theo từng lớp)")
    public ApiResponse<List<LessonResponseDto>> getLessonsByClass(@PathVariable Long classId) {

        List<LessonResponseDto> lessons = lessonService.getLessonsByClass(classId);

        return ApiResponse.<List<LessonResponseDto>>builder()
                .code(200)
                .message("Tải danh sách bài học của lớp học phần thành công!")
                .data(lessons)
                .build();
    }

    @PostMapping(value = "/classes/{classId}/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('LESSON_CREATE')")
    @Operation(summary = "Tạo mới một bài học kèm file tài liệu (Hỗ trợ multipart/form-data)")
    public ApiResponse<String> createLesson(
            @PathVariable Long classId,
            @Valid @ModelAttribute CreateLessonRequestDto dto) {

        lessonService.createLessonWithFiles(classId, dto);

        return ApiResponse.<String>builder()
                .code(201)
                .message("Tạo bài học mới và đính kèm tài liệu thành công!")
                .data("CREATED")
                .build();
    }

    // 🌟 API 2: LẤY DANH SÁCH BÀI GIẢNG CHO SINH VIÊN (Chỉ xem bài đã Xuất bản + File đính kèm)

    @GetMapping("/classes/{classId}/lessons/published")
    @PreAuthorize("hasAuthority('LESSON_VIEW')") // 🛡️ Sinh viên có quyền xem lớp đều gọi được
    @Operation(summary = "Danh sách bài giảng ĐÃ XUẤT BẢN của lớp (Dành cho Sinh viên học tập)")
    public ApiResponse<List<LessonResponseDto>> getPublishedLessons(@PathVariable Long classId) {
        log.info("📡 API: Sinh viên yêu cầu tải bài học đã xuất bản của lớp ID: {}", classId);

        List<LessonResponseDto> lessons = lessonService.getPublishedLessonsForStudent(classId);

        return ApiResponse.<List<LessonResponseDto>>builder()
                .code(200)
                .message("Tải danh sách bài học công khai thành công!")
                .data(lessons)
                .build();
    }
    @GetMapping("/materials/{materialId}/download")
    @PreAuthorize("hasAuthority('LESSON_VIEW')") // 🛡️ Gác cổng: Phải có quyền xem mới được tải
    @Operation(summary = "Tải xuống file tài liệu đính kèm của bài học")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId) {

        // 1. Lấy thông tin tài liệu từ DB
        LessonMaterial material = lessonService.getMaterialById(materialId);

        // 2. Tách lấy tên file vật lý đã lưu từ fileUrl (Ví dụ từ "/uploads/uuid_ten.pdf" -> lấy "uuid_ten.pdf")
        String fileUrl = material.getFileUrl();
        String savedFileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        // 3. Chuyển file vật lý thành dạng Resource
        // (Lưu ý: Ông nhớ Inject thằng fileStorageService vào Controller nhé: private final FileStorageService fileStorageService;)
        Resource resource = fileStorageService.loadFileAsResource(savedFileName);

        // 4. Nếu database không lưu fileType, mặc định dùng "application/octet-stream" để ép tải xuống
        String contentType = material.getFileType() != null ? material.getFileType() : "application/octet-stream";

        // 5. Trả file về cho Frontend với Header ép tải (attachment)
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getFileName() + "\"")
                .body(resource);
    }
}