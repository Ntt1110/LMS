package com.example.LMS.repository;

import com.example.LMS.entity.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {

    // Xem chi tiết theo id, chưa bị xóa mềm
    Optional<Major> findByIdAndDeletedAtIsNull(Long id);
}