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

    private Long id;       // ID thực thể dùng để bọc gửi dữ liệu (roomId, shiftId, userId...)

    private String name;   // Tên hiển thị sạch sẽ trên UI Dropdown (Ví dụ: "Phòng A1", "Ca 1 (Sáng)", "Nguyễn Thành Trung")
}