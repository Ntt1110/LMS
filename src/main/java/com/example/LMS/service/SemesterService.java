package com.example.LMS.service;

import com.example.LMS.dto.request.SemesterListRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;

    // ============================================================
    // DANH SÁCH HỌC KỲ
    // ============================================================
    public Page<SemesterResponse> getSemesters(SemesterListRequest request) {

        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Specification<Semester> spec = buildSpecification(request);

        return semesterRepository.findAll(spec, pageable)
                .map(SemesterResponse::fromEntity);
    }

    // ============================================================
    // XEM CHI TIẾT HỌC KỲ
    // ============================================================
    public SemesterResponse getSemesterById(Long id) {

        Semester semester = semesterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy học kỳ với id: " + id
                ));

        return SemesterResponse.fromEntity(semester);
    }

    // ============================================================
    // SPECIFICATION (dynamic filter)
    // ============================================================
    private Specification<Semester> buildSpecification(SemesterListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Filter theo keyword: tìm trong semesterCode hoặc academicYear
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("semesterCode")), pattern),
                        cb.like(cb.lower(root.get("academicYear")), pattern)
                ));
            }

            // Filter theo status: ACTIVE | CLOSED
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                try {
                    Semester.SemesterStatus status =
                            Semester.SemesterStatus.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Filter theo academicYear
            if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
                predicates.add(cb.equal(root.get("academicYear"), request.getAcademicYear()));
            }

            // Chỉ lấy học kỳ chưa bị xóa mềm
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}