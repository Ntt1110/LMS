package com.example.LMS.controller;

import com.example.LMS.dto.request.CourseApproveRequest;
import com.example.LMS.dto.request.CourseListRequest;
import com.example.LMS.dto.request.CourseProposalRequest;
import com.example.LMS.dto.request.CourseRejectRequest;
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
}