package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class JobEntity {
    private Long id;

    private Long repositoryId;

    private String commitHash;

    private String branchName;

    private Long triggeredBy;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public JobEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public JobEntity withRepositoryId(Long repositoryId) {
        this.setRepositoryId(repositoryId);
        return this;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public JobEntity withCommitHash(String commitHash) {
        this.setCommitHash(commitHash);
        return this;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash == null ? null : commitHash.trim();
    }

    public String getBranchName() {
        return branchName;
    }

    public JobEntity withBranchName(String branchName) {
        this.setBranchName(branchName);
        return this;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName == null ? null : branchName.trim();
    }

    public Long getTriggeredBy() {
        return triggeredBy;
    }

    public JobEntity withTriggeredBy(Long triggeredBy) {
        this.setTriggeredBy(triggeredBy);
        return this;
    }

    public void setTriggeredBy(Long triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public JobEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}