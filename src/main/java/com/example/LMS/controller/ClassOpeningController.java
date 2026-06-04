package com.example.LMS.controller;

import com.example.LMS.dto.request.ApproveClassRequestDto;
import com.example.LMS.dto.request.AssignLecturerDto;
import com.example.LMS.dto.request.ClassOpeningRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.ClassOpeningResponseDto;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.service.ClassOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.LMS.dto.response.CourseWithClassesResponse;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-requests")
@RequiredArgsConstructor
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

    @PutMapping("/{requestId}/review")
    @PreAuthorize("hasAuthority('CLASS_APPROVE') or hasAuthority('CLASS_REJECT')")
    // 🛡️ BẢO VỆ CHẶT CHẼ BẰNG QUYỀN 23 HOẶC 24
    @Operation(summary = "Phê duyệt hoặc Từ chối đơn đề xuất - Chốt dữ liệu từ Form xếp lịch")
    public ApiResponse<String> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApproveClassRequestDto reviewDto) {

        classOpeningService.reviewOpeningRequest(requestId, reviewDto);

        String actionMessage = reviewDto.getStatus() == ClassOpenningStatus.APPROVED ?
                "Đã phê duyệt, khởi tạo lớp học phần và xếp Thời khóa biểu thành công!" : "Đã từ chối đơn đề xuất mở lớp.";

        return ApiResponse.<String>builder()
                .code(200)
                .message(actionMessage)
                .data(reviewDto.getStatus().name())
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

