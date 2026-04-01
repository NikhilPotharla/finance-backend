package com.finance.specification;

import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    // Filter by owner (null = no filter = ADMIN sees all)
    public static Specification<Transaction> hasOwner(User owner) {
        return (root, query, builder) -> owner != null
                ? builder.equal(root.get("owner"), owner)
                : builder.conjunction();
    }

    // Filter by transaction type (CREDIT / DEBIT)
    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, builder) -> type != null
                ? builder.equal(root.get("type"), type)
                : builder.conjunction();
    }

    // Filter by exact category (case-insensitive)
    public static Specification<Transaction> hasCategory(String category) {
        return (root, query, builder) -> (category != null && !category.isBlank())
                ? builder.equal(builder.lower(root.get("category")), category.toLowerCase())
                : builder.conjunction();
    }

    // Filter by start date (inclusive)
    public static Specification<Transaction> dateAfter(LocalDate dateFrom) {
        return (root, query, builder) -> dateFrom != null
                ? builder.greaterThanOrEqualTo(root.get("date"), dateFrom)
                : builder.conjunction();
    }

    // Filter by end date (inclusive)
    public static Specification<Transaction> dateBefore(LocalDate dateTo) {
        return (root, query, builder) -> dateTo != null
                ? builder.lessThanOrEqualTo(root.get("date"), dateTo)
                : builder.conjunction();
    }

    // Keyword search across title and category
    public static Specification<Transaction> keywordSearch(String keyword) {
        return (root, query, builder) -> {
            if (keyword == null || keyword.isBlank()) return builder.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("category")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
            );
        };
    }
}
