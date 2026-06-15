package com.example.LMS.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DropdownResponseDto {

    private Long id;       // ID thực thể

    private String name;   // Tên hiển thị trên UI
}