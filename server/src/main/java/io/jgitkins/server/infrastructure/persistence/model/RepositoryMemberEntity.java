package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class RepositoryMemberEntity {
    private Long id;

    private Long repositoryId;

    private Long userId;

    private String role;

    private LocalDateTime addedAt;

    public Long getId() {
        return id;
    }

    public RepositoryMemberEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public RepositoryMemberEntity withRepositoryId(Long repositoryId) {
        this.setRepositoryId(repositoryId);
        return this;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public RepositoryMemberEntity withUserId(Long userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public RepositoryMemberEntity withRole(String role) {
        this.setRole(role);
        return this;
    }

    public void setRole(String role) {
        this.role = role == null ? null : role.trim();
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public RepositoryMemberEntity withAddedAt(LocalDateTime addedAt) {
        this.setAddedAt(addedAt);
        return this;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}