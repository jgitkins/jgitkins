package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class BranchEntity {
    private Long id;

    private Long repositoryId;

    private String name;

    private Boolean isLocked;

    private Boolean isCi;

    private Boolean isDefault;

    private Long lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public BranchEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public BranchEntity withRepositoryId(Long repositoryId) {
        this.setRepositoryId(repositoryId);
        return this;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getName() {
        return name;
    }

    public BranchEntity withName(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public BranchEntity withIsLocked(Boolean isLocked) {
        this.setIsLocked(isLocked);
        return this;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public Boolean getIsCi() {
        return isCi;
    }

    public BranchEntity withIsCi(Boolean isCi) {
        this.setIsCi(isCi);
        return this;
    }

    public void setIsCi(Boolean isCi) {
        this.isCi = isCi;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public BranchEntity withIsDefault(Boolean isDefault) {
        this.setIsDefault(isDefault);
        return this;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Long getLockedBy() {
        return lockedBy;
    }

    public BranchEntity withLockedBy(Long lockedBy) {
        this.setLockedBy(lockedBy);
        return this;
    }

    public void setLockedBy(Long lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public BranchEntity withLockedAt(LocalDateTime lockedAt) {
        this.setLockedAt(lockedAt);
        return this;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BranchEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public BranchEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}