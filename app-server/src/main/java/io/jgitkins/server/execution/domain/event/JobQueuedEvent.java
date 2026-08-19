package io.jgitkins.server.execution.domain.event;

import io.jgitkins.server.shared.domain.event.DomainEvent;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobQueuedEvent implements DomainEvent {

    private final JobId jobId;
    private final ExecutionRepositoryId repositoryId;
    private final BranchName branchName;
    private final CommitHash commitHash;
    private final RunnerId runnerId;
    private final JobStatus status;
    private final Instant occurredAt;

    public static JobQueuedEvent from(Job job, RunnerId runnerId) {
        return new JobQueuedEvent(
                job.getId(),
                job.getRepositoryId(),
                job.getBranchName(),
                job.getCommitHash(),
                runnerId,
                job.getCurrentStatus(),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
