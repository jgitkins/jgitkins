package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** JPA mapping for the USER_IDENTITIES table. {@code EMAIL_VERIFIED} is {@code tinyint(1)} in the
 * schema and a {@code boolean} in the domain; the driver handles that conversion, so the field stays
 * a primitive rather than gaining a converter that could disagree with what MyBatis writes. */
@Entity
@Table(name = "USER_IDENTITIES")
public class UserIdentityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "PROVIDER_NAME", nullable = false, length = 64)
    private String providerName;

    @Column(name = "PROVIDER_SUB", nullable = false)
    private String providerSub;

    @Column(name = "EMAIL", length = 254)
    private String email;

    @Column(name = "EMAIL_VERIFIED", nullable = false)
    private boolean emailVerified;

    @Column(name = "NAME")
    private String name;

    @Column(name = "AVATAR_URL", length = 1024)
    private String avatarUrl;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected UserIdentityJpaEntity() {
    }

    public UserIdentityJpaEntity(Long id, Long userId, String providerName, String providerSub, String email,
            boolean emailVerified, String name, String avatarUrl,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.providerName = providerName;
        this.providerSub = providerSub;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProviderName() { return providerName; }
    public String getProviderSub() { return providerSub; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
