package com.example.LMS.service;

import com.example.LMS.dto.request.CreateExamRequestDto;
import com.example.LMS.entity.Enum.ExamStatus;
import com.example.LMS.dto.response.ExamResponseDto;
import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.Exam;
import com.example.LMS.entity.model.ExamQuestion;
import com.example.LMS.entity.model.QuestionOption;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassEntityRepository;
import com.example.LMS.repository.ExamQuestionRepository;
import com.example.LMS.repository.ExamRepository;
import com.example.LMS.repository.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final ClassEntityRepository classRepository;
     private final ExamQuestionRepository questionRepository;
 private final QuestionOptionRepository optionRepository;

    // =========================================================================
    // 🌟 1. LUỒNG GIẢNG VIÊN: XEM TẤT CẢ BÀI KIỂM TRA
    // =========================================================================
    @Transactional(readOnly = true)
    public List<ExamResponseDto> getAllExamsByClass(Long classId) {
        log.info("📝 Đang tải toàn bộ danh sách bài kiểm tra của lớp ID: {}", classId);

        if (!classRepository.existsById(classId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Không thể tải danh sách! Lớp học phần không tồn tại.");
        }

        List<Exam> exams = examRepository.findAllExamsByClassId(classId);
        return mapToDtoList(exams);
    }

    // =========================================================================
    // 🌟 2. LUỒNG SINH VIÊN: CHỈ XEM BÀI ĐÃ XUẤT BẢN HOẶC ĐÃ ĐÓNG
    // =========================================================================
    @Transactional(readOnly = true)
    public List<ExamResponseDto> getActiveExamsForStudent(Long classId) {
        log.info("🎓 Sinh viên đang tải danh sách bài kiểm tra khả dụng của lớp ID: {}", classId);

        if (!classRepository.existsById(classId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Lớp học phần không tồn tại!");
        }

        List<Exam> exams = examRepository.findActiveExamsForStudent(classId);
        return mapToDtoList(exams);
    }

    // --- Hàm phụ trợ map Entity sang DTO ---
    private List<ExamResponseDto> mapToDtoList(List<Exam> exams) {
        return exams.stream().map(exam -> ExamResponseDto.builder()
                .id(exam.getId())
                .classId(exam.getClassEntity().getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .examType(exam.getExamType())
                .timeLimit(exam.getTimeLimit())
                .totalQuestions(exam.getTotalQuestions())
                .status(exam.getStatus())
                .createdAt(exam.getCreatedAt())
                .deletedAt(exam.getDeletedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void createExamWithQuestions(Long classId, CreateExamRequestDto dto) {
        log.info("⚡ Tiến hành tạo bài kiểm tra mới cho lớp ID: {}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Lớp học phần không tồn tại!"));

        // 1. LƯU VỎ BÀI KIỂM TRA (EXAM) TRƯỚC
        int totalQuestions = (dto.getQuestions() != null) ? dto.getQuestions().size() : 0;

        Exam newExam = Exam.builder()
                .classEntity(classEntity)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .examType(dto.getExamType())
                .timeLimit(dto.getTimeLimit())
                .totalQuestions(totalQuestions) // Tự động tính tổng số câu hỏi từ mảng gửi lên
                .status(com.example.LMS.entity.Enum.ExamStatus.CREATED) // Mặc định là Nháp
                .build();

        Exam savedExam = examRepository.save(newExam);

        // 2. NẾU CÓ MẢNG CÂU HỎI -> BẮT ĐẦU VÒNG LẶP LƯU CÂU HỎI VÀ ĐÁP ÁN
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            for (CreateExamRequestDto.QuestionDto qDto : dto.getQuestions()) {

                // LƯU CÂU HỎI
                ExamQuestion question = ExamQuestion.builder()
                        .exam(savedExam)
                        .content(qDto.getContent())
                        .orderIndex(qDto.getOrderIndex())
                        .build();
                ExamQuestion savedQuestion = questionRepository.save(question);

                // LƯU ĐÁP ÁN CHO CÂU HỎI ĐÓ
                if (qDto.getOptions() != null && !qDto.getOptions().isEmpty()) {
                    for (CreateExamRequestDto.OptionDto optDto : qDto.getOptions()) {
                        QuestionOption option = QuestionOption.builder()
                                .question(savedQuestion)
                                .content(optDto.getContent())
                                .isCorrect(optDto.getIsCorrect())
                                .orderIndex(optDto.getOrderIndex())
                                .build();
                        optionRepository.save(option);
                    }
                }
            }
        }
        log.info("✅ Hoàn tất tạo bài kiểm tra: {} với {} câu hỏi.", savedExam.getTitle(), totalQuestions);
    }
    // =========================================================================
    // MO BAI KIEM TRA: DRAFT -> PUBLISHED
    // =========================================================================
    @Transactional
    public ExamResponseDto openExam(Long classId, Long examId) {
        log.info("Mo bai kiem tra ID: {} cua lop ID: {}", examId, classId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Khong tim thay bai kiem tra!"));

        if (!exam.getClassEntity().getId().equals(classId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Bai kiem tra khong thuoc lop nay!");
        }

        if (exam.getStatus() != ExamStatus.CREATED) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Chi co the mo bai kiem tra dang o trang thai DRAFT! Trang thai hien tai: " + exam.getStatus());
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        Exam saved = examRepository.save(exam);
        log.info("Da mo bai kiem tra: {}", saved.getTitle());

        return toDto(saved);
    }

    // =========================================================================
    // DONG BAI KIEM TRA: PUBLISHED -> CLOSED
    // =========================================================================
    @Transactional
    public ExamResponseDto closeExam(Long classId, Long examId) {
        log.info("Dong bai kiem tra ID: {} cua lop ID: {}", examId, classId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Khong tim thay bai kiem tra!"));

        if (!exam.getClassEntity().getId().equals(classId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Bai kiem tra khong thuoc lop nay!");
        }

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Chi co the dong bai kiem tra dang o trang thai PUBLISHED! Trang thai hien tai: " + exam.getStatus());
        }

        exam.setStatus(ExamStatus.CLOSED);
        Exam saved = examRepository.save(exam);
        log.info("Da dong bai kiem tra: {}", saved.getTitle());

        return toDto(saved);
    }

    // Helper map 1 entity sang DTO
    private ExamResponseDto toDto(Exam exam) {
        return ExamResponseDto.builder()
                .id(exam.getId())
                .classId(exam.getClassEntity().getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .examType(exam.getExamType())
                .timeLimit(exam.getTimeLimit())
                .totalQuestions(exam.getTotalQuestions())
                .status(exam.getStatus())
                .createdAt(exam.getCreatedAt())
                .deletedAt(exam.getDeletedAt())
                .build();
    }
}