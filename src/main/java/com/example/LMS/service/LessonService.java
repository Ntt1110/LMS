package com.example.LMS.service;

import com.example.LMS.dto.request.CreateLessonRequestDto;
import com.example.LMS.dto.response.LessonMaterialResponseDto;
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

        // Duyệt từng bài học và nhét file vào
        return lessons.stream().map(lesson -> {

            // 🔍 Tìm toàn bộ file thuộc bài học này
            List<LessonMaterial> materials = materialRepository.findByLessonIdAndDeletedAtIsNull(lesson.getId());

            // Ép kiểu File Entity sang File DTO
            List<LessonMaterialResponseDto> materialDtos = materials.stream().map(mat ->
                    LessonMaterialResponseDto.builder()
                            .id(mat.getId())
                            .fileName(mat.getFileName())
                            .fileUrl(mat.getFileUrl())
                            .fileType(mat.getFileType())
                            .fileSize(mat.getFileSize())
                            .build()
            ).collect(Collectors.toList());

            // Đóng gói tất cả trả về
            return LessonResponseDto.builder()
                    .id(lesson.getId())
                    .classId(lesson.getClassEntity().getId())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .orderIndex(lesson.getOrderIndex())
                    .isPublished(lesson.getIsPublished())
                    .createdAt(lesson.getCreatedAt())
                    .materials(materialDtos) // 🌟 Gắn mảng danh sách file vào đây
                    .build();
        }).collect(Collectors.toList());
    }
    // =========================================================================
    // 2. DÀNH CHO SINH VIÊN (Chỉ xem bài Published KÈM FILE)
    // =========================================================================
    @Transactional(readOnly = true)
    public List<LessonResponseDto> getPublishedLessonsForStudent(Long classId) {
        log.info("🎓 Sinh viên đang tải danh sách bài học (Đã xuất bản) kèm file của lớp ID: {}", classId);

        if (!classRepository.existsById(classId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Lớp học phần không tồn tại!");
        }

        // Lọc bài học có isPublished = true
        List<Lesson> lessons = lessonRepository.findPublishedLessonsByClassId(classId);

        return lessons.stream().map(lesson -> {

            // 🔍 Tìm toàn bộ file thuộc bài học này
            List<LessonMaterial> materials = materialRepository.findByLessonIdAndDeletedAtIsNull(lesson.getId());

            List<LessonMaterialResponseDto> materialDtos = materials.stream().map(mat ->
                    LessonMaterialResponseDto.builder()
                            .id(mat.getId())
                            .fileName(mat.getFileName())
                            .fileUrl(mat.getFileUrl())
                            .fileType(mat.getFileType())
                            .fileSize(mat.getFileSize())
                            .build()
            ).collect(Collectors.toList());

            return LessonResponseDto.builder()
                    .id(lesson.getId())
                    .classId(lesson.getClassEntity().getId())
                    .title(lesson.getTitle())
                    .description(lesson.getDescription())
                    .orderIndex(lesson.getOrderIndex())
                    .isPublished(lesson.getIsPublished())
                    .createdAt(lesson.getCreatedAt())
                    .materials(materialDtos)
                    .build();
        }).collect(Collectors.toList());
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

    // 🌟 HÀM PHỤC VỤ DOWNLOAD: Lấy thông tin chi tiết của 1 File đính kèm
    @Transactional(readOnly = true)
    public LessonMaterial getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Tài liệu đính kèm không tồn tại!"));
    }
}