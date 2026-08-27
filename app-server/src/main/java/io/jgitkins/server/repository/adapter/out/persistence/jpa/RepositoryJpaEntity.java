package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping for {@code REPOSITORY}, replacing the MBG {@code RepositoryEntity}.
 *
 * <p>{@code REPOSITORY_TYPE} is deliberately read-only. The DDL declares it
 * {@code varchar(32) NOT NULL DEFAULT 'GIT'} and nothing in the domain models it, so the MyBatis
 * adapter never wrote it and relied on the column default. JPA writes every mapped column, so
 * leaving it writable would insert {@code NULL} into a {@code NOT NULL} column and fail every
 * create. Marking it {@code insertable=false, updatable=false} keeps the database as the owner of
 * that value, exactly as before. The cost is that the in-memory field stays null immediately after
 * an insert until the row is read again — acceptable here only because no caller reads it.
 *
 * <p>{@code STATUS} is the opposite case and stays writable: the MyBatis mapper derived it from
 * {@code lastSyncedAt} on every write, so the application owns it and the adapter reproduces that
 * derivation.
 */
@Entity
@Table(name = "REPOSITORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RepositoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "PATH", nullable = false, length = 255)
    private String path;

    @Column(name = "OWNER_TYPE", length = 32)
    private String ownerType;

    @Column(name = "OWNER_ID")
    private Long ownerId;

    @Column(name = "CREDENTIAL_ID", length = 128)
    private String credentialId;

    @Column(name = "CLONE_PATH", length = 512)
    private String clonePath;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DEFAULT_BRANCH", nullable = false, length = 255)
    private String defaultBranch;

    @Column(name = "VISIBILITY", nullable = false, length = 32)
    private String visibility;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "LAST_SYNCED_AT")
    private LocalDateTime lastSyncedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    // Database-owned; see the class javadoc.
    @Column(name = "REPOSITORY_TYPE", insertable = false, updatable = false, length = 32)
    private String repositoryType;
}
