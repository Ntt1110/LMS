package com.example.LMS.service;

import com.example.LMS.dto.request.UserListRequest;
import com.example.LMS.dto.response.UserResponse;
import com.example.LMS.entity.model.User;
import com.example.LMS.entity.model.UserProfile;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.UserProfileRepository;
import com.example.LMS.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // ============================================================
    // DANH SÁCH NGƯỜI DÙNG
    // ============================================================
    public Page<UserResponse> getUsers(UserListRequest request) {

        // 1. Tạo Pageable (phân trang + sắp xếp)
        Sort sort = request.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 2. Xây dựng Specification (dynamic filter)
        Specification<User> spec = buildSpecification(request);

        // 3. Query users (phân trang)
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // 4. Lấy tất cả profile của trang hiện tại trong 1 query (tránh N+1)
        List<Long> userIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getId(),
                        p -> p
                ));

        // 5. Map sang DTO, kết hợp user + profile
        return userPage.map(user -> UserResponse.fromEntity(user, profileMap.get(user.getId())));
    }

    // ============================================================
    // XEM CHI TIẾT NGƯỜI DÙNG
    // ============================================================
    public UserResponse getUserById(Long id) {

        // 1. Tìm user theo id, chưa bị xóa mềm
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy người dùng với id: " + id
                ));

        // 2. Lấy profile (có thể null nếu chưa có)
        UserProfile profile = userProfileRepository.findByUserId(id).orElse(null);

        // 3. Map sang DTO
        return UserResponse.fromEntity(user, profile);
    }

    // ============================================================
    // SPECIFICATION (dynamic filter cho danh sách)
    // ============================================================
    private Specification<User> buildSpecification(UserListRequest request) {
        return (root, query, cb) -> {

            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Filter theo keyword: username, email, full_name
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Join<Object, Object> profileJoin = root.join("profile", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(profileJoin.get("fullName")), pattern)
                ));
            }

            // Filter theo isActive
            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            // Filter theo roleCode
            if (request.getRoleCode() != null && !request.getRoleCode().isBlank()) {
                Join<Object, Object> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(rolesJoin.get("code"), request.getRoleCode().toUpperCase()));
            }

            // Chỉ lấy user chưa bị xóa mềm
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}