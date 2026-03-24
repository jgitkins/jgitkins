package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class OrganizeMemberEntity {
    private Long id;

    private Long organizeId;

    private Long userId;

    private String role;

    private LocalDateTime joinedAt;

    public Long getId() {
        return id;
    }

    public OrganizeMemberEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizeId() {
        return organizeId;
    }

    public OrganizeMemberEntity withOrganizeId(Long organizeId) {
        this.setOrganizeId(organizeId);
        return this;
    }

    public void setOrganizeId(Long organizeId) {
        this.organizeId = organizeId;
    }

    public Long getUserId() {
        return userId;
    }

    public OrganizeMemberEntity withUserId(Long userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public OrganizeMemberEntity withRole(String role) {
        this.setRole(role);
        return this;
    }

    public void setRole(String role) {
        this.role = role == null ? null : role.trim();
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public OrganizeMemberEntity withJoinedAt(LocalDateTime joinedAt) {
        this.setJoinedAt(joinedAt);
        return this;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}