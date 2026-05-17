package io.jgitkins.server.execution.presentation.api.grpc;

import io.grpc.stub.StreamObserver;
import io.jgitkins.server.execution.application.port.in.JobDispatchUseCase;
import io.jgitkins.server.execution.application.port.in.JobResultReportUseCase;
import io.jgitkins.server.execution.presentation.mapper.JobDispatchGrpcMapper;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobDispatchResponse;
import io.jgitkins.server.grpc.JobDispatchServiceGrpc;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobDispatchGrpcController extends JobDispatchServiceGrpc.JobDispatchServiceImplBase {

    private final JobDispatchUseCase jobDispatchUseCase;
    private final JobResultReportUseCase jobResultReportUseCase;
    private final JobDispatchGrpcMapper jobDispatchGrpcMapper;

    @Override
    public void requestJob(JobDispatchRequest request, StreamObserver<JobDispatchResponse> responseObserver) {
        log.debug("request: ");
        JobDispatchResponse response = jobDispatchUseCase.dispatch(jobDispatchGrpcMapper.toDispatchCommand(request))
                .map(result -> JobDispatchResponse.newBuilder()
                        .setHasJob(true)
                        .setJob(jobDispatchGrpcMapper.toPayload(result))
                        .build())
                .orElseGet(() -> JobDispatchResponse.newBuilder()
                        .setHasJob(false)
                        .build());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void reportJobResult(JobResultRequest request, StreamObserver<JobResultResponse> responseObserver) {
        log.debug("receive result");
        jobResultReportUseCase.reportJobResult(jobDispatchGrpcMapper.toResultReportCommand(request));

        responseObserver.onNext(JobResultResponse.newBuilder()
                                                 .setAccepted(true)
                                                 .setMessage("Result recorded")
                                                 .build());
        responseObserver.onCompleted();
    }
}
