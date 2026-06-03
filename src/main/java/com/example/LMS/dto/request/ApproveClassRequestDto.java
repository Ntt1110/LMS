package com.example.LMS.dto.request;

import com.example.LMS.entity.Enum.ClassOpenningStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveClassRequestDto {

    @NotNull(message = "Trạng thái quyết định không được để trống!")
    private ClassOpenningStatus status;

    private String rejectReason;

    // --- ĐỔI TỪ LECTURER THÀNH MANAGER THEO YÊU CẦU CỦA TRUNG ---
    @NotNull(message = "Vui lòng chọn Trưởng khoa/Trưởng bộ môn quản lý lớp!")
    private Long managerId;     // ID Trưởng khoa được chọn từ Dropdown

    @NotNull(message = "Vui lòng chọn Phòng học!")
    private Long roomId;

    @NotNull(message = "Vui lòng chọn Ca học!")
    private Long shiftId;

    @NotNull(message = "Vui lòng chọn Thứ trong tuần!")
    private Integer dayOfWeek;
}