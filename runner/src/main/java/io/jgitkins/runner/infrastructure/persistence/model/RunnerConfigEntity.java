package io.jgitkins.runner.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RunnerConfigEntity {
    private Long id;

    private Long runnerId;

    private String configKey;

    private String configValue;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public RunnerConfigEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public RunnerConfigEntity withRunnerId(Long runnerId) {
        this.setRunnerId(runnerId);
        return this;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public RunnerConfigEntity withConfigKey(String configKey) {
        this.setConfigKey(configKey);
        return this;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey == null ? null : configKey.trim();
    }

    public String getConfigValue() {
        return configValue;
    }

    public RunnerConfigEntity withConfigValue(String configValue) {
        this.setConfigValue(configValue);
        return this;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue == null ? null : configValue.trim();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RunnerConfigEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}