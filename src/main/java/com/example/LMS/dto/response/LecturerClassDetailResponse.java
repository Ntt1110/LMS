package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

// Chi tiết lớp dành cho giảng viên
@Getter
@Builder
public class LecturerClassDetailResponse {

    // Thông tin lớp
    private Long classId;
    private String classCode;
    private String courseName;
    private String courseCode;
    private Integer credits;
    private String status;
    private Integer maxStudents;
    private Integer currentStudents;

    // ✅ Thay 3 field đơn lẻ bằng full list TKB
    private List<ScheduleInfo> schedules;

    @Getter
    @Builder
    public static class ScheduleInfo {
        private Long scheduleId;
        private Integer dayOfWeek;   // 2=Thứ 2 ... 7=Thứ 7, 8=Chủ nhật
        private String shiftName;    // Ca 1 (Sáng)...
        private LocalTime startTime;
        private LocalTime endTime;
        private String roomName;
        private String roomType;
    }
}