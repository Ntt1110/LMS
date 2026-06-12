package com.example.LMS.controller;

import com.example.LMS.dto.request.CourseApproveRequest;
import com.example.LMS.dto.request.CourseListRequest;
import com.example.LMS.dto.request.CourseProposalRequest;
import com.example.LMS.dto.request.CourseRejectRequest;
import com.example.LMS.dto.request.UpdateCourseRequest;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.CourseResponse;
import com.example.LMS.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Quản lý môn học")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;

    // ============================================================
    // GET /api/v1/courses
    // Danh sách môn học (chỉ APPROVED)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @Operation(summary = "Danh sách môn học",
            description = "Chỉ trả về môn học đã APPROVED. Filter theo keyword (code/name), departmentId.")
    public ResponseEntity<Page<CourseResponse>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection,
            @RequestParam(required = false) String status
    ) {
        CourseListRequest request = new CourseListRequest();
        request.setKeyword(keyword);
        request.setDepartmentId(departmentId);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        request.setStatus(status);

        return ResponseEntity.ok(courseService.getCourses(request));
    }

    // ============================================================
    // GET /api/v1/courses/{id}
    // Chi tiết môn học
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @Operation(summary = "Chi tiết môn học")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    // ============================================================
    // POST /api/v1/courses/propose
    // Đề xuất môn học mới
    // ============================================================
    @PostMapping("/propose")
    @PreAuthorize("hasAuthority('COURSE_PROPOSE')")
    @Operation(summary = "Đề xuất môn học",
            description = "Tạo yêu cầu thêm môn học mới. Môn sẽ ở trạng thái PENDING chờ duyệt.")
    public ResponseEntity<CourseResponse> proposeCourse(@Valid @RequestBody CourseProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.proposeCourse(request));
    }

    // ============================================================
    // POST /api/v1/courses/approve
    // Duyệt môn học
    // ============================================================
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('COURSE_APPROVE')")
    @Operation(summary = "Duyệt môn học",
            description = "Phê duyệt môn học đang ở trạng thái PENDING.")
    public ResponseEntity<CourseResponse> approveCourse(@Valid @RequestBody CourseApproveRequest request) {
        return ResponseEntity.ok(courseService.approveCourse(request));
    }

    // ============================================================
    // POST /api/v1/courses/reject
    // Từ chối môn học
    // ============================================================
    @PostMapping("/reject")
    @PreAuthorize("hasAuthority('COURSE_APPROVE')")
    @Operation(summary = "Từ chối môn học",
            description = "Từ chối môn học đang ở trạng thái PENDING. Bắt buộc phải có lý do.")
    public ResponseEntity<CourseResponse> rejectCourse(@Valid @RequestBody CourseRejectRequest request) {
        return ResponseEntity.ok(courseService.rejectCourse(request));
    }
    // ============================================================
    // PUT /api/v1/courses/{id}
    // Sửa môn học
    // ============================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_EDIT')")
    @Operation(
            summary = "Sửa môn học",
            description = "Cập nhật thông tin môn học (tên, số tín chỉ, số tiết, mô tả, khoa). " +
                    "Không cho phép đổi mã môn học. Chỉ sửa được môn học ở trạng thái PENDING hoặc APPROVED."
    )
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CourseResponse>builder()
                        .code(200)
                        .message("Cập nhật môn học thành công!")
                        .data(courseService.updateCourse(id, request))
                        .build()
        );
    }

    // ============================================================
    // DELETE /api/v1/courses/{id}
    // Xóa mềm môn học (soft delete — set deleted_at = now())
    // ============================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_DELETE')")
    @Operation(
            summary = "Xóa mềm môn học",
            description = "Đánh dấu môn học là đã xóa (set deleted_at). Dữ liệu vẫn còn trong DB, không xóa vật lý."
    )
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(200)
                        .message("Xóa môn học thành công!")
                        .build()
        );
    }


    // ============================================================
// GET /api/v1/courses/approved
// Danh sách môn học đã duyệt
// ============================================================
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @Operation(summary = "Danh sách môn học đã duyệt (APPROVED)", description = "Filter theo keyword (code/name), departmentId.")
    @GetMapping("/approved")
    public ResponseEntity<Page<CourseResponse>> getApprovedCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        CourseListRequest request = new CourseListRequest();
        request.setKeyword(keyword);
        request.setDepartmentId(departmentId);
        request.setStatus("APPROVED");   // cố định APPROVED
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        return ResponseEntity.ok(courseService.getCourses(request));
    }

    // ============================================================
// GET /api/v1/courses/my-department
// Danh sách môn học thuộc khoa của trưởng khoa đang login
// ============================================================
    @GetMapping("/my-department")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    @Operation(summary = "Danh sách môn học theo khoa của trưởng khoa đang login")
    public ResponseEntity<Page<CourseResponse>> getCoursesByHeadOfDept(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDirection
    ) {
        return ResponseEntity.ok(courseService.getCoursesByHeadOfDept(page, size, sortBy, sortDirection));
    }
}