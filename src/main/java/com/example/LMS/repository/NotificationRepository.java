package com.example.LMS.repository;

import com.example.LMS.entity.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 🔍 Lấy danh sách thông tin thông báo phù hợp cho Sinh viên (Sắp xếp mới nhất lên đầu)
    @Query("SELECT n FROM Notification n " +
            "WHERE n.departmentId IS NULL OR n.departmentId = :departmentId " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findActiveNotificationsForStudent(@Param("departmentId") Long departmentId);
}