package com.example.LMS.repository;

import com.example.LMS.dto.response.DropdownResponseDto;
import com.example.LMS.entity.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RoomRepository extends JpaRepository <Room,Long> {

    @Query("SELECT new com.example.LMS.dto.response.DropdownResponseDto(r.id, r.name) FROM Room r")
    List<DropdownResponseDto> findAllRoomsForDropdown();
}
