package com.example.LMS.repository;

import com.example.LMS.entity.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    // Lấy nhiều profile cùng lúc theo danh sách userId (tránh N+1 query)
    @Query("SELECT up FROM UserProfile up WHERE up.user.id IN :userIds")
    List<UserProfile> findAllByUserIdIn(@Param("userIds") List<Long> userIds);


}