package io.jgitkins.server.execution.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import org.junit.jupiter.api.Test;

class JobTest {
    @Test void create_startsPending() {
        Job job = Job.create(ExecutionRepositoryId.of(1L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(2L));
        assertThat(job.getCurrentStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getHistories()).hasSize(1);
    }
    @Test void publish_transitionsToInProgressAndAddsHistory() {
        Job job = Job.create(ExecutionRepositoryId.of(1L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(2L));
        job.publish(RunnerId.of("7"));
        assertThat(job.getCurrentStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(job.getHistories()).hasSize(2);
        assertThat(job.getLatestHistory().getRunnerId().getValue()).isEqualTo("7");
    }
    @Test void publish_rejectsNonPendingJob() {
        Job job = Job.create(ExecutionRepositoryId.of(1L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(2L));
        job.publish(RunnerId.of("7"));
        assertThatThrownBy(() -> job.publish(RunnerId.of("8"))).isInstanceOf(IllegalStateException.class);
    }
}
