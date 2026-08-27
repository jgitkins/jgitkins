package io.jgitkins.server.execution.adapter.out.persistence.jpa;

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
 * JPA mapping for {@code JOB}.
 *
 * <p>{@code CREATED_AT} stays writable even though the DDL defaults it: the domain carries the value
 * and the MyBatis mapper wrote it on every insert, so the application has always owned it.
 */
@Entity
@Table(name = "JOB")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class JobJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "COMMIT_HASH", nullable = false, length = 64)
    private String commitHash;

    @Column(name = "BRANCH_NAME", nullable = false, length = 255)
    private String branchName;

    @Column(name = "TRIGGERED_BY", nullable = false)
    private Long triggeredBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
