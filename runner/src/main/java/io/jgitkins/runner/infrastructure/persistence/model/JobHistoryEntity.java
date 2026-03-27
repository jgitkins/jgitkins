package io.jgitkins.runner.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class JobHistoryEntity {
    private Long id;

    private Long runnerId;

    private Long jobId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String logPath;

    private Integer exitCode;

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

    public Integer getExitCode() {
        return exitCode;
    }

    public JobHistoryEntity withExitCode(Integer exitCode) {
        this.setExitCode(exitCode);
        return this;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }
}