package com.example.LMS.repository;

import com.example.LMS.entity.model.ClassOpeningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassOpeningRequestRepository extends JpaRepository<ClassOpeningRequest, Long> {
    // Tìm các đơn đề xuất của riêng người thực hiện để hiển thị lịch sử ở UI
    List<ClassOpeningRequest> findByRequesterId(Long requesterId);
}