package com.example.LMS.repository;

import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ShiftRepository extends JpaRepository <Shift, Long> {

    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(s.id, s.name) FROM Shift s")
    List<DropdownResponseDto> findAllShiftsForDropdown();
}
