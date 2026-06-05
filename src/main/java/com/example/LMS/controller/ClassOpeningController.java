package com.example.LMS.controller;

import com.example.LMS.dto.request.*;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.ClassOpeningResponseDto;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.entity.model.ClassOpeningRequest;
import com.example.LMS.service.ClassOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.LMS.dto.response.CourseWithClassesResponse;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-requests")
@RequiredArgsConstructor
@Slf4j
public class ClassOpeningController {

    private final ClassOpeningService classOpeningService;

    @PostMapping("/propose")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE')") // 🛡️ Chỉ những ai có quyền gán mã số 21 mới được bấm nút đề xuất
    public ApiResponse<String> proposeClass(@Valid @RequestBody ClassOpeningRequestDto requestDto) {

        classOpeningService.createOpeningRequest(requestDto);

        return ApiResponse.<String>builder()
                .code(201)
                .message("Gửi đề xuất mở lớp học phần thành công! Vui lòng chờ phê duyệt.")
                .data("Submitted Successfully")
                .build();
    }
    @GetMapping("/dropdown/dean-courses")
    @PreAuthorize("hasAuthority('CLASS_VIEW')") // Quyền số 20
    @Operation(summary = "Lấy danh sách Môn học thuộc Khoa/Ngành của Trưởng khoa đang đăng nhập")
    public ApiResponse<List<DropdownResponseDto>> getCoursesForDeanProposal(
            Authentication authentication // 🛡️ Bốc trực tiếp hệ thống chứng thực lõi của Spring Security
    ) {
        // Lấy username (Mã số hoặc Email giảng viên đăng nhập găm trong Token)
        String currentUsername = authentication.getName();

        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách môn học thuộc khoa quản lý thành công!")
                .data(classOpeningService.getCoursesDropdownForDean(currentUsername)) // Đẩy sang Service xử lý
                .build();
    }

    @GetMapping("/dropdown/majors")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Lấy danh sách Ngành học (Dành cho Dropdown Form đề xuất)")
    public ApiResponse<List<DropdownResponseDto>> getMajorsForProposal() {
        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách ngành học thành công!")
                .data(classOpeningService.getMajorsDropdown())
                .build();
    }

    @GetMapping("/pending-list")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public ApiResponse<Page<ClassOpeningResponseDto>> getPendingRequests(RequestFilterDto filterDto) {

        // 🌟 Biến pagingData hứng dữ liệu lúc này phải mang kiểu DTO phẳng sạch sẽ
        Page<ClassOpeningResponseDto> pagingData = classOpeningService.getPagingRequests(filterDto);

        return ApiResponse.<Page<ClassOpeningResponseDto>>builder()
                .code(200)
                .message("Tải danh sách đơn đề xuất mở lớp thành công!")
                .data(pagingData)
                .build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE_VIEW')") // 🛡️ CHỐT CHẶN MÃ QUYỀN SỐ 22 CHI TIẾT CỦA TRUNG!
    @Operation(summary = "Lấy danh sách các đề xuất mở lớp đang chờ duyệt (Dành cho Phòng Đào tạo)")
    public ApiResponse<List<ClassOpeningResponseDto>> getPendingRequests() {

        List<ClassOpeningResponseDto> data = classOpeningService.getPendingOpeningRequests();

        return ApiResponse.<List<ClassOpeningResponseDto>>builder()
                .code(200)
                .message("Tải danh sách lớp chờ phê duyệt thành công!")
                .data(data)
                .build();
    }

    @GetMapping("/{requestId}/detail")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE_VIEW')")
    public ApiResponse<ClassOpeningResponseDto> getRequestDetail(@PathVariable Long requestId) {
        return ApiResponse.<ClassOpeningResponseDto>builder()
                .code(200)
                .message("Tải chi tiết đơn đề xuất thành công!")
                .data(classOpeningService.getRequestDetail(requestId))
                .build();
    }

    @GetMapping("/dropdown/lecturers")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE_VIEW')")
    public ApiResponse<List<DropdownResponseDto>> getLecturers() {
        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách giảng viên thành công!")
                .data(classOpeningService.getLecturersDropdown())
                .build();
    }

    @GetMapping("/dropdown/rooms")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE_VIEW')")
    public ApiResponse<List<DropdownResponseDto>> getRooms() {
        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách phòng học thành công!")
                .data(classOpeningService.getRoomsDropdown())
                .build();
    }

    @GetMapping("/dropdown/shifts")
    @PreAuthorize("hasAuthority('CLASS_PROPOSE_VIEW')")
    public ApiResponse<List<DropdownResponseDto>> getShifts() {
        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách ca học thành công!")
                .data(classOpeningService.getShiftsDropdown())
                .build();
    }

    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasAuthority('CLASS_REJECT')")
    @Operation(summary = "Từ chối đơn đề xuất mở lớp học phần")
    public ApiResponse<String> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectClassRequestDto rejectDto) {

        classOpeningService.rejectOpeningRequest(requestId, rejectDto);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Đã từ chối đơn đề xuất mở lớp học phần thành công!")
                .data("REJECTED_SUCCESSFULLY")
                .build();
    }

