package com.example.LMS.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// @JsonInclude giúp loại bỏ các trường bị null khỏi cục JSON trả về (cho data gọn nhẹ)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private int code = 200; // Mã trạng thái nội bộ hoặc HTTP Status

    private String message; // Thông báo (VD: "Đăng nhập thành công", "Lấy dữ liệu thành công")

    private T data; // Kiểu Generic (T): Có thể nhét bất kỳ loại object nào vào đây (User, List<Course>, Token...)
}