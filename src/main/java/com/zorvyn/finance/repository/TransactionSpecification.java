package com.zorvyn.finance.repository;

import com.zorvyn.finance.dto.request.TransactionFilterRequest;
import com.zorvyn.finance.model.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic transaction filtering.
 * Combines multiple optional predicates so only provided
 * filter fields are applied to the query.
 */
public final class TransactionSpecification {

    private TransactionSpecification() {}

    public static Specification<Transaction> withFilters(TransactionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted records
            predicates.add(cb.isFalse(root.get("deleted")));

            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("category")),
                        filter.getCategory().trim().toLowerCase()
                ));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), filter.getEndDate()));
            }

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Predicate inNotes    = cb.like(cb.lower(root.get("notes")),    pattern);
                Predicate inCategory = cb.like(cb.lower(root.get("category")), pattern);
                predicates.add(cb.or(inNotes, inCategory));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
