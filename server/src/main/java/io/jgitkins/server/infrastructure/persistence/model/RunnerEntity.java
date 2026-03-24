package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RunnerEntity {
    private Long id;

    private String token;

    private String description;

    private String status;

    private String ipAddress;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public RunnerEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public RunnerEntity withToken(String token) {
        this.setToken(token);
        return this;
    }

    public void setToken(String token) {
        this.token = token == null ? null : token.trim();
    }

    public String getDescription() {
        return description;
    }

    public RunnerEntity withDescription(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public String getStatus() {
        return status;
    }

    public RunnerEntity withStatus(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public RunnerEntity withIpAddress(String ipAddress) {
        this.setIpAddress(ipAddress);
        return this;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress == null ? null : ipAddress.trim();
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public RunnerEntity withLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.setLastHeartbeatAt(lastHeartbeatAt);
        return this;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public RunnerEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}