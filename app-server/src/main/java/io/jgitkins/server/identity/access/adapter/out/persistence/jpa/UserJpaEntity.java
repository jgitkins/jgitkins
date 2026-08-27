package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the USER table.
 *
 * <p>Column set taken from {@code app-server/data/ddl.sql}. {@code AUTHORITY} and {@code STATUS} are
 * stored as the enum name in a {@code varchar}, matching what {@code UserDomainMapper} writes for
 * MyBatis; mapping them as {@code @Enumerated} instead would couple the column format to Java enum
 * ordering and change what a rollback reads back.
 *
 * <p>{@code USER} is a reserved word in some dialects, which is why the table name is quoted in the
 * DDL. Hibernate quotes identifiers it recognises as reserved, so the plain name is correct here and
 * the H2 profiles pass {@code NON_KEYWORDS=USER} for the same reason.
 */
@Entity
@Table(name = "USER")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, length = 150)
    private String username;

    @Column(name = "EMAIL", length = 254)
    private String email;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "AVATAR_URL", length = 1024)
    private String avatarUrl;

    @Column(name = "AUTHORITY", nullable = false, length = 32)
    private String authority;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected UserJpaEntity() {
        // required by JPA
    }

    public UserJpaEntity(Long id, String username, String email, String displayName, String avatarUrl,
            String authority, String status, LocalDateTime lastLoginAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.authority = authority;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getAuthority() { return authority; }
    public String getStatus() { return status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
