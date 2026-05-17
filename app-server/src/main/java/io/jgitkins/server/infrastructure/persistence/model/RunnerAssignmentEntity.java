package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RunnerAssignmentEntity {
    private Long id;

    private Long runnerId;

    private String targetType;

    private Long targetId;

    private LocalDateTime assignedAt;

    public Long getId() {
        return id;
    }

    public RunnerAssignmentEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public RunnerAssignmentEntity withRunnerId(Long runnerId) {
        this.setRunnerId(runnerId);
        return this;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getTargetType() {
        return targetType;
    }

    public RunnerAssignmentEntity withTargetType(String targetType) {
        this.setTargetType(targetType);
        return this;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType == null ? null : targetType.trim();
    }

    public Long getTargetId() {
        return targetId;
    }

    public RunnerAssignmentEntity withTargetId(Long targetId) {
        this.setTargetId(targetId);
        return this;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public RunnerAssignmentEntity withAssignedAt(LocalDateTime assignedAt) {
        this.setAssignedAt(assignedAt);
        return this;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}