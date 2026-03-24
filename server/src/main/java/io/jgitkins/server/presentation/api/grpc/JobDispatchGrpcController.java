package io.jgitkins.server.presentation.api.grpc;

import io.grpc.stub.StreamObserver;
import io.jgitkins.server.application.dto.command.JobResultReportCommand;
import io.jgitkins.server.application.dto.command.DispatchJobCommand;
import io.jgitkins.server.application.dto.JobResultStatus;
import io.jgitkins.server.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.application.port.in.JobResultReportUseCase;
import io.jgitkins.server.application.dto.result.JobDispatchResult;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobDispatchResponse;
import io.jgitkins.server.grpc.JobDispatchServiceGrpc;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobDispatchGrpcController extends JobDispatchServiceGrpc.JobDispatchServiceImplBase {

    private final JobDispatchUseCase jobDispatchUseCase;
    private final JobResultReportUseCase jobResultReportUseCase;

    @Override
    public void requestJob(JobDispatchRequest request, StreamObserver<JobDispatchResponse> responseObserver) {
        log.debug("request: ");
        DispatchJobCommand command = new DispatchJobCommand(request.getRunnerToken());

        Optional<JobDispatchResult> dispatchResult = jobDispatchUseCase.dispatch(command);
        log.info("dispatchResult: [{}]", dispatchResult);

        JobDispatchResponse.Builder responseBuilder = JobDispatchResponse.newBuilder();

        if (dispatchResult.isPresent()) {
            responseBuilder.setHasJob(true)
                           .setJob(toPayload(dispatchResult.get()));
        } else {
            responseBuilder.setHasJob(false);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void reportJobResult(JobResultRequest request, StreamObserver<JobResultResponse> responseObserver) {
        JobResultReportCommand command = JobResultReportCommand.builder()
                                                               .runnerToken(request.getRunnerToken())
                                                               .jobId(request.getJobId())
                                                               .status(convertStatus(request.getStatus()))
                                                               .build();
        log.debug("receive result");
        jobResultReportUseCase.reportJobResult(command);

        responseObserver.onNext(JobResultResponse.newBuilder()
                                                 .setAccepted(true)
                                                 .setMessage("Result recorded")
                                                 .build());
        responseObserver.onCompleted();
    }

    private JobPayload toPayload(JobDispatchResult result) {
        return JobPayload.newBuilder()
                         .setJobId(toLong(result.jobId()))
                         .setJobHistoryId(toLong(result.jobHistoryId()))
                         .setRunnerId(toLong(result.runnerId()))
                         .setRepositoryId(toLong(result.repositoryId()))
                         .setOrganizeId(toLong(result.organizeId()))
                         .setCommitHash(toString(result.commitHash()))
                         .setBranchName(toString(result.branchName()))
                         .setTriggeredBy(toLong(result.triggeredBy()))
                         .setCloneUrl(toString(result.cloneUrl()))
                         .build();
    }

    private JobResultStatus convertStatus(io.jgitkins.server.grpc.JobResultStatus status) {
        return switch (status) {
            case JOB_RESULT_FAILED -> JobResultStatus.FAILED;
            case JOB_RESULT_SUCCESS, UNRECOGNIZED -> JobResultStatus.SUCCESS;
        };
    }

    private long toLong(Long value) {
        return value == null ? 0L : value;
    }

    private String toString(String value) {
        return value == null ? "" : value;
    }
}
