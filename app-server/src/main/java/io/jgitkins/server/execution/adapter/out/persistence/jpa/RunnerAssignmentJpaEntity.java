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
 * JPA mapping for {@code RUNNER_ASSIGNMENT}.
 *
 * <p>There is no unique key on {@code RUNNER_ID}: a runner can accumulate several assignment rows, and
 * the effective scope is the newest by {@code ASSIGNED_AT}. That is what the MyBatis adapter reads, so
 * it is what this mapping has to support — a one-to-one association would be a schema claim the DDL
 * does not make.
 */
@Entity
@Table(name = "RUNNER_ASSIGNMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RunnerAssignmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUNNER_ID", nullable = false)
    private Long runnerId;

    @Column(name = "TARGET_TYPE", nullable = false, length = 32)
    private String targetType;

    @Column(name = "TARGET_ID")
    private Long targetId;

    @Column(name = "ASSIGNED_AT", nullable = false)
    private LocalDateTime assignedAt;
}
