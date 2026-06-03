package com.example.LMS.service;

import com.example.LMS.dto.request.ApproveClassRequestDto;
import com.example.LMS.dto.request.ClassOpeningRequestDto;
import com.example.LMS.dto.response.ClassOpeningResponseDto;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.entity.Enum.ClassStatus;
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

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassOpeningService {

    private final ClassOpeningRequestRepository requestRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CourseRepository courseRepository;

    private final RoomRepository roomRepository;
    private final ShiftRepository shiftRepository;

     private final ClassEntityRepository classRepository;
     private final ClassScheduleRepository classScheduleRepository;

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

    public List<ClassOpeningResponseDto> getPendingOpeningRequests() {
        log.info("🔍 Phòng Đào tạo đang truy vấn danh sách lớp học phần chờ duyệt...");

        // 1. Tìm toàn bộ các đơn đề xuất có status là PENDING
        List<ClassOpeningRequest> pendingRequests =
                requestRepository.findByStatusOrderByCreatedAtDesc(ClassOpenningStatus.PENDING);

        // 2. Map danh sách thực thể sang danh sách DTO để gửi về
        return pendingRequests.stream().map(request -> {

            // 🔥 BIẾN HÌNH: 1 dòng duy nhất thay thế toàn bộ đống try-catch và if-else phức tạp!
            String UserSend = userProfileRepository.findByUserId(request.getRequesterId())
                    .map(UserProfile::getFullName) // Nếu có profile, trích xuất lấy FullName
                    .orElse("phòng đào tạo"); // Nếu không có, mặc định gán chuỗi này
            String courseName = courseRepository.findNameById(request.getCourseId())
                    .orElse("Môn học không tồn tại");

            return ClassOpeningResponseDto.builder()
                    .requestId(request.getId())
                    .semesterCode(request.getSemester().getSemesterCode())
                    .courseId(request.getCourseId())
                    .courseName(courseName)
                    .requesterId(request.getRequesterId())
                    .requesterName(UserSend) // Trả về tên hiển thị sạch sẽ
                    .expectedStudents(request.getExpectedStudents())
                    .note(request.getNote())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .build();
        }).collect(toList());
    }

    // 1. Lấy thông tin chi tiết của đơn đề xuất để đổ dữ liệu tĩnh lên Form
    public ClassOpeningResponseDto getRequestDetail(Long requestId) {
        ClassOpeningRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất!"));

        String teacherName = userProfileRepository.findByUserId(request.getRequesterId())
                .map(UserProfile::getFullName) //
                .orElse("Giảng viên hệ thống");

        String courseName = courseRepository.findNameById(request.getCourseId())
                .orElse("Môn học không tồn tại");

        return ClassOpeningResponseDto.builder()
                .requestId(request.getId())
                .semesterCode(request.getSemester().getSemesterCode())
                .courseId(request.getCourseId())
                .courseName(courseName)
                .requesterId(request.getRequesterId())
                .requesterName(teacherName) //
                .expectedStudents(request.getExpectedStudents())
                .note(request.getNote())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    // 2. Lấy danh sách Trưởng khoa / Giảng viên phục vụ Dropdown gán quản lý lớp
    public List<DropdownResponseDto> getLecturersDropdown() {
        return userRepository.findAllActiveLecturers();
    }

    // 3. Lấy danh sách phòng học phục vụ Dropdown xếp lịch
    public List<DropdownResponseDto> getRoomsDropdown() {
        return roomRepository.findAllRoomsForDropdown();
    }

    // 4. Lấy danh sách ca học phục vụ Dropdown xếp lịch
    public List<DropdownResponseDto> getShiftsDropdown() {
        return shiftRepository.findAllShiftsForDropdown();
    }

    @Transactional
    public void reviewOpeningRequest(Long requestId, ApproveClassRequestDto dto) {
        log.info("⚡ Tiến hành thẩm định đơn đề xuất mở lớp ID: {} từ Form xếp lịch...", requestId);

        // 1. Tìm đơn đề xuất trong DB
        ClassOpeningRequest openingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đề xuất mở lớp học phần này!"));

        if (openingRequest.getStatus() != ClassOpenningStatus.PENDING) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Đơn đề xuất này đã được xử lý rồi, không thể chỉnh sửa!");
        }

        // 2. XỬ LÝ NHÁNH TỪ CHỐI (REJECTED)
        if (dto.getStatus() == ClassOpenningStatus.REJECTED) {
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do từ chối đề xuất mở lớp!");
            }
            openingRequest.setStatus(ClassOpenningStatus.REJECTED);
            openingRequest.setRejectReason(dto.getRejectReason());
            openingRequest.setUpdatedAt(LocalDateTime.now());
            requestRepository.save(openingRequest);
            log.info("🔴 Đã từ chối đơn đề xuất mở lớp ID: {}. Lý do: {}", requestId, dto.getRejectReason());
            return;
        }

        // 3. XỬ LÝ NHÁNH PHÊ DUYỆT (APPROVED)
        if (dto.getStatus() == ClassOpenningStatus.APPROVED) {
            // Validate dữ liệu từ Form gửi lên
            if (!userRepository.existsById(dto.getManagerId())) {
                throw new CustomException(HttpStatus.NOT_FOUND, "Không thể gán lớp! Trưởng khoa được chọn không tồn tại.");
            }
            // 🛡️ CHỐT CHẶN VÀNG: Kiểm tra trùng lịch phòng học (Tránh đụng độ TKB)
            boolean isRoomOccupied = classScheduleRepository.existsByRoomIdAndDayOfWeekAndShiftId(
                    dto.getRoomId(), dto.getDayOfWeek(), dto.getShiftId()
            );
            if (isRoomOccupied) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Xung đột lịch học! Phòng học này vào Thứ " + dto.getDayOfWeek() + " - Ca " + dto.getShiftId() + " đã có lớp khác sử dụng.");
            }

            // Cập nhật trạng thái đơn đề xuất sang APPROVED
            openingRequest.setStatus(ClassOpenningStatus.APPROVED);
            openingRequest.setUpdatedAt(LocalDateTime.now());
            requestRepository.save(openingRequest);

            // 4. TIẾN HÀNH SINH LỚP HỌC PHẦN CHÍNH THỨC (`classes`)
            String courseCode = courseRepository.findById(openingRequest.getCourseId())
                    .map(Course::getCode)
                    .orElse("MONHOC");
            String semesterCode = openingRequest.getSemester().getSemesterCode();

            // Đếm số lớp hiện tại của môn đó trong kỳ để sinh số thứ tự (Ví dụ: KTPM-HK1-L01)
            long currentClassCount = classRepository.countBySemesterIdAndCourseId(openingRequest.getSemester().getId(), openingRequest.getCourseId());
            String autoClassCode = String.format("%s-%s-L%02d", courseCode, semesterCode, currentClassCount + 1);

            ClassEntity officialClass = ClassEntity.builder()
                    .semester(openingRequest.getSemester())
                    .courseId(openingRequest.getCourseId())
                    .managerId(dto.getManagerId())
                    .lecturerId(null)       // Giảng viên được chọn từ Form
                    .code(autoClassCode)
                    .maxStudents(openingRequest.getExpectedStudents())
                    .status(ClassStatus.PENDING)    // Chờ cổng đăng ký học phần mở
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            ClassEntity savedClass = classRepository.save(officialClass);

            Room cleanRoom = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Phòng học không tồn tại!"));

            Shift cleanShift = shiftRepository.findById(dto.getShiftId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Ca học không tồn tại!"));
            // 5. TỰ ĐỘNG LƯU LỊCH HỌC VÀO THỜI KHÓA BIỂU (`class_schedules`)
            ClassSchedule schedule = ClassSchedule.builder()
                    .classEntity(savedClass)
                    .room(cleanRoom)
                    .shift(cleanShift)
                    .dayOfWeek(dto.getDayOfWeek()) // Thứ từ Form
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            classScheduleRepository.save(schedule);
            log.info("🟢 Phê duyệt thành công đơn ID: {}. Đã tạo lớp {} và ghim lịch học thành công!", requestId, autoClassCode);
        }
    }
}