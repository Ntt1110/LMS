package com.example.LMS.service;

import com.example.LMS.dto.request.CreateExamRequestDto;
import com.example.LMS.dto.response.ExamAttemptResponseDto;
import com.example.LMS.dto.response.ExamPaperResponseDto;
import com.example.LMS.dto.response.ExamResultResponseDto;
import com.example.LMS.entity.Enum.AttemptStatus;
import com.example.LMS.entity.Enum.ExamStatus;
import com.example.LMS.dto.response.ExamResponseDto;
import com.example.LMS.entity.StudentExamAnswer;
import com.example.LMS.entity.model.*;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

     private final StudentExamAttemptRepository attemptRepository;
     private final UserRepository userRepository;

     private final StudentExamAnswerRepository answerRepository;

    private final EnrollmentRepository enrollmentRepository;
    private final ClassGradeRepository classGradeRepository;

    private final StudentExamAnswerRepository studentExamAnswerRepository;

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

        exam.setStatus(ExamStatus.OPEN);
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

        if (exam.getStatus() != ExamStatus.OPEN) {
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

    @Transactional
    public ExamAttemptResponseDto startExamAttempt(Long examId) {
        // 1. Lấy thông tin sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long studentId = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ!"))
                .getId();

        log.info("🚀 Sinh viên ID [{}] yêu cầu bắt đầu làm bài kiểm tra ID [{}]", studentId, examId);

        // 2. Kiểm tra tính hợp lệ của bài thi
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Bài kiểm tra không tồn tại!"));

        if (exam.getStatus() != ExamStatus.OPEN) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Bài kiểm tra chưa được mở để làm bài!");
        }

        // 3. 🔍 KIỂM TRA LỊCH SỬ LÀM BÀI CỦA SINH VIÊN (Tránh spam API)
        var existingAttemptOpt = attemptRepository.findByExamIdAndStudentId(examId, studentId);

        if (existingAttemptOpt.isPresent()) {
            StudentExamAttempt existingAttempt = existingAttemptOpt.get();

            // Nếu đã nộp bài (COMPLETED) hoặc bị ép nộp (FORCED) -> Chặn luôn
            if (existingAttempt.getStatus() == com.example.LMS.entity.Enum.AttemptStatus.COMPLETED ||
                    existingAttempt.getStatus() == com.example.LMS.entity.Enum.AttemptStatus.FORCED) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Bạn đã hoàn thành bài kiểm tra này rồi, không thể làm lại!");
            }

            // Nếu đang làm dở (IN_PROGRESS) do rớt mạng -> Trả về phiên làm bài cũ để tiếp tục tính giờ
            log.info("🔄 Sinh viên tiếp tục phiên làm bài dang dở (Attempt ID: {})", existingAttempt.getId());
            return ExamAttemptResponseDto.builder()
                    .attemptId(existingAttempt.getId())
                    .examId(exam.getId())
                    .startTime(existingAttempt.getStartTime())
                    .status(existingAttempt.getStatus().name())
                    .build();
        }

        // 4. Nếu hợp lệ và chưa làm bao giờ -> Tạo mới tinh
        StudentExamAttempt newAttempt = StudentExamAttempt.builder()
                .exam(exam)
                .studentId(studentId)
                .startTime(LocalDateTime.now()) // Bắt đầu tính giờ từ thời điểm này
                .status(com.example.LMS.entity.Enum.AttemptStatus.IN_PROGRESS)
                .build();

        StudentExamAttempt savedAttempt = attemptRepository.save(newAttempt);

        log.info("✅ Tạo thành công phiên làm bài mới (Attempt ID: {})", savedAttempt.getId());

        return ExamAttemptResponseDto.builder()
                .attemptId(savedAttempt.getId())
                .examId(exam.getId())
                .startTime(savedAttempt.getStartTime())
                .status(savedAttempt.getStatus().name())
                .build();
    }

    // TẢI ĐỀ THI MÙ CHO SINH VIÊN (Chỉ cho phép khi đang IN_PROGRESS)
    @Transactional(readOnly = true)
    public ExamPaperResponseDto getExamPaperForStudent(Long examId) {
        // 1. Lấy thông tin User
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long studentId = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ!"))
                .getId();

        log.info("🎓 Sinh viên ID [{}] đang yêu cầu tải đề thi mù ID: {}", studentId, examId);

        // 2. Kiểm tra bài kiểm tra
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Bài kiểm tra không tồn tại!"));

        if (exam.getStatus() != ExamStatus.OPEN) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bài kiểm tra này chưa được xuất bản!");
        }

        // 3. 🛡️ LÁ CHẮN BẢO MẬT: Kiểm tra phiên làm bài (Attempt)
        StudentExamAttempt attempt = attemptRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new CustomException(HttpStatus.FORBIDDEN, "Bạn chưa bắt đầu phiên làm bài. Vui lòng bấm Bắt đầu trước!"));

        if (attempt.getStatus() != com.example.LMS.entity.Enum.AttemptStatus.IN_PROGRESS) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Phiên làm bài của bạn đã kết thúc, không thể xem lại đề thi lúc này!");
        }

        // 4. Kéo toàn bộ câu hỏi của đề thi này lên
        List<ExamQuestion> questions = questionRepository.findByExamIdOrderByOrderIndexAsc(examId);

        // 5. Map sang DTO an toàn (Bỏ isCorrect)
        List<ExamPaperResponseDto.ExamQuestionDto> questionDtos = questions.stream().map(q -> {

            // Kéo đáp án của từng câu hỏi
            List<QuestionOption> options = optionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());

            // Map đáp án sang DTO
            List<ExamPaperResponseDto.ExamOptionDto> optionDtos = options.stream().map(opt ->
                    ExamPaperResponseDto.ExamOptionDto.builder()
                            .optionId(opt.getId())
                            .content(opt.getContent())
                            .build()
            ).collect(Collectors.toList());

            return ExamPaperResponseDto.ExamQuestionDto.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .options(optionDtos) // Nhét mảng đáp án vào câu hỏi
                    .build();

        }).collect(Collectors.toList());

        // 6. Trả về toàn bộ vỏ đề thi
        log.info("✅ Kéo đề thi thành công! Tổng số câu hỏi: {}", questions.size());
        return ExamPaperResponseDto.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .timeLimit(exam.getTimeLimit())
                .totalQuestions(exam.getTotalQuestions())
                .questions(questionDtos)
                .build();
    }

    // =========================================================================
    @Transactional
    public ExamResultResponseDto submitExamAttempt(Long attemptId, boolean acceptIncomplete) {
        StudentExamAttempt attempt = getValidatedAttempt(attemptId);

        // 1. 🔍 KIỂM TRA SỐ CÂU CHƯA HOÀN THÀNH
        int totalQuestions = attempt.getExam().getTotalQuestions();

        // Vét số lượng câu sinh viên ĐÃ LÀM nháp trong DB
        int answeredCount = answerRepository.findByAttemptId(attemptId).size();
        int unansweredCount = totalQuestions - answeredCount;

        // 2. Nếu còn câu chưa làm VÀ sinh viên chưa bấm xác nhận "Nộp bất chấp"
        if (unansweredCount > 0 && !acceptIncomplete) {
            log.warn("⚠️ Sinh viên còn {} câu chưa làm. Thả xích gửi thông báo yêu cầu xác nhận!", unansweredCount);

            // Ném Exception kèm mã lỗi và số câu chưa làm để Frontend Vue 3 bắt được bắt Popup
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "LEAVE_BLANK_WARNING:" + unansweredCount
            );
        }

        // 3. Nếu mọi thứ OK hoặc sinh viên đã bấm "Vẫn nộp" -> Tiến hành chấm điểm
        return processGrading(attempt, AttemptStatus.COMPLETED);
    }

    @Transactional
    public ExamResultResponseDto forceSubmitExamAttempt(Long attemptId) {
        StudentExamAttempt attempt = getValidatedAttempt(attemptId);
        log.warn("⏰  báo hết giờ! Hệ thống ép nộp bài cho Attempt ID: {}", attemptId);
        return processGrading(attempt, AttemptStatus.FORCED); // Chốt trạng thái bị ép nộp
    }
    private ExamResultResponseDto processGrading(StudentExamAttempt attempt, AttemptStatus targetStatus) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Bài thi này đã kết thúc hoặc đã được nộp trước đó!");
        }

        Exam exam = attempt.getExam();
        int totalQuestions = exam.getTotalQuestions();
        if (totalQuestions == 0) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Đề thi bị lỗi: Không có câu hỏi nào!");
        }

        // Vét sạch đáp án nháp của sinh viên trong DB
        List<StudentExamAnswer> studentAnswers = answerRepository.findByAttemptId(attempt.getId());

        int correctCount = 0;
        for (StudentExamAnswer answer : studentAnswers) {
            if (answer.getSelectedOption().getIsCorrect()) {
                correctCount++;
            }
        }

        // Tính điểm thang 10
        double rawScore = ((double) correctCount / (double) totalQuestions) * 10.0;
        double finalScore = Math.round(rawScore * 100.0) / 100.0;

        // Cập nhật record phiên làm bài
        attempt.setStatus(targetStatus); // Lưu COMPLETED hoặc FORCED tùy luồng gọi
        attempt.setSubmitTime(LocalDateTime.now());
        attempt.setScore(finalScore);
        attemptRepository.save(attempt);

        log.info("📊 [SCORE CALCULATED] Điểm bài thi trắc nghiệm: {}", finalScore);

        // 3. 🌟 LUỒNG ĐỒNG BỘ ĐIỂM SANG BẢNG CLASS_GRADE (CHỈ LƯU, KHÔNG TÍNH TỔNG KẾT)
        try {
            Long studentId = attempt.getStudentId();
            Long classId = exam.getClassEntity().getId(); // ID lớp học phần nối từ đề thi

            // Tìm thông tin đăng ký học của sinh viên trong lớp này
            ClassEnrollment enrollment = enrollmentRepository.findByStudentIdAndClassEntityId(studentId, classId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin học phần của sinh viên!"));

            // Tìm hoặc khởi tạo mới bản ghi sổ điểm (ClassGrade) cho sinh viên
            ClassGrade classGrade = classGradeRepository.findByEnrollmentId(enrollment.getId())
                    .orElseGet(() -> ClassGrade.builder()
                            .enrollment(enrollment)
                            .status(ClassGrade.GradeStatus.PENDING) //
                            .build());

            // Đổ điểm trực tiếp vào đúng cột được cấu hình từ thuộc tính ExamType
            if (exam.getExamType() != null) {
                switch (exam.getExamType()) {
                    case REGULAR -> {
                        // 🔍 Dò xem cột Regular 1 trống thì điền, không thì đẩy sang Regular 2
                        if (classGrade.getRegularScore1() == null) { //
                            classGrade.setRegularScore1(finalScore); //
                            log.info("📝 [GRADE SYNC] Cột Regular 1 đang trống. Đã điền điểm: {}", finalScore);
                        } else if (classGrade.getRegularScore2() == null) { //
                            classGrade.setRegularScore2(finalScore); //
                            log.info("📝 [GRADE SYNC] Cột Regular 1 đã có điểm. Tự động chuyển sang điền Regular 2: {}", finalScore);
                        } else {
                            // Trường hợp sinh viên làm đến bài kiểm tra nhỏ thứ 3 (Cả 2 cột đều đầy)
                            log.warn("⚠️ [GRADE SYNC] Cả 2 cột điểm Regular đều đã đầy! Bỏ qua ghi đè bài thi này.");
                        }
                    }
                    case MIDTERM -> {
                        classGrade.setMidtermScore(finalScore); //
                        log.info("📝 [GRADE SYNC] Đã điền điểm Giữa kỳ: {}", finalScore);
                    }
                    case FINAL -> {
                        classGrade.setFinalScore(finalScore); //
                        log.info("📝 [GRADE SYNC] Đã điền điểm Cuối kỳ: {}", finalScore);
                    }
                }

                calculateAndUpdateTotalScore(classGrade);

                classGrade.setUpdatedAt(LocalDateTime.now());
                classGradeRepository.save(classGrade);

                log.info("🔄 [GRADE SYNC SUCCESS] Đã lưu điểm {} vào sổ điểm học phần thành công!", finalScore);
            }

        } catch (Exception e) {
            // Bao bọc try-catch để nếu có lỗi đồng bộ điểm, sinh viên vẫn nộp bài thi thành công
            log.error("🚨 [GRADE SYNC ERROR] Lỗi phát sinh khi đẩy điểm sang bảng ClassGrade: {}", e.getMessage());
        }


        return ExamResultResponseDto.builder()
                .attemptId(attempt.getId())
                .score(finalScore)
                .correctAnswers(correctCount)
                .totalQuestions(totalQuestions)
                .submitTime(attempt.getSubmitTime())
                .status(attempt.getStatus().name())
                .build();
    }
    private StudentExamAttempt getValidatedAttempt(Long attemptId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long studentId = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ!"))
                .getId();

        StudentExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên làm bài!"));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên bài thi của người khác!");
        }
        return attempt;
    }
    @Transactional
    public void forceSubmitExamAttemptFromScheduler(Long attemptId) {
        StudentExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên làm bài!"));
        processGrading(attempt, AttemptStatus.FORCED);
    }

    @Transactional
    public void saveStudentAnswer(Long attemptId, com.example.LMS.dto.request.SaveAnswerRequestDto dto) {
        // 1. Lấy thông tin sinh viên đang đăng nhập từ Token
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long studentId = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ!"))
                .getId();

        // 2. Kiểm tra phiên làm bài có tồn tại không
        StudentExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên làm bài!"));

        // Security: Chặn trường hợp sinh viên A hack truyền nhầm attemptId của sinh viên B
        if (!attempt.getStudentId().equals(studentId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên bài thi này!");
        }

        // Kiểm tra xem bài thi còn trong thời gian làm hay đã nộp/bị ép nộp rồi
        if (attempt.getStatus() != com.example.LMS.entity.Enum.AttemptStatus.IN_PROGRESS) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Bài thi đã kết thúc, không thể lưu thêm đáp án!");
        }

        // 3. Tìm thực thể Câu hỏi và Đáp án được chọn tương ứng
        ExamQuestion question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Câu hỏi không tồn tại!"));

        QuestionOption selectedOption = optionRepository.findById(dto.getSelectedOptionId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Đáp án lựa chọn không hợp lệ!"));

        // 4. 🔍 THUẬT TOÁN UPSERT: Kiểm tra xem câu này đã từng tích chọn chưa
        var existingAnswerOpt = studentExamAnswerRepository.findByAttemptIdAndQuestionId(attemptId, dto.getQuestionId());

        if (existingAnswerOpt.isPresent()) {
            // Sinh viên ĐỔI ĐÁP ÁN -> Chạy lệnh UPDATE đè lên dòng cũ
            StudentExamAnswer existingAnswer = existingAnswerOpt.get();
            existingAnswer.setSelectedOption(selectedOption);
            studentExamAnswerRepository.save(existingAnswer);
            log.info("🔄 [Auto-Save UPDATE] Sinh viên [{}] đổi đáp án câu [{}], chọn Option ID: {}",
                    studentId, dto.getQuestionId(), dto.getSelectedOptionId());
        } else {
            // Sinh viên LẦN ĐẦU CHỌN CÂU NÀY -> Chạy lệnh INSERT mới
            StudentExamAnswer newAnswer = StudentExamAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selectedOption)
                    .build();
            studentExamAnswerRepository.save(newAnswer);
            log.info("✅ [Auto-Save INSERT] Sinh viên [{}] tích câu mới [{}], chọn Option ID: {}",
                    studentId, dto.getQuestionId(), dto.getSelectedOptionId());
        }
    }

    private void calculateAndUpdateTotalScore(ClassGrade classGrade) {
        // Chỉ tiến hành tính điểm tổng kết khi SV ĐÃ CÓ ĐỦ cả 4 cột điểm quan trọng
        if (classGrade.getRegularScore1() != null &&
                classGrade.getRegularScore2() != null &&
                classGrade.getMidtermScore() != null &&
                classGrade.getFinalScore() != null) {

            log.info("📊 [TOTAL SCORE CALCULATING] Đang tính điểm tổng kết học phần cho sổ điểm ID: {}", classGrade.getId());

            // Công thức trọng số theo yêu cầu: 10% - 10% - 30% - 50%
            double r1 = classGrade.getRegularScore1() * 0.1;
            double r2 = classGrade.getRegularScore2() * 0.1;
            double mid = classGrade.getMidtermScore() * 0.3;
            double fin = classGrade.getFinalScore() * 0.5;

            double rawTotal = r1 + r2 + mid + fin;

            // Làm tròn lấy 2 chữ số thập phân (Ví dụ: 7.456 -> 7.46)
            double finalTotalScore = Math.round(rawTotal * 100.0) / 100.0;
            classGrade.setTotalScore(finalTotalScore);

            // Xét trạng thái PASS/FAIL dựa trên điểm tổng kết hệ 10 (Chuẩn tín chỉ là >= 4.0)
            if (finalTotalScore >= 4.0) {
                classGrade.setStatus(ClassGrade.GradeStatus.PASS);
                log.info("🎉 Kết quả học phần: PASS ({})", finalTotalScore);
            } else {
                classGrade.setStatus(ClassGrade.GradeStatus.FAIL);
                log.warn("❌ Kết quả học phần: FAIL ({})", finalTotalScore);
            }
        } else {
            // Nếu chưa đủ cột điểm (Ví dụ: Mới thi giữa kỳ, chưa thi cuối kỳ), giữ nguyên PENDING
            classGrade.setStatus(ClassGrade.GradeStatus.PENDING);
            log.info("ℹ️ [TOTAL SCORE] Chưa đủ cả 4 cột điểm để tính điểm tổng kết. Trạng thái giữ nguyên PENDING.");
        }
    }
}