package com.example.LMS.controller;

import com.example.LMS.dto.request.ClassListRequest;
import com.example.LMS.dto.response.ClassDetailResponse;
import com.example.LMS.dto.response.ClassDetailForStudentResponse;
import com.example.LMS.dto.response.LecturerClassResponse;
import com.example.LMS.dto.response.LecturerClassDetailResponse;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "Class Management", description = "Quản lý lớp học phần")
@SecurityRequirement(name = "bearerAuth")
public class ClassController {

    private final ClassService classService;

    // ============================================================
    // GET /api/v1/classes
    // Danh sách lớp học phần — Phòng đào tạo thấy hết, Trưởng khoa chỉ thấy lớp mình
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TRAINING_DEPT', 'ROLE_HEAD_OF_DEPT')")
    @Operation(
            summary = "Danh sách lớp học phần",
            description = "Tìm theo mã lớp/tên môn, lọc theo học kỳ, trạng thái, khoa. " +
                    "Phòng đào tạo thấy tất cả. Trưởng khoa chỉ thấy lớp mình quản lý."
    )
    public ResponseEntity<Page<ClassDetailResponse>> getClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        ClassListRequest request = new ClassListRequest();
        request.setKeyword(keyword);
        request.setSemesterId(semesterId);
        request.setDepartmentId(departmentId);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(classService.getClasses(request));
    }

    // ============================================================
    // GET /api/v1/classes/{classId}/student-detail
    // Chi tiết lớp học phần dành cho sinh viên xem trước khi đăng ký
    // Trả về: giảng viên, ca học, phòng học, học thứ mấy, sĩ số hiện tại
    // ============================================================
    @GetMapping("/{classId}/student-detail")
    @PreAuthorize("hasAuthority('COURSE_CLASS_VIEW')")
    @Operation(summary = "Chi tiết lớp học phần (sinh viên)")
    public ApiResponse<ClassDetailForStudentResponse> getClassDetailForStudent(@PathVariable Long classId) {
        return ApiResponse.<ClassDetailForStudentResponse>builder()
                .code(200)
                .message("Tải chi tiết lớp học phần thành công!")
                .data(classService.getClassDetailForStudent(classId))
                .build();
    }

    // ============================================================
    // GET /api/v1/classes/my-assigned
    // Danh sách lớp được phân công — dành cho Giảng viên
    // ============================================================
    @GetMapping("/my-assigned")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    @Operation(summary = "Danh sách lớp được phân công (giảng viên)")
    public ApiResponse<List<LecturerClassResponse>> getMyAssignedClasses(
            @RequestParam(required = false) String status) {
        return ApiResponse.<List<LecturerClassResponse>>builder()
                .code(200)
                .message("Tải danh sách lớp được phân công thành công!")
                .data(classService.getMyAssignedClasses(status))
                .build();
    }

    @GetMapping("/{classId}/detail-with-students")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    @Operation(summary = "Chi tiết lớp + danh sách sinh viên (giảng viên)")
    public ApiResponse<LecturerClassDetailResponse> getAssignedClassDetail(@PathVariable Long classId) {
        return ApiResponse.<LecturerClassDetailResponse>builder()
                .code(200)
                .message("Tải chi tiết lớp học phần thành công!")
                .data(classService.getAssignedClassDetail(classId))
                .build();
    }
}