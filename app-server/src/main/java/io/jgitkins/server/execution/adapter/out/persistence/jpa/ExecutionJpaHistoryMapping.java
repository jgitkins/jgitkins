package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.shared.domain.model.vo.SequenceNumber;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Job-history rows to domain, in one place.
 *
 * <p>Extracted in task 2.76 because both the write adapter and the dispatch query adapter load the same
 * ordered history list, and the sequence number is the part that must not drift between them: there is no
 * sequence column, so the value comes from the row's position in the {@code CREATED_AT ASC, ID ASC}
 * ordering. Two copies of that rule is two chances for the dispatch path and the write path to disagree
 * about which transition is number two — and they compare history identity to decide who wins a dispatch.
 */
final class ExecutionJpaHistoryMapping {

    private ExecutionJpaHistoryMapping() {
    }

    /** Requires the list already ordered {@code CREATED_AT ASC, ID ASC}; the position is the sequence. */
    static List<JobHistory> toDomain(List<JobHistoryJpaEntity> ordered) {
        return IntStream.range(0, ordered.size())
                .mapToObj(index -> toDomain(ordered.get(index), index + 1))
                .toList();
    }

    static JobHistory toDomain(JobHistoryJpaEntity entity, int sequence) {
        return JobHistory.reconstruct(
                JobHistoryId.of(String.valueOf(entity.getId())),
                JobId.of(String.valueOf(entity.getJobId())),
                SequenceNumber.of(sequence),
                entity.getRunnerId() != null ? RunnerId.of(String.valueOf(entity.getRunnerId())) : null,
                JobStatus.valueOf(entity.getStatus()),
                ExecutionSystemActor.SYSTEM,
                entity.getCreatedAt());
    }
}
