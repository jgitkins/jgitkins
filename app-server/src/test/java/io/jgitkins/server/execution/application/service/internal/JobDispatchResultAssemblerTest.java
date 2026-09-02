package io.jgitkins.server.execution.application.service.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.application.contract.external.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.external.RunnerDispatchContext;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import io.jgitkins.server.shared.domain.model.vo.SequenceNumber;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobDispatchResultAssemblerTest {
    @Test void assemble_preservesProjectionAndAggregateFields() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 20, 12, 0);
        Job job = Job.reconstruct(JobId.of("101"), ExecutionRepositoryId.of(55L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(3L), now,
                List.of(io.jgitkins.server.execution.domain.entity.JobHistory.reconstruct(JobHistoryId.of("1"), JobId.of("101"), SequenceNumber.first(), null, JobStatus.PENDING, ExecutionSystemActor.SYSTEM, now)));
        var result = new JobDispatchResultAssembler().assemble(new RunnerDispatchContext(7L, JobDispatchScope.GLOBAL, null), new DispatchableJob(101L, job, 12L, "/repo"), job, 999L, "https://git/repo");
        assertThat(result).extracting("jobId", "jobHistoryId", "runnerId", "repositoryId", "organizeId", "commitHash", "branchName", "triggeredBy", "cloneUrl")
                .containsExactly(101L, 999L, 7L, 55L, 12L, "abc1234", "main", 3L, "https://git/repo");
    }
}
