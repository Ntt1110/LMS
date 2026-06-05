package com.example.LMS.service;

import com.example.LMS.dto.response.DepartmentResponse;
import com.example.LMS.entity.model.Department;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // ============================================================
    // DANH SÁCH KHOA (dùng cho dropdown)
    // ============================================================
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ============================================================
    // XEM CHI TIẾT KHOA
    // ============================================================
    public DepartmentResponse getDepartmentById(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khoa với id: " + id
                ));

        return DepartmentResponse.fromEntity(department);
    }
}