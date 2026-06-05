package com.example.LMS.repository;

import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {

    // Xem chi tiết theo id, chưa bị xóa mềm
    Optional<Major> findByIdAndDeletedAtIsNull(Long id);


    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(m.id, m.name) " +
            "FROM Major m")
    List<DropdownResponseDto> findAllMajorsDropdown();


}