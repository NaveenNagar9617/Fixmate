package com.fixmate.repository.spec;

import com.fixmate.model.Complaint;
import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ComplaintSpecification {

    public static Specification<Complaint> withFilters(
            UUID studentId,
            UUID staffId,
            ComplaintStatus status,
            ComplaintCategory category,
            Priority priority,
            String block,
            Integer floor,
            UUID assignedStaffId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (studentId != null) {
                predicates.add(cb.equal(root.get("student").get("id"), studentId));
            }
            if (staffId != null) {
                predicates.add(cb.equal(root.get("assignedStaff").get("id"), staffId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (block != null) {
                predicates.add(cb.equal(root.get("locationBlock"), block));
            }
            if (floor != null) {
                predicates.add(cb.equal(root.get("locationFloor"), floor));
            }
            if (assignedStaffId != null) {
                predicates.add(cb.equal(root.get("assignedStaff").get("id"), assignedStaffId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
