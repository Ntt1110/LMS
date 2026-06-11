package com.example.LMS.service;

import com.example.LMS.dto.request.CreateMajorRequest;
import com.example.LMS.dto.request.MajorListRequest;
import com.example.LMS.dto.request.UpdateMajorRequest;
import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.dto.response.MajorResponse;
import com.example.LMS.entity.model.Department;
import com.example.LMS.entity.model.Major;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.DepartmentRepository;
import com.example.LMS.repository.MajorRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MajorService {

    private final MajorRepository majorRepository;
    private final DepartmentRepository departmentRepository;

    // ============================================================
    // TẠO NGÀNH HỌC
    // ============================================================
    @Transactional
    public MajorResponse createMajor(CreateMajorRequest request) {
        log.info("⏳ Đang tạo ngành học mới: {}", request.getCode());

        // 1. Kiểm tra mã ngành đã tồn tại chưa
        if (majorRepository.existsByCode(request.getCode().trim())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Mã ngành '" + request.getCode() + "' đã tồn tại trong hệ thống!");
        }

        // 2. Kiểm tra khoa tồn tại
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy khoa với id: " + request.getDepartmentId()));

        // 3. Tạo mới ngành học
        Major major = Major.builder()
                .department(department)
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .requiredMinimumCredits(request.getRequiredMinimumCredits())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isActive(true) // Mặc định kích hoạt luôn khi tạo mới
                .build();

        Major saved = majorRepository.save(major);
        log.info("✅ Tạo ngành học thành công: {} - {}", saved.getCode(), saved.getName());

        return MajorResponse.fromEntity(saved);
    }

    // ============================================================
    // SỬA NGÀNH HỌC
    // ============================================================
    @Transactional
    public MajorResponse updateMajor(Long id, UpdateMajorRequest request) {
        log.info("⏳ Đang cập nhật ngành học ID: {}", id);

        // 1. Tìm ngành học, chưa bị xóa mềm
        Major major = majorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy ngành học với id: " + id));

        // 2. Cập nhật khoa nếu có
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy khoa với id: " + request.getDepartmentId()));
            major.setDepartment(department);
        }

        // 3. Cập nhật các trường được gửi lên (null = giữ nguyên)
        if (request.getName() != null && !request.getName().isBlank())
            major.setName(request.getName().trim());

        if (request.getRequiredMinimumCredits() != null)
            major.setRequiredMinimumCredits(request.getRequiredMinimumCredits());

        if (request.getDescription() != null)
            major.setDescription(request.getDescription().trim());

        if (request.getIsActive() != null) {
            major.setIsActive(request.getIsActive());
            // Nếu kích hoạt lại thì xóa lý do khóa
            if (request.getIsActive()) {
                major.setLockReason(null);
            }
        }

        // lockReason chỉ cập nhật khi isActive = false
        if (request.getLockReason() != null && Boolean.FALSE.equals(major.getIsActive()))
            major.setLockReason(request.getLockReason().trim());

        Major saved = majorRepository.save(major);
        log.info("✅ Cập nhật ngành học thành công: {}", saved.getCode());

        return MajorResponse.fromEntity(saved);
    }

    // ============================================================
    // XÓA NGÀNH HỌC (xóa mềm)
    // ============================================================
    @Transactional
    public void deleteMajor(Long id) {
        log.info("⏳ Đang xóa mềm ngành học ID: {}", id);

        Major major = majorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy ngành học với id: " + id));

        major.setDeletedAt(java.time.LocalDateTime.now());
        majorRepository.save(major);

        log.info("✅ Đã xóa mềm ngành học: {} - {}", major.getCode(), major.getName());
    }

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