package com.example.LMS.dto.request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationPeriodListRequest {

    // Tìm kiếm theo tên
    private String keyword;

    // Lọc theo học kỳ
    private Long semesterId;

    // Lọc theo trạng thái: PENDING | ACTIVE | CLOSED
    private String status;

    // Phân trang
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}
