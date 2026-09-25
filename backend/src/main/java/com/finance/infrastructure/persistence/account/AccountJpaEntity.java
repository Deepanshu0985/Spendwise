package com.finance.infrastructure.persistence.account;

import com.finance.domain.account.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "last4")
    private String last4;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountJpaEntity() {
        // JPA
    }

    AccountJpaEntity(UUID id, UUID userId, String name, AccountType accountType, String institutionName, String last4, String currency, boolean active) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.accountType = accountType;
        this.institutionName = institutionName;
        this.last4 = last4;
        this.currency = currency;
        this.active = active;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    AccountType getAccountType() {
        return accountType;
    }

    String getInstitutionName() {
        return institutionName;
    }

    void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    String getLast4() {
        return last4;
    }

    void setLast4(String last4) {
        this.last4 = last4;
    }

    String getCurrency() {
        return currency;
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
