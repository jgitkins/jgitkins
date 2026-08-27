package io.jgitkins.server.change.review.adapter.out.persistence.model;

import java.time.LocalDateTime;

public class PullRequestEntity {
    private Long id;

    private Long repositoryId;

    private String sourceBranch;

    private String sourceHead;

    private String targetBranch;

    private String targetHead;

    private String status;

    private Boolean targetDrifted;

    private String previousTargetHead;

    private String currentTargetHead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public PullRequestEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public PullRequestEntity withRepositoryId(Long repositoryId) {
        this.setRepositoryId(repositoryId);
        return this;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public PullRequestEntity withSourceBranch(String sourceBranch) {
        this.setSourceBranch(sourceBranch);
        return this;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch == null ? null : sourceBranch.trim();
    }

    public String getSourceHead() {
        return sourceHead;
    }

    public PullRequestEntity withSourceHead(String sourceHead) {
        this.setSourceHead(sourceHead);
        return this;
    }

    public void setSourceHead(String sourceHead) {
        this.sourceHead = sourceHead == null ? null : sourceHead.trim();
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public PullRequestEntity withTargetBranch(String targetBranch) {
        this.setTargetBranch(targetBranch);
        return this;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch == null ? null : targetBranch.trim();
    }

    public String getTargetHead() {
        return targetHead;
    }

    public PullRequestEntity withTargetHead(String targetHead) {
        this.setTargetHead(targetHead);
        return this;
    }

    public void setTargetHead(String targetHead) {
        this.targetHead = targetHead == null ? null : targetHead.trim();
    }

    public String getStatus() {
        return status;
    }

    public PullRequestEntity withStatus(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public Boolean getTargetDrifted() {
        return targetDrifted;
    }

    public PullRequestEntity withTargetDrifted(Boolean targetDrifted) {
        this.setTargetDrifted(targetDrifted);
        return this;
    }

    public void setTargetDrifted(Boolean targetDrifted) {
        this.targetDrifted = targetDrifted;
    }

    public String getPreviousTargetHead() {
        return previousTargetHead;
    }

    public PullRequestEntity withPreviousTargetHead(String previousTargetHead) {
        this.setPreviousTargetHead(previousTargetHead);
        return this;
    }

    public void setPreviousTargetHead(String previousTargetHead) {
        this.previousTargetHead = previousTargetHead == null ? null : previousTargetHead.trim();
    }

    public String getCurrentTargetHead() {
        return currentTargetHead;
    }

    public PullRequestEntity withCurrentTargetHead(String currentTargetHead) {
        this.setCurrentTargetHead(currentTargetHead);
        return this;
    }

    public void setCurrentTargetHead(String currentTargetHead) {
        this.currentTargetHead = currentTargetHead == null ? null : currentTargetHead.trim();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PullRequestEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PullRequestEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
