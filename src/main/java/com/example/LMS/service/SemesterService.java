package com.example.LMS.service;

import com.example.LMS.dto.request.SemesterCreateRequest;
import com.example.LMS.dto.response.SemesterResponse;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;

    // ============================================================
    // DANH SÁCH HỌC KỲ (dùng cho dropdown)
    // Chỉ trả về id, semesterCode, academicYear — không phân trang
    // ============================================================
    public List<SemesterResponse> getSemesters() {
        return semesterRepository.findAllByDeletedAtIsNullOrderByAcademicYearDescSemesterCodeAsc()
                .stream()
                .map(SemesterResponse::fromEntity)
                .collect(Collectors.toList());
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
    // TẠO HỌC KỲ (SEMESTER_CREATE)
    // ============================================================
    @Transactional
    public SemesterResponse createSemester(SemesterCreateRequest request) {

        if (semesterRepository.existsBySemesterCode(request.getSemesterCode().toUpperCase().trim())) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "Mã học kỳ '" + request.getSemesterCode() + "' đã tồn tại");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }

        Semester semester = Semester.builder()
                .semesterCode(request.getSemesterCode().toUpperCase().trim())
                .academicYear(request.getAcademicYear().trim())
                .semesterNumber(request.getSemesterNumber())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Semester.SemesterStatus.ACTIVE)
                .build();

        return SemesterResponse.fromEntity(semesterRepository.save(semester));
    }
}