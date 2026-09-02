package io.jgitkins.server.execution.adapter.in.grpc.translator;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.application.contract.JobDispatchResult;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JobDispatchGrpcMapperTest {
    private final JobDispatchGrpcMapper mapper = new JobDispatchGrpcMapper();
    @Test void mapsCommandsAndStatus() {
        assertThat(mapper.toDispatchCommand(JobDispatchRequest.newBuilder().setRunnerToken("token").build()).runnerToken()).isEqualTo("token");
        var command = mapper.toResultReportCommand(JobResultRequest.newBuilder().setRunnerToken("token").setJobId(7).setStatus(JobResultStatus.JOB_RESULT_FAILED).build());
        assertThat(command.jobId()).isEqualTo(7L);
        assertThat(command.status()).isEqualTo(io.jgitkins.server.execution.application.contract.internal.JobResultStatus.FAILED);
    }
    @Test void payload_preservesNineWireFieldsAndOmitsInternalTimestamp() {
        JobDispatchResult result = new JobDispatchResult(1L, 2L, 3L, 4L, 5L, "commit", "main", 8L, LocalDateTime.now(), "url");
        JobPayload payload = mapper.toPayload(result);
        assertThat(payload.getJobId()).isEqualTo(1L);
        assertThat(payload.getJobHistoryId()).isEqualTo(2L);
        assertThat(payload.getRunnerId()).isEqualTo(3L);
        assertThat(payload.getRepositoryId()).isEqualTo(4L);
        assertThat(payload.getOrganizeId()).isEqualTo(5L);
        assertThat(payload.getCommitHash()).isEqualTo("commit");
        assertThat(payload.getBranchName()).isEqualTo("main");
        assertThat(payload.getTriggeredBy()).isEqualTo(8L);
        assertThat(payload.getCloneUrl()).isEqualTo("url");
        assertThat(JobPayload.getDescriptor().getFields()).hasSize(9);
    }
}
