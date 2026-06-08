package com.example.LMS.service;

import com.example.LMS.dto.request.RequestFilterDto;
import com.example.LMS.entity.model.ClassOpeningRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class ClassOpeningSpecification {
    public static Specification<ClassOpeningRequest> filterRequests(RequestFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Long.class != query.getResultType()) {
                root.fetch("semester", jakarta.persistence.criteria.JoinType.LEFT);
                // root.fetch("course", jakarta.persistence.criteria.JoinType.LEFT); // Nếu có liên kết course
            }
            // 1. Lọc theo trạng thái
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            // 2. Lọc theo học kỳ
            if (filter.getSemesterId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("semester").get("id"), filter.getSemesterId()));
            }
            // tim kiem
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String keyword = "%" + filter.getSearch().trim().toLowerCase() + "%";
                Predicate searchInNote = criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), keyword);
                // Tìm xuyên bảng thông qua liên kết đã được fetch
                Predicate searchInSemester = criteriaBuilder.like(criteriaBuilder.lower(root.get("semester").get("semesterCode")), keyword);

                predicates.add(criteriaBuilder.or(searchInNote, searchInSemester));
            }

            // Sắp xếp đơn mới nhất lên đầu bản ghi
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));

            // 🌟 THẦN CHÚ VÁ LỖI TẠI ĐÂY: Ép câu lệnh SQL sinh ra phải có DISTINCT để chặn đứng lặp dữ liệu!
            if (Long.class != query.getResultType()) {
                // Chỉ distinct khi câu lệnh bốc dữ liệu content, không làm khó câu lệnh count của phân trang
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
