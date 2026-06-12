package com.example.LMS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockUserRequest {

    @NotBlank(message = "Lý do khóa không được để trống")
    private String lockReason;
}   