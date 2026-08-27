package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

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
 * JPA mapping for {@code PULL_REQUEST}.
 *
 * <p>{@code TARGET_DRIFTED} is a primitive {@code boolean}: the DDL declares it
 * {@code NOT NULL DEFAULT 0} and the domain's {@code TargetDrift} has no third state, so a nullable
 * wrapper would only invent one the column cannot store.
 *
 * <p>{@code CREATED_AT} and {@code UPDATED_AT} stay writable. The adapter sets both explicitly on
 * every write, which is also why the column's {@code ON UPDATE current_timestamp()} never fires for
 * either provider.
 */
@Entity
@Table(name = "PULL_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class PullRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "SOURCE_BRANCH", nullable = false, length = 255)
    private String sourceBranch;

    @Column(name = "SOURCE_HEAD", nullable = false, length = 64)
    private String sourceHead;

    @Column(name = "TARGET_BRANCH", nullable = false, length = 255)
    private String targetBranch;

    @Column(name = "TARGET_HEAD", nullable = false, length = 64)
    private String targetHead;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "TARGET_DRIFTED", nullable = false)
    private boolean targetDrifted;

    @Column(name = "PREVIOUS_TARGET_HEAD", length = 64)
    private String previousTargetHead;

    @Column(name = "CURRENT_TARGET_HEAD", length = 64)
    private String currentTargetHead;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
