package com.example.LMS.service;

import com.example.LMS.dto.response.GradeResponseDto;
import com.example.LMS.dto.response.ClassGradeListResponseDto;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.repository.EnrollmentRepository;
import com.example.LMS.repository.UserProfileRepository;
import com.example.LMS.repository.StudentProfileRepository;
import java.util.ArrayList;
import java.util.List;
import com.example.LMS.entity.model.ClassGrade;
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

        // 4. Lấy bảng điểm
        ClassGrade grade = classGradeRepository
                .findByClassIdAndStudentId(classId, student.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Chưa có bảng điểm cho lớp này!"));

        // 5. Lấy thông tin môn học
        var course = courseRepository.findById(classEntity.getCourseId()).orElse(null);

        log.info("Sinh viên {} xem bảng điểm lớp ID: {}", username, classId);

        return GradeResponseDto.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .courseName(course != null ? course.getName() : "N/A")
                .courseCode(course != null ? course.getCode() : "N/A")
                .credits(course != null ? course.getCredits() : null)
                .regularScore1(grade.getRegularScore1())
                .regularScore2(grade.getRegularScore2())
                .midtermScore(grade.getMidtermScore())
                .finalScore(grade.getFinalScore())
                .totalScore(grade.getTotalScore())
                .status(grade.getStatus() != null ? grade.getStatus().name() : "PENDING")
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
                    .regularScore1(gradeOpt.map(g -> g.getRegularScore1()).orElse(null))
                    .regularScore2(gradeOpt.map(g -> g.getRegularScore2()).orElse(null))
                    .midtermScore(gradeOpt.map(g -> g.getMidtermScore()).orElse(null))
                    .finalScore(gradeOpt.map(g -> g.getFinalScore()).orElse(null))
                    .totalScore(gradeOpt.map(g -> g.getTotalScore()).orElse(null))
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
}