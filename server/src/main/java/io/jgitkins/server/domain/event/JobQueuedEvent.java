package io.jgitkins.server.domain.event;

import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.domain.model.vo.JobId;
import io.jgitkins.server.domain.model.vo.JobStatus;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RunnerId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobQueuedEvent implements DomainEvent {

    private final JobId jobId;
    private final RepositoryId repositoryId;
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
