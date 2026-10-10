package com.finance.infrastructure.persistence.transaction;

import com.finance.domain.transaction.TransactionFilter;
import com.finance.domain.transaction.TransactionLookup;
import com.finance.domain.transaction.TransactionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pure query-building logic, not swappable business behavior - no interface, same reasoning as CurrentUserGuard. */
final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<TransactionJpaEntity> forUserAndFilter(UUID userId, TransactionFilter filter) {
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

    /** The assistant's search: confirmed rows only, every text word must match somewhere (see TransactionLookup). */
    static Specification<TransactionJpaEntity> forLookup(UUID userId, TransactionLookup lookup) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("status"), TransactionStatus.CONFIRMED));
            if (lookup.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), lookup.from()));
            }
            if (lookup.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), lookup.to()));
            }
            if (lookup.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), lookup.minAmount()));
            }
            if (lookup.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), lookup.maxAmount()));
            }
            if (lookup.type() != null) {
                predicates.add(cb.equal(root.get("transactionType"), lookup.type()));
            }
            if (lookup.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), lookup.categoryId()));
            }
            if (lookup.merchantIds() != null) {
                // An empty set means "a merchant was named but none matched": nothing can match.
                predicates.add(lookup.merchantIds().isEmpty() ? cb.disjunction() : root.get("merchantId").in(lookup.merchantIds()));
            }
            for (TransactionLookup.TextMatcher matcher : lookup.textMatchers()) {
                String pattern = "%" + escapeLike(matcher.word().toLowerCase(java.util.Locale.ROOT)) + "%";
                List<Predicate> anyOf = new ArrayList<>();
                anyOf.add(cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), pattern, '\\'));
                anyOf.add(cb.like(cb.lower(cb.coalesce(root.<String>get("rawDescription"), "")), pattern, '\\'));
                if (!matcher.merchantIdsWhoseNameContainsIt().isEmpty()) {
                    anyOf.add(root.get("merchantId").in(matcher.merchantIdsWhoseNameContainsIt()));
                }
                predicates.add(cb.or(anyOf.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** The words come from a model, so % _ and \ are made literal rather than acting as wildcards. */
    private static String escapeLike(String word) {
        return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
