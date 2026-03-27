package io.jgitkins.runner.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RunnerConfigFileEntity {
    private Long id;

    private Long runnerId;

    private String filename;

    private String contents;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public RunnerConfigFileEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public RunnerConfigFileEntity withRunnerId(Long runnerId) {
        this.setRunnerId(runnerId);
        return this;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getFilename() {
        return filename;
    }

    public RunnerConfigFileEntity withFilename(String filename) {
        this.setFilename(filename);
        return this;
    }

    public void setFilename(String filename) {
        this.filename = filename == null ? null : filename.trim();
    }

    public String getContents() {
        return contents;
    }

    public RunnerConfigFileEntity withContents(String contents) {
        this.setContents(contents);
        return this;
    }

    public void setContents(String contents) {
        this.contents = contents == null ? null : contents.trim();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RunnerConfigFileEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}