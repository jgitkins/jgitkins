package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class OrganizeEntity {
    private Long id;

    private String name;

    private String path;

    private Long ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String description;

    public Long getId() {
        return id;
    }

    public OrganizeEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public OrganizeEntity withName(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPath() {
        return path;
    }

    public OrganizeEntity withPath(String path) {
        this.setPath(path);
        return this;
    }

    public void setPath(String path) {
        this.path = path == null ? null : path.trim();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public OrganizeEntity withOwnerId(Long ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrganizeEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OrganizeEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public OrganizeEntity withDescription(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}