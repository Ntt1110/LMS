package com.example.LMS.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalizeGradeRequest {

    @NotNull(message = "ID sinh viên không được để trống")
    private Long studentId;

    @NotNull(message = "Điểm TX1 không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm TX1 phải >= 0")
    @DecimalMax(value = "10.0", message = "Điểm TX1 phải <= 10")
    private Double regularScore1;

    @NotNull(message = "Điểm TX2 không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm TX2 phải >= 0")
    @DecimalMax(value = "10.0", message = "Điểm TX2 phải <= 10")
    private Double regularScore2;

    @NotNull(message = "Điểm giữa kỳ không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm giữa kỳ phải >= 0")
    @DecimalMax(value = "10.0", message = "Điểm giữa kỳ phải <= 10")
    private Double midtermScore;

    @NotNull(message = "Điểm cuối kỳ không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm cuối kỳ phải >= 0")
    @DecimalMax(value = "10.0", message = "Điểm cuối kỳ phải <= 10")
    private Double finalScore;
}