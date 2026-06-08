package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.example.LMS.entity.Enum.ClassOpenningStatus;
import jakarta.validation.constraints.NotNull;

@Data
public class RejectClassRequestDto {
    @NotBlank(message = "Vui lòng nhập lý do từ chối đề xuất mở lớp!")
    private String rejectReason;
}
