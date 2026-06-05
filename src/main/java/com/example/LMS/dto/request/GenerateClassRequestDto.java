package com.example.LMS.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateClassRequestDto {
    @NotNull(message = "Vui lòng chọn Trưởng khoa/Trưởng bộ môn quản lý lớp!")
    private Long managerId;

    @NotNull(message = "Vui lòng chọn Phòng học cho lớp đầu tiên!")
    private Long roomId;

    @NotNull(message = "Vui lòng chọn Ca học cho lớp đầu tiên!")
    private Long shiftId;

    @NotNull(message = "Vui lòng chọn Thứ trong tuần!")
    private Integer dayOfWeek;

    @NotNull(message = "Vui lòng nhập số lượng lớp cần mở!")
    @Min(value = 1, message = "Số lượng lớp mở tối thiểu phải là 1 lớp!")
    private Integer numberOfClasses;
}
