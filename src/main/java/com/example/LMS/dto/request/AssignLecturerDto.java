package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignLecturerDto {

    @NotNull(message = "Vui lòng chọn giảng viên phụ trách lớp học!")
    private Long lecturerId;
}