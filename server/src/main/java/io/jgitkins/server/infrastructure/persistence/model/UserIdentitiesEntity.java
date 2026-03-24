package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public class UserIdentitiesEntity {
    private Long id;

    private Long userId;

    private String providerName;

    private String providerSub;

    private String email;

    private Boolean emailVerified;

    private String name;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public UserIdentitiesEntity withId(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public UserIdentitiesEntity withUserId(Long userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProviderName() {
        return providerName;
    }

    public UserIdentitiesEntity withProviderName(String providerName) {
        this.setProviderName(providerName);
        return this;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName == null ? null : providerName.trim();
    }

    public String getProviderSub() {
        return providerSub;
    }

    public UserIdentitiesEntity withProviderSub(String providerSub) {
        this.setProviderSub(providerSub);
        return this;
    }

    public void setProviderSub(String providerSub) {
        this.providerSub = providerSub == null ? null : providerSub.trim();
    }

    public String getEmail() {
        return email;
    }

    public UserIdentitiesEntity withEmail(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public UserIdentitiesEntity withEmailVerified(Boolean emailVerified) {
        this.setEmailVerified(emailVerified);
        return this;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public UserIdentitiesEntity withName(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserIdentitiesEntity withAvatarUrl(String avatarUrl) {
        this.setAvatarUrl(avatarUrl);
        return this;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl == null ? null : avatarUrl.trim();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserIdentitiesEntity withCreatedAt(LocalDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UserIdentitiesEntity withUpdatedAt(LocalDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}