package com.example.LMS.service;

import com.example.LMS.dto.request.CreateLessonRequestDto;
import com.example.LMS.dto.response.LessonResponseDto;
import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.Lesson;
import com.example.LMS.entity.model.LessonMaterial;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassEntityRepository;
import com.example.LMS.repository.LessonMaterialRepository;
import com.example.LMS.repository.LessonRepository;
import com.example.LMS.service.File.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ClassEntityRepository classRepository; // Dùng để xác thực sự tồn tại của lớp
    private final LessonMaterialRepository materialRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<LessonResponseDto> getLessonsByClass(Long classId) {
        log.info("📚 Đang thực hiện bốc danh sách bài học trực thuộc lớp ID: {}", classId);

        // 1. Kiểm tra an toàn xem lớp học phần có tồn tại không
        if (!classRepository.existsById(classId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Không thể tải bài học! Lớp học phần được chọn không tồn tại.");
        }

        // 2. Kéo loạt bài học từ Repository lên
        List<Lesson> lessons = lessonRepository.findByClassEntityIdAndDeletedAtIsNullOrderByOrderIndexAsc(classId);

        // 3. Map danh sách sang DTO để dọn đường cho Frontend Vue 3 render
        return lessons.stream().map(lesson -> LessonResponseDto.builder()
                .id(lesson.getId())
                .classId(lesson.getClassEntity().getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .orderIndex(lesson.getOrderIndex())
                .isPublished(lesson.getIsPublished())
                .createdAt(lesson.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void createLessonWithFiles(Long classId, CreateLessonRequestDto dto) {
        log.info("⚡ Tạo bài học mới cho lớp ID: {}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Lớp học phần không tồn tại!"));

        // 1. Khởi tạo và Lưu Lesson xuống DB TRƯỚC để sinh ra cái ID tự tăng
        Lesson newLesson = Lesson.builder()
                .classEntity(classEntity)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0)
                .isPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false)
                .build();

        Lesson savedLesson = lessonRepository.save(newLesson);

        // 2. Xử lý vòng lặp lưu danh sách File vào LessonMaterial
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (MultipartFile file : dto.getFiles()) {
                if (!file.isEmpty()) {
                    // FileStorage lưu xuống ổ đĩa và nhả về URL
                    String savedFileUrl = fileStorageService.storeFile(file);

                    // Đóng gói thông tin ném xuống bảng lesson_materials
                    LessonMaterial material = LessonMaterial.builder()
                            .lesson(savedLesson) // Nối khóa ngoại về cái Lesson vừa tạo
                            .fileName(file.getOriginalFilename())
                            .fileUrl(savedFileUrl)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();
                    materialRepository.save(material);
                }
            }
            log.info("📁 Đã đính kèm {} tài liệu thành công.", dto.getFiles().size());
        }

        log.info("✅ Hoàn tất tạo bài học: {}", dto.getTitle());
    }
}