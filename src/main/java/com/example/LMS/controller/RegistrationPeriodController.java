package com.example.LMS.controller;

import com.example.LMS.dto.request.RegistrationPeriodRequestDto;
import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.service.RegistrationPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registration-periods")
@RequiredArgsConstructor
public class RegistrationPeriodController {

    private final RegistrationPeriodService periodService;

    @PostMapping("/open")
    @PreAuthorize("hasAuthority('CLASS_OPEN_REG')") // 🛡️ CHỐT CHẶN MÃ QUYỀN SỐ 27 CỦA TRUNG!
    @io.swagger.v3.oas.annotations.Operation(summary = "Mở đợt đăng ký học phần mới (Dành cho Phòng Đào tạo)")
    public ApiResponse<String> openRegistration(@Valid @RequestBody RegistrationPeriodRequestDto requestDto) {

        periodService.createRegistrationPeriod(requestDto);

        return ApiResponse.<String>builder()
                .code(201)
                .message("Khởi tạo đợt mở đăng ký học phần và cấu hình đối tượng đích thành công!")
                .data("Created Successfully")
                .build();
    }
}