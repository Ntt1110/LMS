package com.example.LMS.service;

import com.example.LMS.dto.request.FinalizeGradeRequest;
import com.example.LMS.dto.response.GradeResponseDto;
import com.example.LMS.dto.response.ClassGradeListResponseDto;
import com.example.LMS.dto.response.TranscriptResponseDto;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.model.ClassEnrollment;
import com.example.LMS.entity.model.ClassGrade;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private String calcStatus(Double tx1, Double tx2, Double gk, Double ck) {
        if (tx1 == null || tx2 == null || gk == null || ck == null) return "PENDING";
        return calcTotalScore(tx1, tx2, gk, ck) >= 4.0 ? "PASS" : "FAIL";
    }

    private double calcTotalScorePartial(Double tx1, Double tx2, Double gk, Double ck) {
        double t1 = tx1 != null ? tx1 : 0.0;
        double t2 = tx2 != null ? tx2 : 0.0;
        double g  = gk  != null ? gk  : 0.0;
        double c  = ck  != null ? ck  : 0.0;
        return Math.round((t1 * 0.1 + t2 * 0.1 + g * 0.3 + c * 0.5) * 100.0) / 100.0;
    }

    private double toGrade4(double score10) {
        if (score10 >= 8.5) return 4.0;
        if (score10 >= 7.0) return 3.0;
        if (score10 >= 5.5) return 2.0;
        if (score10 >= 4.0) return 1.0;
        return 0.0;
    }

    private String toLetterGrade(double score10) {
        if (score10 >= 8.5) return "A";
        if (score10 >= 7.0) return "B";
        if (score10 >= 5.5) return "C";
        if (score10 >= 4.0) return "D";
        return "F";
    }

    // ============================================================
    // XEM BẢNG ĐIỂM CỦA LỚP (Sinh viên)
    // ============================================================
    public GradeResponseDto getMyGrade(Long classId) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        enrollmentRepository.findByClassEntityIdAndStudentId(classId, student.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học phần này!"));

        ClassGrade grade = classGradeRepository
                .findByClassIdAndStudentId(classId, student.getId())
                .orElse(null);

        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        Double tx1 = grade != null ? grade.getRegularScore1() : null;
        Double tx2 = grade != null ? grade.getRegularScore2() : null;
        Double gk  = grade != null ? grade.getMidtermScore()  : null;
        Double ck  = grade != null ? grade.getFinalScore()     : null;

        String status = calcStatus(tx1, tx2, gk, ck);
        double total  = calcTotalScorePartial(tx1, tx2, gk, ck);

        Double grade4      = "PENDING".equals(status) ? null : toGrade4(total);
        String letterGrade = "PENDING".equals(status) ? null : toLetterGrade(total);

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
    // ============================================================
    public ClassGradeListResponseDto getClassGrades(Long classId) {

        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);
        var enrollments = enrollmentRepository.findByClassEntityIdAndStatusNot(classId, EnrollmentStatus.DROPPED);
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

            studentGrades.add(ClassGradeListResponseDto.StudentGradeDto.builder()
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
                    .build());
        }

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
    // CHỐT ĐIỂM LỚP (Giảng viên)
    // POST /api/v1/grades/lock?classId=1
    // Nghiệp vụ:
    //   1. Kiểm tra lớp tồn tại & GV được phân công
    //   2. Kiểm tra không còn SV nào thiếu bảng điểm
    //   3. Kiểm tra không còn bảng điểm nào PENDING
    //   4. Đổi trạng thái lớp sang COMPLETED
    // ============================================================
    @Transactional
    public void lockClassGrades(Long classId) {

        // 1. Lấy giảng viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Kiểm tra lớp tồn tại
        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        // 3. Kiểm tra GV có được phân công dạy lớp này không
        if (!lecturer.getId().equals(classEntity.getLecturerId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bạn không có quyền chốt điểm cho lớp này!");
        }

        // 4. Kiểm tra lớp đã COMPLETED chưa
        if (classEntity.getStatus() == com.example.LMS.entity.Enum.ClassStatus.COMPLETED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lớp học phần này đã được chốt điểm trước đó!");
        }

        // 5. Kiểm tra còn SV nào chưa có bảng điểm không
        long missingGradeCount = classGradeRepository.countEnrollmentsWithoutGrade(classId);
        if (missingGradeCount > 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Còn " + missingGradeCount + " sinh viên chưa được nhập điểm. Vui lòng nhập đủ điểm trước khi chốt!");
        }

        // 6. Kiểm tra còn bảng điểm nào status = PENDING không
        boolean hasPending = classGradeRepository.existsPendingByClassId(classId);
        if (hasPending) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Vẫn còn sinh viên có trạng thái điểm PENDING. Vui lòng nhập đủ 4 cột điểm cho tất cả sinh viên trước khi chốt lớp!");
        }

        // 7. Chốt lớp — cập nhật trạng thái sang COMPLETED
        classEntity.setStatus(com.example.LMS.entity.Enum.ClassStatus.COMPLETED);
        classEntity.setUpdatedAt(LocalDateTime.now());
        classEntityRepository.save(classEntity);

        log.info("✅ Giảng viên {} đã chốt điểm lớp ID {} — Status chuyển sang COMPLETED", username, classId);
    }

    // ============================================================
    // CHỐT ĐIỂM (Giảng viên) — GRADE_LOCK
    // POST /api/v1/classes/{classId}/grades/finalize
    // Nhập đủ 4 cột điểm cho 1 SV → tính totalScore + status → lưu DB
    // Chỉ giảng viên được phân công dạy lớp đó mới được chốt
    // ============================================================
    @Transactional
    public ClassGradeListResponseDto.StudentGradeDto finalizeGrade(Long classId, FinalizeGradeRequest request) {

        // 1. Lấy giảng viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var lecturer = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Kiểm tra lớp tồn tại
        var classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần!"));

        // 3. Kiểm tra giảng viên có được phân công dạy lớp này không
        if (!lecturer.getId().equals(classEntity.getLecturerId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Bạn không có quyền chốt điểm cho lớp này!");
        }

        // 4. Kiểm tra sinh viên có trong lớp không (không bị DROPPED)
        ClassEnrollment enrollment = enrollmentRepository
                .findByClassEntityIdAndStudentId(classId, request.getStudentId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Sinh viên không thuộc lớp học phần này!"));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Sinh viên đã hủy đăng ký lớp này!");
        }

        // 5. Tính toán điểm
        Double tx1 = request.getRegularScore1();
        Double tx2 = request.getRegularScore2();
        Double gk  = request.getMidtermScore();
        Double ck  = request.getFinalScore();

        double total = calcTotalScore(tx1, tx2, gk, ck);
        ClassGrade.GradeStatus gradeStatus = total >= 4.0
                ? ClassGrade.GradeStatus.PASS
                : ClassGrade.GradeStatus.FAIL;

        // 6. Tạo mới hoặc cập nhật bảng điểm
        ClassGrade grade = classGradeRepository
                .findByEnrollmentId(enrollment.getId())
                .orElse(ClassGrade.builder().enrollment(enrollment).build());

        grade.setRegularScore1(tx1);
        grade.setRegularScore2(tx2);
        grade.setMidtermScore(gk);
        grade.setFinalScore(ck);
        grade.setTotalScore(total);
        grade.setStatus(gradeStatus);
        grade.setUpdatedAt(LocalDateTime.now());

        classGradeRepository.save(grade);

        log.info("✅ Giảng viên {} đã chốt điểm SV ID {} lớp ID {} — Total: {}, Status: {}",
                username, request.getStudentId(), classId, total, gradeStatus);

        // 7. Trả về thông tin điểm vừa chốt
        var profile        = userProfileRepository.findByUserId(request.getStudentId()).orElse(null);
        var studentProfile = studentProfileRepository.findByUserId(request.getStudentId()).orElse(null);
        var user           = userRepository.findById(request.getStudentId()).orElse(null);

        return ClassGradeListResponseDto.StudentGradeDto.builder()
                .studentId(request.getStudentId())
                .studentCode(studentProfile != null ? studentProfile.getStudentCode() : "N/A")
                .fullName(profile != null ? profile.getFullName() : "N/A")
                .email(user != null ? user.getEmail() : "N/A")
                .regularScore1(tx1)
                .regularScore2(tx2)
                .midtermScore(gk)
                .finalScore(ck)
                .totalScore(total)
                .status(gradeStatus.name())
                .build();
    }

    // ============================================================
    // XEM BẢNG ĐIỂM TOÀN KHOÁ (Sinh viên)
    // ============================================================
    public TranscriptResponseDto getMyTranscript() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        List<ClassGrade> allGrades = classGradeRepository.findAllByStudentId(student.getId());

        Map<Long, List<ClassGrade>> bySemester = new LinkedHashMap<>();
        for (ClassGrade g : allGrades) {
            Semester sem = g.getEnrollment().getClassEntity().getSemester();
            bySemester.computeIfAbsent(sem.getId(), k -> new ArrayList<>()).add(g);
        }

        int    totalCreditsEarned   = 0;
        double weightedSum10        = 0.0;
        double weightedSum4         = 0.0;
        int    totalWeightedCredits = 0;
        int    totalSubjects        = 0;

        var semesterDtos = new ArrayList<TranscriptResponseDto.SemesterTranscriptDto>();

        for (Map.Entry<Long, List<ClassGrade>> entry : bySemester.entrySet()) {
            Semester sem    = entry.getValue().get(0).getEnrollment().getClassEntity().getSemester();
            List<ClassGrade> grades = entry.getValue();

            int    semCredits         = 0;
            int    semCreditsEarned   = 0;
            double semWeightedSum10   = 0.0;
            double semWeightedSum4    = 0.0;
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

        return TranscriptResponseDto.builder()
                .gpaOverall(gpaOverall10)
                .gpaOverall4(gpaOverall4)
                .totalCreditsEarned(totalCreditsEarned)
                .totalSubjects(totalSubjects)
                .semesters(semesterDtos)
                .build();
    }
}