    // 🌟 API 1B: CHỈ XỬ LÝ PHÊ DUYỆT ĐƠN HÀNH CHÍNH (CHƯA SINH LỚP)
    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasAuthority('CLASS_APPROVE')")
    @Operation(summary = "Phê duyệt đơn đề xuất mở lớp - Chuyển trạng thái đơn sang APPROVED")
    public ApiResponse<String> approveRequest(@PathVariable Long requestId) {

        classOpeningService.approveOpeningRequest(requestId);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Đã phê duyệt đơn đề xuất hành chính! Đơn hiện tại đã mang nhãn APPROVED.")
                .data("APPROVED_SUCCESSFULLY")
                .build();
    }
    @PutMapping("/{requestId}/generate-classes")
    @PreAuthorize("hasAuthority('CLASS_APPROVE')")
    @Operation(summary = "Khởi tạo loạt lớp học phần trống và ghim lịch học ban đầu dựa theo số lượng")
    public ApiResponse<String> generateClasses(
            @PathVariable Long requestId,
            @Valid @RequestBody GenerateClassRequestDto generateDto) {

        classOpeningService.generateClassesFromRequest(requestId, generateDto);

        // 🌟 SỬA CHÍNH XÁC DÒNG NÀY: Thay generateDto.getNumberOfClasses() bằng generateDto.getClasses().size()
        int totalClassesCreated = (generateDto.getClasses() != null) ? generateDto.getClasses().size() : 0;

        return ApiResponse.<String>builder()
                .code(200)
                .message("Hệ thống đã tự động sinh khởi tạo thành công " + totalClassesCreated + " lớp học phần trống!")
                .data("CLASSES_GENERATED_SUCCESSFULLY")
                .build();
    }

    @GetMapping("/classes/{classId}/dropdown/instructors")
    @PreAuthorize("hasAuthority('CLASS_VIEW')") // Quyền số 20
    @Operation(summary = "Lấy danh sách giảng viên thuộc khoa của môn học (Phục vụ Dropdown phân công)")
    public ApiResponse<List<DropdownResponseDto>> getInstructors(@PathVariable Long classId) {
        return ApiResponse.<List<DropdownResponseDto>>builder()
                .code(200)
                .message("Tải danh sách giảng viên trực thuộc khoa thành công!")
                .data(classOpeningService.getInstructorsDropdown(classId))
                .build();
    }

    @PutMapping("/classes/{classId}/assign-lecturer")
    @PreAuthorize("hasAuthority('CLASS_ASSIGN_TEACHER')") // 🛡️ CHỐT CHẶN MÃ QUYỀN SỐ 17
    @io.swagger.v3.oas.annotations.Operation(summary = "Phân công giảng viên phụ trách lớp học phần (Dành cho Trưởng khoa)")
    public ApiResponse<String> assignLecturer(
            @PathVariable Long classId,
            @Valid @RequestBody AssignLecturerDto assignLecturerDto) {

        classOpeningService.assignLecturerToClass(classId, assignLecturerDto);

        return ApiResponse.<String>builder()
                .code(200)
                .message("Phân công giảng viên phụ trách lớp học phần thành công!")
                .data("Assigned Successfully")
                 .build();
    }

        // Thêm endpoint phần xem danh sách môn học và lớp học cho Sinh viên
        @GetMapping("/courses-with-classes")
        @PreAuthorize("hasAuthority('COURSE_CLASS_VIEW')")
        @Operation(summary = "Danh sách môn học kèm lớp học phần",
                description = "Dành cho sinh viên xem để đăng ký học phần")
        public ApiResponse<Page<CourseWithClassesResponse>> getCoursesWithClasses (
                @RequestParam(required = false) String keyword,
                @RequestParam(required = false) Long departmentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ){
            return ApiResponse.<Page<CourseWithClassesResponse>>builder()
                    .code(200)
                    .message("Tải danh sách môn học và lớp học thành công!")
                    .data(classOpeningService.getCoursesWithClasses(keyword, departmentId, page, size))
                    .build();
        }
    }

