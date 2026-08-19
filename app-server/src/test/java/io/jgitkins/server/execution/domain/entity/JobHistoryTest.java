package io.jgitkins.server.execution.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JobHistoryTest {
    @Test void factories_useSystemActorAndExpectedStatuses() {
        JobId jobId = JobId.of("1");
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 12, 0);
        assertThat(JobHistory.createPending(jobId, now).getCreatedBy()).isEqualTo(ExecutionSystemActor.SYSTEM);
        assertThat(JobHistory.createInProgress(jobId, 2, RunnerId.of("7"), now).getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(JobHistory.createSuccess(jobId, 3, RunnerId.of("7"), now).getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(JobHistory.createFailed(jobId, 3, RunnerId.of("7"), now).getStatus()).isEqualTo(JobStatus.FAILED);
    }
    @Test void waitingAndRunnerFlags_followHistoryState() {
        JobHistory pending = JobHistory.createPending(JobId.of("1"), LocalDateTime.now());
        assertThat(pending.isWaitingForRunner()).isTrue();
        assertThat(pending.hasRunner()).isFalse();
        JobHistory running = JobHistory.createInProgress(JobId.of("1"), 2, RunnerId.of("7"), LocalDateTime.now());
        assertThat(running.isWaitingForRunner()).isFalse();
        assertThat(running.hasRunner()).isTrue();
    }
}
