package io.jgitkins.runner.infrastructure.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.jgitkins.runner.application.port.out.JobFetchPort;
import io.jgitkins.runner.application.port.out.JobResultReportPort;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.infrastructure.helper.EndpointPaths;
import io.jgitkins.runner.infrastructure.helper.GrpcSignatureHelper;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobDispatchResponse;
import io.jgitkins.server.grpc.JobDispatchServiceGrpc;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultResponse;
import io.jgitkins.server.grpc.JobResultStatus;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobGrpcClient implements JobFetchPort, JobResultReportPort {

    private ManagedChannel channel;
    private JobDispatchServiceGrpc.JobDispatchServiceBlockingStub blockingStub;
    private String configuredHost;
    private int configuredPort;

    @Override
    public Optional<JobPayload> fetchJob(RunnerConfiguration configuration) {
//        if (configuration == null || !configuration.isReadyForScheduling()) {
//            return Optional.empty();
//        }

        ensureChannel(configuration);
        log.info("ensuring channel ... ");
        if (blockingStub == null) {
            return Optional.empty();
        }

        JobDispatchRequest dispatchRequest = JobDispatchRequest.newBuilder()
                                                       .setRunnerToken(configuration.getRunnerToken())
                                                       .build();
        try {
            JobDispatchResponse jobDispatchResponse = signedStub(configuration, EndpointPaths.Grpc.JOB_REQUEST)
                    .requestJob(dispatchRequest);
            if (jobDispatchResponse.getHasJob()) {
                return Optional.of(jobDispatchResponse.getJob());
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            log.error("Job request failed message: [{}]", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void reportResult(RunnerConfiguration configuration, long jobId, JobResultStatus status) {
        if (configuration == null || !configuration.isReadyForScheduling()) {
            log.warn("Runner 설정이 완료되지 않아 Job 결과를 보고할 수 없습니다.");
            return;
        }
        ensureChannel(configuration);
        if (blockingStub == null) {
            return;
        }

        JobResultRequest request = JobResultRequest.newBuilder()
                                                   .setRunnerToken(configuration.getRunnerToken())
                                                   .setJobId(jobId)
                                                   .setStatus(status)
                                                   .build();
        try {
            JobResultResponse response = signedStub(configuration, EndpointPaths.Grpc.JOB_RESULT)
                    .reportJobResult(request);
            log.info("Job result acknowledged? {}", response.getAccepted());
        } catch (StatusRuntimeException e) {
            log.error("Failed to report job result for job {}", jobId, e);
        }
    }

    private synchronized void ensureChannel(RunnerConfiguration configuration) {
        if (configuration == null || configuration.grpcHost() == null || configuration.grpcHost().isBlank()) {
            return;
        }
        Integer port = configuration.grpcPort();
        if (port == null || port <= 0) {
            return;
        }
        if (channel != null && configuration.grpcHost().equals(configuredHost) && port == configuredPort) {
            return;
        }

        shutdownChannel();
        channel = ManagedChannelBuilder.forAddress(configuration.grpcHost(), port)
                                       .usePlaintext()
                                       .build();
        blockingStub = JobDispatchServiceGrpc.newBlockingStub(channel);
        configuredHost = configuration.grpcHost();
        configuredPort = port;
    }

    private JobDispatchServiceGrpc.JobDispatchServiceBlockingStub signedStub(RunnerConfiguration configuration,
                                                                             String path) {
        if (blockingStub == null) {
            return null;
        }
        return GrpcSignatureHelper.attach(blockingStub, configuration.getRunnerToken(), path);
    }

    private void shutdownChannel() {
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            channel = null;
            blockingStub = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        shutdownChannel();
    }
}
