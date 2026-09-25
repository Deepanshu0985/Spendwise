package com.finance.domain.category;

import java.time.Instant;
import java.util.UUID;

/** Pure business model - no persistence framework dependency; see infrastructure.persistence.category for the JPA mapping. */
public class Category {

    private final UUID id;
    private final UUID userId;
    private final UUID parentId;
    private String name;
    private final CategoryType categoryType;
    private final boolean system;
    private boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    /** Always creates a custom (non-system) category; system rows only ever come from migrations. */
    public Category(UUID userId, String name, CategoryType categoryType) {
        this(UUID.randomUUID(), userId, null, name, categoryType, false, true, null, null);
    }

    /** Reconstitution constructor - used by the persistence mapper to rebuild a domain object from a stored row. */
    public Category(
            UUID id,
            UUID userId,
            UUID parentId,
            String name,
            CategoryType categoryType,
            boolean system,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.parentId = parentId;
        this.name = name;
        this.categoryType = categoryType;
        this.system = system;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public boolean isSystem() {
        return system;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
