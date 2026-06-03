package com.example.LMS.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ClassOpeningRequestDto {

    @NotNull(message = "Học kỳ không được để trống!")
    private Long semesterId;

    @NotNull(message = "Môn học không được để trống!")
    private Long courseId;

    @NotNull(message = "Số lượng sinh viên dự kiến không được để trống!")
    @Min(value = 10, message = "Một lớp học phần phải có tối thiểu 10 sinh viên!")
    private Integer expectedStudents;

    private String note;
}
