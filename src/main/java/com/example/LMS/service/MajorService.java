package com.example.LMS.service;

import com.example.LMS.dto.request.MajorListRequest;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.dto.response.MajorResponse;
import com.example.LMS.entity.model.Major;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.MajorRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorRepository majorRepository;

    // ============================================================
    // DANH SÁCH NGÀNH HỌC
    // ============================================================
    public Page<MajorResponse> getMajors(MajorListRequest request) {

        // 1. Pageable
        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 2. Specification (dynamic filter)
        Specification<Major> spec = buildSpecification(request);

        // 3. Query + map sang DTO
        return majorRepository.findAll(spec, pageable)
                .map(MajorResponse::fromEntity);
    }

    // ============================================================
    // DROPDOWN NGÀNH HỌC (chỉ trả id + name, dùng cho select/dropdown)
    // Filter theo departmentId nếu cần lọc theo khoa cụ thể
    // ============================================================
    public List<DropdownResponseDto> getMajorsDropdown(Long departmentId) {
        if (departmentId != null) {
            return majorRepository.findDropdownByDepartmentId(departmentId);
        }
        return majorRepository.findAllMajorsDropdown();
    }

    // ============================================================
    // XEM CHI TIẾT NGÀNH HỌC
    // ============================================================
    public MajorResponse getMajorById(Long id) {

        Major major = majorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy ngành học với id: " + id
                ));

        return MajorResponse.fromEntity(major);
    }

    // ============================================================
    // SPECIFICATION (dynamic filter)
    // ============================================================
    private Specification<Major> buildSpecification(MajorListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Filter theo keyword: tìm trong code hoặc name
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            // Filter theo isActive
            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            // Filter theo departmentId (JOIN sang departments)
            if (request.getDepartmentId() != null) {
                Join<Object, Object> deptJoin = root.join("department", JoinType.INNER);
                predicates.add(cb.equal(deptJoin.get("id"), request.getDepartmentId()));
            }

            // Chỉ lấy ngành chưa bị xóa mềm
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}