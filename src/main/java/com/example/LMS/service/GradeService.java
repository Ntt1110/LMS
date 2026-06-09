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

        // 4. Lấy bảng điểm — nếu chưa có thì trả về điểm 0, trạng thái PENDING
        ClassGrade grade = classGradeRepository
                .findByClassIdAndStudentId(classId, student.getId())
                .orElse(null);

        // 5. Lấy thông tin môn học
        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        log.info("Sinh viên {} xem bảng điểm lớp ID: {}", username, classId);

        return GradeResponseDto.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .courseName(course != null ? course.getName() : "N/A")
                .courseCode(course != null ? course.getCode() : "N/A")
                .credits(course != null ? course.getCredits() : null)
                .regularScore1(grade != null && grade.getRegularScore1() != null ? grade.getRegularScore1() : 0.0)
                .regularScore2(grade != null && grade.getRegularScore2() != null ? grade.getRegularScore2() : 0.0)
                .midtermScore(grade != null && grade.getMidtermScore() != null ? grade.getMidtermScore() : 0.0)
                .finalScore(grade != null && grade.getFinalScore() != null ? grade.getFinalScore() : 0.0)
                .totalScore(grade != null && grade.getTotalScore() != null ? grade.getTotalScore() : 0.0)
                .status(grade != null && grade.getStatus() != null ? grade.getStatus().name() : "PENDING")
                .build();
    }
    // ============================================================
    // XEM DANH SÁCH BẢNG ĐIỂM SINH VIÊN CỦA LỚP (Giảng viên)
    // GET /api/v1/classes/{classId}/grades
    // Quyền: GRADE_LOCK (id = 52)
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

            // Lấy thông tin sinh viên
            var user = userRepository.findById(studentId).orElse(null);
            var profile = userProfileRepository.findByUserId(studentId).orElse(null);
            var studentProfile = studentProfileRepository.findByUserId(studentId).orElse(null);

            // Lấy điểm — nếu chưa có thì để null hết
            var gradeOpt = classGradeRepository.findByClassIdAndStudentId(classId, studentId);

            var dto = ClassGradeListResponseDto.StudentGradeDto.builder()
                    .studentId(studentId)
                    .studentCode(studentProfile != null ? studentProfile.getStudentCode() : "N/A")
                    .fullName(profile != null ? profile.getFullName() : "N/A")
                    .email(user != null ? user.getEmail() : "N/A")
                    .regularScore1(gradeOpt.map(g -> g.getRegularScore1() != null ? g.getRegularScore1() : 0.0).orElse(0.0))
                    .regularScore2(gradeOpt.map(g -> g.getRegularScore2() != null ? g.getRegularScore2() : 0.0).orElse(0.0))
                    .midtermScore(gradeOpt.map(g -> g.getMidtermScore() != null ? g.getMidtermScore() : 0.0).orElse(0.0))
                    .finalScore(gradeOpt.map(g -> g.getFinalScore() != null ? g.getFinalScore() : 0.0).orElse(0.0))
                    .totalScore(gradeOpt.map(g -> g.getTotalScore() != null ? g.getTotalScore() : 0.0).orElse(0.0))
                    .status(gradeOpt.map(g -> g.getStatus() != null ? g.getStatus().name() : "PENDING").orElse("PENDING"))
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
    // Trả về tất cả học kỳ đã học, mỗi học kỳ có danh sách môn + điểm.
    // Môn chưa có điểm thì trả về 0 hết, trạng thái PENDING.
    // ============================================================
    public TranscriptResponseDto getMyTranscript() {

        // 1. Lấy sinh viên đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var student = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản!"));

        // 2. Lấy toàn bộ điểm của sinh viên (đã sắp xếp theo học kỳ, môn)
        List<ClassGrade> allGrades = classGradeRepository.findAllByStudentId(student.getId());

        // 3. Gom theo học kỳ (dùng LinkedHashMap giữ thứ tự đã sắp xếp từ query)
        Map<Long, List<ClassGrade>> bySemester = new LinkedHashMap<>();
        for (ClassGrade g : allGrades) {
            Semester sem = g.getEnrollment().getClassEntity().getSemester();
            bySemester.computeIfAbsent(sem.getId(), k -> new ArrayList<>()).add(g);
        }

        // 4. Build từng học kỳ
        int totalCreditsEarned = 0;
        double weightedSum = 0.0;
        int totalWeightedCredits = 0;
        int totalSubjects = 0;

        var semesterDtos = new ArrayList<TranscriptResponseDto.SemesterTranscriptDto>();

        for (Map.Entry<Long, List<ClassGrade>> entry : bySemester.entrySet()) {
            Semester sem = entry.getValue().get(0).getEnrollment().getClassEntity().getSemester();
            List<ClassGrade> grades = entry.getValue();

            int semCredits = 0;
            int semCreditsEarned = 0;
            double semWeightedSum = 0.0;
            int semWeightedCredits = 0;

            var subjectDtos = new ArrayList<TranscriptResponseDto.SubjectGradeDto>();

            for (ClassGrade g : grades) {
                var classEntity = g.getEnrollment().getClassEntity();
                var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);
                int credits = (course != null && course.getCredits() != null) ? course.getCredits() : 0;

                double total = g.getTotalScore() != null ? g.getTotalScore() : 0.0;
                String status = (g.getStatus() != null) ? g.getStatus().name() : "PENDING";

                semCredits += credits;
                if ("PASS".equals(status)) {
                    semCreditsEarned += credits;
                    semWeightedSum += total * credits;
                    semWeightedCredits += credits;
                }
                totalSubjects++;

                subjectDtos.add(TranscriptResponseDto.SubjectGradeDto.builder()
                        .classId(classEntity.getId())
                        .classCode(classEntity.getCode())
                        .courseCode(course != null ? course.getCode() : "N/A")
                        .courseName(course != null ? course.getName() : "N/A")
                        .credits(credits)
                        .regularScore1(g.getRegularScore1() != null ? g.getRegularScore1() : 0.0)
                        .regularScore2(g.getRegularScore2() != null ? g.getRegularScore2() : 0.0)
                        .midtermScore(g.getMidtermScore() != null ? g.getMidtermScore() : 0.0)
                        .finalScore(g.getFinalScore() != null ? g.getFinalScore() : 0.0)
                        .totalScore(total)
                        .status(status)
                        .build());
            }

            double semGpa = semWeightedCredits > 0
                    ? Math.round((semWeightedSum / semWeightedCredits) * 100.0) / 100.0
                    : 0.0;

            totalCreditsEarned += semCreditsEarned;
            weightedSum += semWeightedSum;
            totalWeightedCredits += semWeightedCredits;

            semesterDtos.add(TranscriptResponseDto.SemesterTranscriptDto.builder()
                    .semesterId(sem.getId())
                    .semesterCode(sem.getSemesterCode())
                    .academicYear(sem.getAcademicYear())
                    .semesterNumber(sem.getSemesterNumber())
                    .gpaThisSemester(semGpa)
                    .creditsThisSemester(semCredits)
                    .creditsEarnedThisSemester(semCreditsEarned)
                    .subjects(subjectDtos)
                    .build());
        }

        double gpaOverall = totalWeightedCredits > 0
                ? Math.round((weightedSum / totalWeightedCredits) * 100.0) / 100.0
                : 0.0;

        log.info("Sinh viên {} xem bảng điểm toàn khoá — {} học kỳ, {} môn", username, semesterDtos.size(), totalSubjects);

        return TranscriptResponseDto.builder()
                .gpaOverall(gpaOverall)
                .totalCreditsEarned(totalCreditsEarned)
                .totalSubjects(totalSubjects)
                .semesters(semesterDtos)
                .build();
    }
}