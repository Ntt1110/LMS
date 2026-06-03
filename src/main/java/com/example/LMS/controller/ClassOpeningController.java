package com.example.LMS.controller;

import com.example.LMS.dto.request.ClassOpeningRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.service.ClassOpeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}