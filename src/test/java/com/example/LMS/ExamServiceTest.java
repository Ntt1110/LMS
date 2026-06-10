package com.example.LMS;






import com.example.LMS.entity.Enum.AttemptStatus;
import com.example.LMS.entity.Enum.ExamType;
import com.example.LMS.entity.StudentExamAnswer;
import com.example.LMS.entity.model.*;
import com.example.LMS.repository.*;
import com.example.LMS.service.ExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @InjectMocks
    private ExamService examService;

    @Mock
    private StudentExamAttemptRepository attemptRepository;

    @Mock
    private StudentExamAnswerRepository answerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassGradeRepository classGradeRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User mockUser;
    private ClassEnrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("student_test");

        mockUser = User.builder().id(1L).username("student_test").build();

        // Tạo lượt đăng ký lớp giả lập
        mockEnrollment = ClassEnrollment.builder().id(50L).build();
    }

    @Test
    @DisplayName("Đồng bộ điểm REGULAR lần 1 - Cột 1 trống thì điểm phải vào cột 1")
    void processGrading_RegularExam_FillsRegularScore1() {
        System.out.println("\n=== 🧪 RUNNING TEST CASE: Dò điểm REGULAR lần 1 ===");

        // Thiết lập đề thi hệ REGULAR có 2 câu hỏi
        Exam mockExam = new Exam();
        mockExam.setId(10L);
        mockExam.setTotalQuestions(2);
        mockExam.setExamType(ExamType.REGULAR);
        // Giả lập lấy ID lớp học (sửa lại tùy thuộc vào hàm trong Entity của ông, ví dụ getId() từ ClassEntity)
        // Ở đây ta mock hành vi trả về ID lớp học là 20L
        mockExam.setClassEntity(com.example.LMS.entity.model.ClassEntity.builder().id(20L).build());

        StudentExamAttempt mockAttempt = StudentExamAttempt.builder()
                .id(100L).exam(mockExam).studentId(1L).status(AttemptStatus.IN_PROGRESS).build();

        // Sinh viên làm đúng cả 2 câu -> 10 điểm tròn
        QuestionOption correctOpt = QuestionOption.builder().id(1L).isCorrect(true).build();
        List<StudentExamAnswer> mockAnswers = List.of(
                StudentExamAnswer.builder().selectedOption(correctOpt).build(),
                StudentExamAnswer.builder().selectedOption(correctOpt).build()
        );

        // Bảng điểm hiện tại đang trống trơn (Mới đi học chưa có đầu điểm nào)
        ClassGrade freshGrade = ClassGrade.builder().enrollment(mockEnrollment).build();

        // Gài dữ liệu Mock
        when(userRepository.findByUsername("student_test")).thenReturn(Optional.of(mockUser));
        when(attemptRepository.findById(100L)).thenReturn(Optional.of(mockAttempt));
        when(answerRepository.findByAttemptId(100L)).thenReturn(mockAnswers);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 20L)).thenReturn(Optional.of(mockEnrollment));
        when(classGradeRepository.findByEnrollmentId(50L)).thenReturn(Optional.of(freshGrade));

        // Thực thi hàm nộp bài chủ động
        examService.submitExamAttempt(100L, false);

        // Sử dụng ArgumentCaptor để "bắt" quả data chuẩn bị lưu xuống bảng ClassGrade
        ArgumentCaptor<ClassGrade> gradeCaptor = ArgumentCaptor.forClass(ClassGrade.class);
        verify(classGradeRepository, times(1)).save(gradeCaptor.capture());

        ClassGrade savedGrade = gradeCaptor.getValue();

        // 🌟 LOG IN KẾT QUẢ KIỂM TRA
        System.out.println("📊 [TEST LOG] Kết quả dò cột điểm:");
        System.out.println("   - Cột điểm Regular 1 nhận được: " + savedGrade.getRegularScore1());
        System.out.println("   - Cột điểm Regular 2 nhận được: " + savedGrade.getRegularScore2());

        // Khẳng định: Điểm 10 phải nằm ở cột 1, cột 2 phải giữ nguyên là NULL
        assertNotNull(savedGrade.getRegularScore1());
        assertEquals(10.0, savedGrade.getRegularScore1());
        assertNull(savedGrade.getRegularScore2());

        System.out.println("✅ [TEST PASSED]: Điểm thi thường xuyên lần đầu đã đổ trúng vào cột RegularScore1!");
    }

    @Test
    @DisplayName("Đồng bộ điểm REGULAR lần 2 - Cột 1 đã đầy thì điểm phải tự động nhảy sang cột 2")
    void processGrading_RegularExam_FillsRegularScore2_WhenScore1Exists() {
        System.out.println("\n=== 🧪 RUNNING TEST CASE: Dò điểm REGULAR lần 2 (Cột 1 đã có điểm) ===");

        Exam mockExam = new Exam();
        mockExam.setId(11L);
        mockExam.setTotalQuestions(2);
        mockExam.setExamType(ExamType.REGULAR);
        mockExam.setClassEntity(com.example.LMS.entity.model.ClassEntity.builder().id(20L).build());

        StudentExamAttempt mockAttempt = StudentExamAttempt.builder()
                .id(101L).exam(mockExam).studentId(1L).status(AttemptStatus.IN_PROGRESS).build();

        // Sinh viên làm đúng 1/2 câu -> 5.0 điểm
        QuestionOption correctOpt = QuestionOption.builder().id(1L).isCorrect(true).build();
        QuestionOption incorrectOpt = QuestionOption.builder().id(2L).isCorrect(false).build();
        List<StudentExamAnswer> mockAnswers = List.of(
                StudentExamAnswer.builder().selectedOption(correctOpt).build(),
                StudentExamAnswer.builder().selectedOption(incorrectOpt).build()
        );

        // Giả lập: Sổ điểm hiện tại ĐÃ CÓ điểm Regular 1 (Lần trước được 8.0 điểm)
        ClassGrade existingGrade = ClassGrade.builder()
                .enrollment(mockEnrollment)
                .regularScore1(8.0) // Cột 1 đã bị chiếm chỗ!
                .build();

        when(userRepository.findByUsername("student_test")).thenReturn(Optional.of(mockUser));
        when(attemptRepository.findById(101L)).thenReturn(Optional.of(mockAttempt));
        when(answerRepository.findByAttemptId(101L)).thenReturn(mockAnswers);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 20L)).thenReturn(Optional.of(mockEnrollment));
        when(classGradeRepository.findByEnrollmentId(50L)).thenReturn(Optional.of(existingGrade));

        // Thực thi
        examService.submitExamAttempt(101L, false);

        ArgumentCaptor<ClassGrade> gradeCaptor = ArgumentCaptor.forClass(ClassGrade.class);
        verify(classGradeRepository, times(1)).save(gradeCaptor.capture());
        ClassGrade savedGrade = gradeCaptor.getValue();

        // 🌟 LOG IN KẾT QUẢ KIỂM TRA
        System.out.println("📊 [TEST LOG] Kết quả dò cột điểm sau khi dịch chuyển:");
        System.out.println("   - Cột điểm Regular 1 (Giữ nguyên): " + savedGrade.getRegularScore1());
        System.out.println("   - Cột điểm Regular 2 (Tự động điền mới): " + savedGrade.getRegularScore2());

        // Khẳng định: Cột 1 giữ nguyên là 8.0, điểm 5.0 mới chấm phải ăn vào cột 2
        assertEquals(8.0, savedGrade.getRegularScore1());
        assertNotNull(savedGrade.getRegularScore2());
        assertEquals(5.0, savedGrade.getRegularScore2());

        System.out.println("✅ [TEST PASSED]: Hệ thống tự dịch chuyển điểm sang cột RegularScore2 thông minh!");
    }

    @Test
    @DisplayName("Đồng bộ điểm MIDTERM - Điểm phải nhảy thẳng vào cột Giữa kỳ")
    void processGrading_MidtermExam_FillsMidtermScore() {
        System.out.println("\n=== 🧪 RUNNING TEST CASE: Đổ điểm MIDTERM (Giữa kỳ) ===");

        Exam mockExam = new Exam();
        mockExam.setId(12L);
        mockExam.setTotalQuestions(4);
        mockExam.setExamType(ExamType.MIDTERM); // Đề thi giữa kỳ
        mockExam.setClassEntity(com.example.LMS.entity.model.ClassEntity.builder().id(20L).build());

        StudentExamAttempt mockAttempt = StudentExamAttempt.builder()
                .id(102L).exam(mockExam).studentId(1L).status(AttemptStatus.IN_PROGRESS).build();

        // Đúng 3/4 câu -> 7.5 điểm
        QuestionOption correctOpt = QuestionOption.builder().id(1L).isCorrect(true).build();
        QuestionOption incorrectOpt = QuestionOption.builder().id(2L).isCorrect(false).build();
        List<StudentExamAnswer> mockAnswers = List.of(
                StudentExamAnswer.builder().selectedOption(correctOpt).build(),
                StudentExamAnswer.builder().selectedOption(correctOpt).build(),
                StudentExamAnswer.builder().selectedOption(correctOpt).build(),
                StudentExamAnswer.builder().selectedOption(incorrectOpt).build()
        );

        ClassGrade gradeRecord = ClassGrade.builder().enrollment(mockEnrollment).build();

        when(userRepository.findByUsername("student_test")).thenReturn(Optional.of(mockUser));
        when(attemptRepository.findById(102L)).thenReturn(Optional.of(mockAttempt));
        when(answerRepository.findByAttemptId(102L)).thenReturn(mockAnswers);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 20L)).thenReturn(Optional.of(mockEnrollment));
        when(classGradeRepository.findByEnrollmentId(50L)).thenReturn(Optional.of(gradeRecord));

        // Thực thi
        examService.submitExamAttempt(102L, false);

        ArgumentCaptor<ClassGrade> gradeCaptor = ArgumentCaptor.forClass(ClassGrade.class);
        verify(classGradeRepository, times(1)).save(gradeCaptor.capture());
        ClassGrade savedGrade = gradeCaptor.getValue();

        // 🌟 LOG IN KẾT QUẢ KIỂM TRA
        System.out.println("📊 [TEST LOG] Kết quả ghi nhận điểm Giữa kỳ:");
        System.out.println("   - Cột Midterm Score: " + savedGrade.getMidtermScore());

        assertNotNull(savedGrade.getMidtermScore());
        assertEquals(7.5, savedGrade.getMidtermScore());

        System.out.println("✅ [TEST PASSED]: Điểm giữa kỳ đổ trúng đích 7.5!");
    }
}