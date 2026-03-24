package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class JobHistoryEntity {
    private Long id;

    private Long jobId;

    private Long runnerId;

    private String status;

    private String logPath;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public JobHistoryEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public JobHistoryEntity withJobId(Long jobId) {
        this.setJobId(jobId);
        return this;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public JobHistoryEntity withRunnerId(Long runnerId) {
        this.setRunnerId(runnerId);
        return this;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getStatus() {
        return status;
    }

    public JobHistoryEntity withStatus(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getLogPath() {
        return logPath;
    }

    public JobHistoryEntity withLogPath(String logPath) {
        this.setLogPath(logPath);
        return this;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath == null ? null : logPath.trim();
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public JobHistoryEntity withStartedAt(LocalDateTime startedAt) {
        this.setStartedAt(startedAt);
        return this;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public JobHistoryEntity withFinishedAt(LocalDateTime finishedAt) {
        this.setFinishedAt(finishedAt);
        return this;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public JobHistoryEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}