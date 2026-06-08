package com.example.LMS.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ScheduleResponse {
    private String courseName;      // Tên môn
    private String classCode;       // Mã lớp
    private String roomName;        // Tên phòng
    private Integer dayOfWeek;      // Ngày trong tuần (2=Thứ 2...)
    private String shiftName;       // Ca học
    private LocalTime startTime;    // Thời gian bắt đầu
    private LocalTime endTime;      // Thời gian kết thúc
    private String lecturerName;    // Tên giảng viên (null với giảng viên)
    private Integer totalWeeks;
    private LocalDateTime startDate;// Thời khóa biểu kéo dài bao nhiêu tuần
}