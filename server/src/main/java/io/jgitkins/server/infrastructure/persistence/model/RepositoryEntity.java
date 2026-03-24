package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RepositoryEntity {
    private Long id;

    private String name;

    private String path;

    private String repositoryType;

    private String ownerType;

    private Long ownerId;

    private String credentialId;

    private String clonePath;

    private String defaultBranch;

    private String visibility;

    private String status;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String description;

    public Long getId() {
        return id;
    }

    public RepositoryEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public RepositoryEntity withName(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPath() {
        return path;
    }

    public RepositoryEntity withPath(String path) {
        this.setPath(path);
        return this;
    }

    public void setPath(String path) {
        this.path = path == null ? null : path.trim();
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public RepositoryEntity withRepositoryType(String repositoryType) {
        this.setRepositoryType(repositoryType);
        return this;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType == null ? null : repositoryType.trim();
    }

    public String getOwnerType() {
        return ownerType;
    }

    public RepositoryEntity withOwnerType(String ownerType) {
        this.setOwnerType(ownerType);
        return this;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType == null ? null : ownerType.trim();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public RepositoryEntity withOwnerId(Long ownerId) {
        this.setOwnerId(ownerId);
        return this;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public RepositoryEntity withCredentialId(String credentialId) {
        this.setCredentialId(credentialId);
        return this;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId == null ? null : credentialId.trim();
    }

    public String getClonePath() {
        return clonePath;
    }

    public RepositoryEntity withClonePath(String clonePath) {
        this.setClonePath(clonePath);
        return this;
    }

    public void setClonePath(String clonePath) {
        this.clonePath = clonePath == null ? null : clonePath.trim();
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public RepositoryEntity withDefaultBranch(String defaultBranch) {
        this.setDefaultBranch(defaultBranch);
        return this;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch == null ? null : defaultBranch.trim();
    }

    public String getVisibility() {
        return visibility;
    }

    public RepositoryEntity withVisibility(String visibility) {
        this.setVisibility(visibility);
        return this;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility == null ? null : visibility.trim();
    }

    public String getStatus() {
        return status;
    }

    public RepositoryEntity withStatus(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public RepositoryEntity withLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.setLastSyncedAt(lastSyncedAt);
        return this;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public RepositoryEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RepositoryEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public RepositoryEntity withDescription(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}