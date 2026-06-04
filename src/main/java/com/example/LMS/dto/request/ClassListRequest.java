package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassListRequest {
    private String keyword;
    private Long semesterId;
    private Long departmentId;
    private String status;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}   