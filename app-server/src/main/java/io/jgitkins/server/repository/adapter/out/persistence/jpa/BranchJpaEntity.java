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
 * JPA mapping for {@code BRANCH}.
 *
 * <p>The {@code Branch} aggregate carries no identity and no timestamps: it is keyed by
 * {@code (repositoryId, name)} in the domain, and {@code ID}, {@code CREATED_AT} and
 * {@code UPDATED_AT} were all supplied by the database under MyBatis. {@code CREATED_AT} and
 * {@code UPDATED_AT} are therefore read-only here so the database keeps owning them — including the
 * {@code ON UPDATE current_timestamp()} refresh, which would stop firing the moment JPA wrote the
 * column explicitly.
 *
 * <p>The three flags are primitive {@code boolean}, not {@code Boolean}. The DDL declares them
 * {@code NOT NULL DEFAULT 0}, and the domain models them as primitives, so a nullable wrapper would
 * only create a state the domain cannot express and the column cannot store.
 */
@Entity
@Table(name = "BRANCH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class BranchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "IS_LOCKED", nullable = false)
    private boolean locked;

    @Column(name = "IS_CI", nullable = false)
    private boolean ci;

    @Column(name = "IS_DEFAULT", nullable = false)
    private boolean defaultBranch;

    @Column(name = "LOCKED_BY")
    private Long lockedBy;

    @Column(name = "LOCKED_AT")
    private LocalDateTime lockedAt;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
