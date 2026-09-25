package com.finance.transaction;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pure query-building logic, not swappable business behavior - no interface, same reasoning as SessionCookieFactory. */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> forUserAndFilter(UUID userId, TransactionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            // Soft-deleted rows never show up unless a caller explicitly asks
            // for status=DELETED (api-specification.md's DELETE is soft).
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            } else {
                predicates.add(cb.notEqual(root.get("status"), TransactionStatus.DELETED));
            }

            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), filter.to()));
            }
            if (filter.accountId() != null) {
                predicates.add(cb.equal(root.get("accountId"), filter.accountId()));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
            }
            if (filter.merchantId() != null) {
                predicates.add(cb.equal(root.get("merchantId"), filter.merchantId()));
            }
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("transactionType"), filter.type()));
            }
            if (filter.currency() != null) {
                predicates.add(cb.equal(root.get("currency"), filter.currency()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
