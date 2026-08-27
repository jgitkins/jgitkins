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
 * JPA mapping for {@code JOB_HISTORY}.
 *
 * <p>{@code JOB_ID} is a plain column, not a {@code @ManyToOne} to {@link JobJpaEntity}. The reads it
 * replaces never join, and an association would have Hibernate fetch the parent job on every history
 * row — changing the query plan of the dispatch hot path, which is the one thing a technology swap
 * behind a selector must not do.
 *
 * <p>{@code LOG_PATH}, {@code STARTED_AT} and {@code FINISHED_AT} are mapped but never written by the
 * adapter, matching the MyBatis mapper, which left them out of its selective insert. They are nullable
 * in the DDL, so writing explicit nulls is equivalent.
 */
@Entity
@Table(name = "JOB_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class JobHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "JOB_ID", nullable = false)
    private Long jobId;

    @Column(name = "RUNNER_ID")
    private Long runnerId;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "LOG_PATH", length = 1024)
    private String logPath;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "FINISHED_AT")
    private LocalDateTime finishedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
