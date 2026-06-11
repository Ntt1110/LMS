package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassEntity;
import com.example.LMS.entity.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long>, JpaSpecificationExecutor<Semester> {

    Optional<Semester> findByIdAndDeletedAtIsNull(Long id);

    boolean existsBySemesterCode(String semesterCode);

    // Lấy tất cả học kỳ chưa xóa, sắp xếp theo năm học desc, mã học kỳ asc
    List<Semester> findAllByDeletedAtIsNullOrderByAcademicYearDescSemesterCodeAsc();
}