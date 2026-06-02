package com.example.LMS.service;

import com.example.LMS.dto.response.DepartmentResponse;
import com.example.LMS.entity.model.Department;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // ============================================================
    // DANH SÁCH KHOA (không phân trang vì thường ít, dùng để dropdown)
    // ============================================================
    public List<DepartmentResponse> getDepartments(String keyword, Boolean isActive) {

        Specification<Department> spec = buildSpecification(keyword, isActive);

        return departmentRepository.findAll(spec)
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

    // ============================================================
    // SPECIFICATION
    // ============================================================
    private Specification<Department> buildSpecification(String keyword, Boolean isActive) {
        return (root, query, cb) -> {

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Tìm theo code hoặc name
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            // Lọc theo trạng thái
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}