package com.example.LMS.service;

import com.example.LMS.dto.response.GradeResponseDto;
import com.example.LMS.dto.response.ClassGradeListResponseDto;
import com.example.LMS.dto.response.TranscriptResponseDto;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.repository.EnrollmentRepository;
import com.example.LMS.repository.UserProfileRepository;
import com.example.LMS.repository.StudentProfileRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.example.LMS.entity.model.ClassGrade;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassEntityRepository;
import com.example.LMS.repository.ClassGradeRepository;
import com.example.LMS.repository.CourseRepository;
import com.example.LMS.repository.EnrollmentRepository;
import com.example.LMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final ClassGradeRepository classGradeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final StudentProfileRepository studentProfileRepository;

    // ============================================================
    // HELPER: Tính điểm trung bình môn (totalScore)
    // Công thức: tx1*10% + tx2*10% + gk*30% + ck*50%
    // ============================================================
    private double calcTotalScore(Double tx1, Double tx2, Double gk, Double ck) {
        return Math.round((tx1 * 0.1 + tx2 * 0.1 + gk * 0.3 + ck * 0.5) * 100.0) / 100.0;
    }

    // ============================================================
    // HELPER: Xác định status dựa trên 4 cột điểm
    // Chưa đủ 4 cột → PENDING (vẫn tính totalScore nếu có thể)
    // Đủ 4 cột, totalScore >= 4.0 → PASS
    // Đủ 4 cột, totalScore <  4.0 → FAIL
    // ============================================================
    private String calcStatus(Double tx1, Double tx2, Double gk, Double ck) {
        if (tx1 == null || tx2 == null || gk == null || ck == null) return "PENDING";
        return calcTotalScore(tx1, tx2, gk, ck) >= 4.0 ? "PASS" : "FAIL";
    }

    // ============================================================
    // HELPER: Tính totalScore trả về FE
    // Nếu đủ 4 cột → tính đủ
    // Nếu thiếu cột nào → tính với các cột đã có (cột thiếu tính = 0)
    // ============================================================
    private double calcTotalScorePartial(Double tx1, Double tx2, Double gk, Double ck) {
        double t1 = tx1 != null ? tx1 : 0.0;
        double t2 = tx2 != null ? tx2 : 0.0;
        double g  = gk  != null ? gk  : 0.0;
        double c  = ck  != null ? ck  : 0.0;
        return Math.round((t1 * 0.1 + t2 * 0.1 + g * 0.3 + c * 0.5) * 100.0) / 100.0;
    }

    // ============================================================
    // HELPER: Quy đổi điểm thang 10 → thang 4
    // A=4.0 | B=3.0 | C=2.0 | D=1.0 | F=0.0
    // ============================================================
    private double toGrade4(double score10) {
        if (score10 >= 8.5) return 4.0;
        if (score10 >= 7.0) return 3.0;
        if (score10 >= 5.5) return 2.0;
        if (score10 >= 4.0) return 1.0;
        return 0.0;
    }

    // ============================================================
    // HELPER: Quy đổi điểm thang 10 → điểm chữ
    // A | B | C | D | F
    // ============================================================
    private String toLetterGrade(double score10) {
        if (score10 >= 8.5) return "A";
        if (score10 >= 7.0) return "B";
        if (score10 >= 5.5) return "C";
        if (score10 >= 4.0) return "D";
        return "F";
    }

    // ============================================================
    // XEM BẢNG ĐIỂM CỦA LỚP (Sinh viên)
    // GET /api/v1/classes/{classId}/my-grade
    // ============================================================
    public GradeResponseDto getMyGrade(Long classId) {

        // 1. Lấy sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Kiểm tra lớp tồn tại
        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        // 3. Kiểm tra sinh viên có đăng ký lớp này không
        enrollmentRepository.findByClassEntityIdAndStudentId(classId, student.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học phần này!"));

        // 4. Lấy bảng điểm
        ClassGrade grade = classGradeRepository
                .findByClassIdAndStudentId(classId, student.getId())
                .orElse(null);

        // 5. Lấy thông tin môn học
        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        // 6. Lấy từng cột điểm (null nếu chưa nhập)
        Double tx1 = grade != null ? grade.getRegularScore1() : null;
        Double tx2 = grade != null ? grade.getRegularScore2() : null;
        Double gk  = grade != null ? grade.getMidtermScore()  : null;
        Double ck  = grade != null ? grade.getFinalScore()     : null;

        // 7. Tính status và totalScore theo logic mới
        String status = calcStatus(tx1, tx2, gk, ck);
        double total  = calcTotalScorePartial(tx1, tx2, gk, ck);

        // 8. Chỉ trả điểm hệ 4 + chữ khi không còn PENDING
        Double grade4     = "PENDING".equals(status) ? null : toGrade4(total);
        String letterGrade = "PENDING".equals(status) ? null : toLetterGrade(total);

        log.info("Sinh viên {} xem bảng điểm lớp ID: {}", username, classId);

        return GradeResponseDto.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .courseName(course != null ? course.getName() : "N/A")
                .courseCode(course != null ? course.getCode() : "N/A")
                .credits(course != null ? course.getCredits() : null)
                .regularScore1(tx1 != null ? tx1 : 0.0)
                .regularScore2(tx2 != null ? tx2 : 0.0)
                .midtermScore(gk  != null ? gk  : 0.0)
                .finalScore(ck    != null ? ck  : 0.0)
                .totalScore(total)
                .grade4(grade4)
                .letterGrade(letterGrade)
                .status(status)
                .build();
    }

    // ============================================================
    // XEM DANH SÁCH BẢNG ĐIỂM SINH VIÊN CỦA LỚP (Giảng viên)
    // GET /api/v1/classes/{classId}/grades
    // ============================================================
    public ClassGradeListResponseDto getClassGrades(Long classId) {

        // 1. Kiểm tra lớp tồn tại
        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        // 2. Lấy thông tin môn học
        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        // 3. Lấy danh sách sinh viên trong lớp (không bị DROPPED)
        var enrollments = enrollmentRepository.findByClassEntityIdAndStatusNot(classId, EnrollmentStatus.DROPPED);

        // 4. Build danh sách điểm từng sinh viên
        var studentGrades = new ArrayList<ClassGradeListResponseDto.StudentGradeDto>();

        for (var enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();

            var user           = userRepository.findById(studentId).orElse(null);
            var profile        = userProfileRepository.findByUserId(studentId).orElse(null);
            var studentProfile = studentProfileRepository.findByUserId(studentId).orElse(null);
            var gradeOpt       = classGradeRepository.findByClassIdAndStudentId(classId, studentId);

            Double tx1 = gradeOpt.map(ClassGrade::getRegularScore1).orElse(null);
            Double tx2 = gradeOpt.map(ClassGrade::getRegularScore2).orElse(null);
            Double gk  = gradeOpt.map(ClassGrade::getMidtermScore).orElse(null);
            Double ck  = gradeOpt.map(ClassGrade::getFinalScore).orElse(null);

            String status = calcStatus(tx1, tx2, gk, ck);
            double total  = calcTotalScorePartial(tx1, tx2, gk, ck);

            var dto = ClassGradeListResponseDto.StudentGradeDto.builder()
                    .studentId(studentId)
                    .studentCode(studentProfile != null ? studentProfile.getStudentCode() : "N/A")
                    .fullName(profile != null ? profile.getFullName() : "N/A")
                    .email(user != null ? user.getEmail() : "N/A")
                    .regularScore1(tx1 != null ? tx1 : 0.0)
                    .regularScore2(tx2 != null ? tx2 : 0.0)
                    .midtermScore(gk   != null ? gk  : 0.0)
                    .finalScore(ck     != null ? ck  : 0.0)
                    .totalScore(total)
                    .status(status)
                    .build();

            studentGrades.add(dto);
        }

        log.info("Giảng viên xem bảng điểm lớp ID: {}, tổng {} sinh viên", classId, studentGrades.size());

        return ClassGradeListResponseDto.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .courseName(course != null ? course.getName() : "N/A")
                .courseCode(course != null ? course.getCode() : "N/A")
                .totalStudents(studentGrades.size())
                .students(studentGrades)
                .build();
    }

    // ============================================================
    // XEM BẢNG ĐIỂM TOÀN KHOÁ (Sinh viên)
    // GET /api/v1/grades/my-transcript
    // Chỉ lấy học kỳ đã CLOSED (filter ở repository).
    // ============================================================
    public TranscriptResponseDto getMyTranscript() {

        // 1. Lấy sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Lấy toàn bộ điểm của sinh viên — chỉ học kỳ CLOSED
        List<ClassGrade> allGrades = classGradeRepository.findAllByStudentId(student.getId());

        // 3. Gom theo học kỳ
        Map<Long, List<ClassGrade>> bySemester = new LinkedHashMap<>();
        for (ClassGrade g : allGrades) {
            Semester sem = g.getEnrollment().getClassEntity().getSemester();
            bySemester.computeIfAbsent(sem.getId(), k -> new ArrayList<>()).add(g);
        }

        // 4. Build từng học kỳ
        int    totalCreditsEarned  = 0;
        double weightedSum10       = 0.0;
        double weightedSum4        = 0.0;
        int    totalWeightedCredits = 0;
        int    totalSubjects        = 0;

        var semesterDtos = new ArrayList<TranscriptResponseDto.SemesterTranscriptDto>();

        for (Map.Entry<Long, List<ClassGrade>> entry : bySemester.entrySet()) {
            Semester sem    = entry.getValue().get(0).getEnrollment().getClassEntity().getSemester();
            List<ClassGrade> grades = entry.getValue();

            int    semCredits        = 0;
            int    semCreditsEarned  = 0;
            double semWeightedSum10  = 0.0;
            double semWeightedSum4   = 0.0;
            int    semWeightedCredits = 0;

            var subjectDtos = new ArrayList<TranscriptResponseDto.SubjectGradeDto>();

            for (ClassGrade g : grades) {
                var classEntity = g.getEnrollment().getClassEntity();
                var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);
                int credits = (course != null && course.getCredits() != null) ? course.getCredits() : 0;

                Double tx1 = g.getRegularScore1();
                Double tx2 = g.getRegularScore2();
                Double gk  = g.getMidtermScore();
                Double ck  = g.getFinalScore();

                String status = calcStatus(tx1, tx2, gk, ck);
                double total  = calcTotalScorePartial(tx1, tx2, gk, ck);

                Double grade4      = "PENDING".equals(status) ? null : toGrade4(total);
                String letterGrade = "PENDING".equals(status) ? null : toLetterGrade(total);

                semCredits += credits;
                if ("PASS".equals(status)) {
                    semCreditsEarned   += credits;
                    semWeightedSum10   += total * credits;
                    semWeightedSum4    += toGrade4(total) * credits;
                    semWeightedCredits += credits;
                }
                totalSubjects++;

                subjectDtos.add(TranscriptResponseDto.SubjectGradeDto.builder()
                        .classId(classEntity.getId())
                        .classCode(classEntity.getCode())
                        .courseCode(course != null ? course.getCode() : "N/A")
                        .courseName(course != null ? course.getName() : "N/A")
                        .credits(credits)
                        .regularScore1(tx1 != null ? tx1 : 0.0)
                        .regularScore2(tx2 != null ? tx2 : 0.0)
                        .midtermScore(gk   != null ? gk  : 0.0)
                        .finalScore(ck     != null ? ck  : 0.0)
                        .totalScore(total)
                        .grade4(grade4)
                        .letterGrade(letterGrade)
                        .status(status)
                        .build());
            }

            double semGpa10 = semWeightedCredits > 0
                    ? Math.round((semWeightedSum10 / semWeightedCredits) * 100.0) / 100.0 : 0.0;
            double semGpa4  = semWeightedCredits > 0
                    ? Math.round((semWeightedSum4  / semWeightedCredits) * 100.0) / 100.0 : 0.0;

            totalCreditsEarned   += semCreditsEarned;
            weightedSum10        += semWeightedSum10;
            weightedSum4         += semWeightedSum4;
            totalWeightedCredits += semWeightedCredits;

            semesterDtos.add(TranscriptResponseDto.SemesterTranscriptDto.builder()
                    .semesterId(sem.getId())
                    .semesterCode(sem.getSemesterCode())
                    .academicYear(sem.getAcademicYear())
                    .semesterNumber(sem.getSemesterNumber())
                    .gpaThisSemester(semGpa10)
                    .gpaThisSemester4(semGpa4)
                    .creditsThisSemester(semCredits)
                    .creditsEarnedThisSemester(semCreditsEarned)
                    .subjects(subjectDtos)
                    .build());
        }

        double gpaOverall10 = totalWeightedCredits > 0
                ? Math.round((weightedSum10 / totalWeightedCredits) * 100.0) / 100.0 : 0.0;
        double gpaOverall4  = totalWeightedCredits > 0
                ? Math.round((weightedSum4  / totalWeightedCredits) * 100.0) / 100.0 : 0.0;

        log.info("Sinh viên {} xem bảng điểm toàn khoá — {} học kỳ, {} môn", username, semesterDtos.size(), totalSubjects);

        return TranscriptResponseDto.builder()
                .gpaOverall(gpaOverall10)
                .gpaOverall4(gpaOverall4)
                .totalCreditsEarned(totalCreditsEarned)
                .totalSubjects(totalSubjects)
                .semesters(semesterDtos)
                .build();
    }
}