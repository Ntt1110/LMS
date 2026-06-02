package com.example.LMS.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CourseListRequest {

    //Tim kiem theo ten khoa hoc hoac ma khoa hoc
    private String keyword;


    //Loc theo khoa
    private Long departmentId;

    // Phan trang
    private int page = 0;
    private int size = 10;

    // Sắp xếp
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}
