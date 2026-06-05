package com.example.LMS.repository;

import com.example.LMS.entity.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    @Query("SELECT d.id FROM Department d WHERE d.manager.id = :deanUserId")
    Optional<Long> findIdByManagerId(@Param("deanUserId") Long deanUserId);
}