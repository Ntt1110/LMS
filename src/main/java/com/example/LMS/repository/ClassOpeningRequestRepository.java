package com.example.LMS.repository;

import com.example.LMS.entity.Enum.ClassOpenningStatus;
import com.example.LMS.entity.model.ClassOpeningRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassOpeningRequestRepository extends JpaRepository<ClassOpeningRequest, Long>, JpaSpecificationExecutor<ClassOpeningRequest> {
    // Tìm các đơn đề xuất của riêng người thực hiện để hiển thị lịch sử ở UI
    List<ClassOpeningRequest> findByRequesterId(Long requesterId);

    List<ClassOpeningRequest> findByStatusOrderByCreatedAtDesc(ClassOpenningStatus status);

    @Query("SELECT r FROM ClassOpeningRequest r ORDER BY r.createdAt DESC")
    Page<ClassOpeningRequest> findAllRequestsForPDT(Pageable pageable);

    // 🌟 HÀM 2: Dành cho Trưởng Khoa (Phải JOIN để lọc đúng khoa mình quản lý)
    @Query("SELECT r FROM ClassOpeningRequest r " +
            "JOIN Course c ON r.courseId = c.id " +
            "JOIN c.department d " +
            "WHERE d.manager.id = :managerId " +
            "ORDER BY r.createdAt DESC")
    Page<ClassOpeningRequest> findRequestsByDean(@Param("managerId") Long managerId, Pageable pageable);

    // 🌟 TRUY VẤN VẠN NĂNG: Tự động lọc theo Phân Quyền + Trạng thái + Tìm kiếm + Học kỳ
    @Query("SELECT r FROM ClassOpeningRequest r " +
            "LEFT JOIN Course c ON r.courseId = c.id " +
            "LEFT JOIN c.department d " +
            "WHERE r.deletedAt IS NULL " + // 🛡️ Bỏ qua các đơn đã bị xóa mềm
            "AND (:managerId IS NULL OR d.manager.id = :managerId) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:semesterId IS NULL OR r.semester.id = :semesterId) " +
            "AND (:search IS NULL OR LOWER(r.note) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.semester.semesterCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY r.createdAt DESC")
    Page<ClassOpeningRequest> findFilteredRequests(
            @Param("managerId") Long managerId,
            @Param("status") com.example.LMS.entity.Enum.ClassOpenningStatus status,
            @Param("semesterId") Long semesterId,
            @Param("search") String search,
            Pageable pageable);
}