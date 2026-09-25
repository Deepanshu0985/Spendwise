package com.finance.infrastructure.persistence.category;

import com.finance.domain.category.CategoryType;
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
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CategoryType categoryType;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryJpaEntity() {
        // JPA
    }

    CategoryJpaEntity(UUID id, UUID userId, UUID parentId, String name, CategoryType categoryType, boolean system, boolean active) {
        this.id = id;
        this.userId = userId;
        this.parentId = parentId;
        this.name = name;
        this.categoryType = categoryType;
        this.system = system;
        this.active = active;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getParentId() {
        return parentId;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    CategoryType getCategoryType() {
        return categoryType;
    }

    boolean isSystem() {
        return system;
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
