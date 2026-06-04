package com.example.LMS.controller;

import com.example.LMS.dto.response.ApiResponse;
import com.example.LMS.dto.response.ScheduleResponse;
import com.example.LMS.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Thời khóa biểu")
@SecurityRequirement(name = "bearerAuth")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    @Operation(summary = "Xem thời khóa biểu của tôi",
            description = "Trả về lịch học của các lớp sinh viên đã đăng ký")
    public ApiResponse<List<ScheduleResponse>> getMySchedule() {
        return ApiResponse.<List<ScheduleResponse>>builder()
                .code(200)
                .message("Tải thời khóa biểu thành công!")
                .data(scheduleService.getMySchedule())
                .build();
    }
}