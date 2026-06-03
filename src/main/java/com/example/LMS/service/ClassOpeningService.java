package com.example.LMS.service;

import com.example.LMS.dto.request.ClassOpeningRequestDto;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.entity.model.ClassOpeningRequest;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.ClassOpeningRequestRepository;
import com.example.LMS.repository.SemesterRepository;
import com.example.LMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassOpeningService {

    private final ClassOpeningRequestRepository requestRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createOpeningRequest(ClassOpeningRequestDto dto) {
        log.info("⏳ Tháo chốt kiểm tra đề xuất mở lớp học phần mới...");

        // 1. Kiểm tra học kỳ gửi lên có tồn tại trong hệ thống hay không
        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Học kỳ được chọn không tồn tại!"));

        if (semester.getStatus() == Semester.SemesterStatus.CLOSED) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Không thể đề xuất mở lớp cho học kỳ đã đóng!");
        }

        // 2. Lấy thông tin giảng viên/trưởng bộ môn đang thao tác từ Security Context
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tài khoản người đề xuất!"));

        // 3. Tiến hành đóng gói dữ liệu và lưu đơn ở trạng thái PENDING
        ClassOpeningRequest openingRequest = ClassOpeningRequest.builder()
                .semester(semester)
                .courseId(dto.getCourseId())
                .requesterId(user.getId())
                .expectedStudents(dto.getExpectedStudents())
                .note(dto.getNote())
                .status(ClassOpenningStatus.PENDING) // Luôn luôn là chờ duyệt
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestRepository.save(openingRequest);
        log.info("✅ Giảng viên {} đã gửi đề xuất mở lớp thành công, chờ Phòng Đào tạo duyệt.", currentUsername);
    }
}