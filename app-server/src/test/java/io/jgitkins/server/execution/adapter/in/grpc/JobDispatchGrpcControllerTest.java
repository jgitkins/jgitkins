package io.jgitkins.server.execution.adapter.in.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.stub.StreamObserver;
import io.jgitkins.server.execution.application.contract.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.JobResultReportCommand;
import io.jgitkins.server.execution.application.contract.JobDispatchResult;
import io.jgitkins.server.execution.application.contract.internal.JobResultStatus;
import io.jgitkins.server.execution.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.execution.application.port.in.JobResultReportUseCase;
import io.jgitkins.server.execution.adapter.in.grpc.translator.JobDispatchGrpcMapper;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobDispatchResponse;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobDispatchGrpcControllerTest {

    @Mock
    private JobDispatchUseCase jobDispatchUseCase;

    @Mock
    private JobResultReportUseCase jobResultReportUseCase;

    @Mock
    private JobDispatchGrpcMapper jobDispatchGrpcMapper;

    @InjectMocks
    private JobDispatchGrpcController controller;

    @Test
    void requestJob_setsJobIdDirectly() {
        JobDispatchRequest request = JobDispatchRequest.newBuilder()
                .setRunnerToken("token")
                .build();
        StreamObserver<JobDispatchResponse> responseObserver = mock(StreamObserver.class);
        DispatchJobCommand command = new DispatchJobCommand("token");
        JobDispatchResult result = new JobDispatchResult(
                101L,
                999L,
                7L,
                55L,
                12L,
                "abc123",
                "main",
                3L,
                LocalDateTime.of(2026, 3, 12, 10, 30),
                "https://git.example/org/repo.git"
        );

        when(jobDispatchGrpcMapper.toDispatchCommand(request)).thenReturn(command);
        when(jobDispatchUseCase.dispatch(any(DispatchJobCommand.class)))
                .thenReturn(Optional.of(result));
        when(jobDispatchGrpcMapper.toPayload(result)).thenReturn(JobPayload.newBuilder()
                .setJobId(101L)
                .setJobHistoryId(999L)
                .setRunnerId(7L)
                .setRepositoryId(55L)
                .setOrganizeId(12L)
                .setCommitHash("abc123")
                .setBranchName("main")
                .setTriggeredBy(3L)
                .setCloneUrl("https://git.example/org/repo.git")
                .build());

        controller.requestJob(request, responseObserver);

        ArgumentCaptor<JobDispatchResponse> responseCaptor = ArgumentCaptor.forClass(JobDispatchResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        JobDispatchResponse response = responseCaptor.getValue();
        assertThat(response.getHasJob()).isTrue();
        JobPayload payload = response.getJob();
        assertThat(payload.getJobId()).isEqualTo(101L);
        assertThat(payload.getJobHistoryId()).isEqualTo(999L);
        assertThat(payload.getRunnerId()).isEqualTo(7L);
        assertThat(payload.getRepositoryId()).isEqualTo(55L);
        assertThat(payload.getOrganizeId()).isEqualTo(12L);
        assertThat(payload.getCommitHash()).isEqualTo("abc123");
        assertThat(payload.getBranchName()).isEqualTo("main");
        assertThat(payload.getTriggeredBy()).isEqualTo(3L);
        assertThat(payload.getCloneUrl()).isEqualTo("https://git.example/org/repo.git");
    }

    @Test
    void reportJobResult_forwardsCommand() {
        JobResultRequest request = JobResultRequest.newBuilder()
                .setRunnerToken("token")
                .setJobId(101L)
                .setStatus(io.jgitkins.server.grpc.JobResultStatus.JOB_RESULT_SUCCESS)
                .build();
        StreamObserver<JobResultResponse> responseObserver = mock(StreamObserver.class);
        JobResultReportCommand expectedCommand = new JobResultReportCommand(
                "token",
                101L,
                io.jgitkins.server.execution.application.contract.internal.JobResultStatus.SUCCESS);

        when(jobDispatchGrpcMapper.toResultReportCommand(request)).thenReturn(expectedCommand);

        controller.reportJobResult(request, responseObserver);

        verify(jobDispatchGrpcMapper).toResultReportCommand(request);
        verify(jobResultReportUseCase).reportJobResult(expectedCommand);
        verify(responseObserver).onNext(JobResultResponse.newBuilder()
                .setAccepted(true)
                .setMessage("Result recorded")
                .build());
        verify(responseObserver).onCompleted();
    }
}
