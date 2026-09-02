package io.jgitkins.server.execution.adapter.in.grpc.translator;

import io.jgitkins.server.execution.application.contract.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.JobResultReportCommand;
import io.jgitkins.server.execution.application.contract.JobDispatchResult;
import io.jgitkins.server.execution.application.contract.internal.JobResultStatus;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import org.springframework.stereotype.Component;

@Component
public class JobDispatchGrpcMapper {

    public DispatchJobCommand toDispatchCommand(JobDispatchRequest request) {
        return new DispatchJobCommand(request.getRunnerToken());
    }

    public JobResultReportCommand toResultReportCommand(JobResultRequest request) {
        return new JobResultReportCommand(
                request.getRunnerToken(),
                request.getJobId(),
                toApplicationStatus(request.getStatus())
        );
    }

    public JobPayload toPayload(JobDispatchResult result) {
        return JobPayload.newBuilder()
                .setJobId(result.jobId())
                .setJobHistoryId(result.jobHistoryId())
                .setRunnerId(result.runnerId())
                .setRepositoryId(result.repositoryId())
                .setOrganizeId(result.organizeId())
                .setCommitHash(result.commitHash())
                .setBranchName(result.branchName())
                .setTriggeredBy(result.triggeredBy())
                .setCloneUrl(result.cloneUrl())
                .build();
    }

    private JobResultStatus toApplicationStatus(io.jgitkins.server.grpc.JobResultStatus status) {
        return switch (status) {
            case JOB_RESULT_FAILED -> JobResultStatus.FAILED;
            case JOB_RESULT_SUCCESS, UNRECOGNIZED -> JobResultStatus.SUCCESS;
        };
    }
}
