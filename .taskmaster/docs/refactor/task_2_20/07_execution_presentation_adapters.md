# 07. Execution Presentation Adapters 상세 계획

## 목적

Execution Context의 REST/gRPC inbound adapter와 request/response mapper를 `execution.presentation`으로 이동한다. 공통 presentation infrastructure는 top-level에 유지한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/presentation/api/rest/RunnerController.java
server/src/main/java/io/jgitkins/server/presentation/api/grpc/JobDispatchGrpcController.java
server/src/main/java/io/jgitkins/server/presentation/dto/RunnerCreateRequest.java
server/src/main/java/io/jgitkins/server/presentation/dto/RunnerActivateRequest.java
server/src/main/java/io/jgitkins/server/presentation/dto/RunnerResponse.java
server/src/main/java/io/jgitkins/server/presentation/mapper/RunnerRequestMapper.java
server/src/main/java/io/jgitkins/server/presentation/mapper/RunnerResponseMapper.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/presentation/api/rest/RunnerController.java
server/src/main/java/io/jgitkins/server/execution/presentation/api/grpc/JobDispatchGrpcController.java
server/src/main/java/io/jgitkins/server/execution/presentation/dto/RunnerCreateRequest.java
server/src/main/java/io/jgitkins/server/execution/presentation/dto/RunnerActivateRequest.java
server/src/main/java/io/jgitkins/server/execution/presentation/dto/RunnerResponse.java
server/src/main/java/io/jgitkins/server/execution/presentation/mapper/RunnerRequestMapper.java
server/src/main/java/io/jgitkins/server/execution/presentation/mapper/RunnerResponseMapper.java
```

유지:

```text
server/src/main/java/io/jgitkins/server/presentation/common/ApiResponse.java
server/src/main/java/io/jgitkins/server/presentation/advice/GlobalExceptionHandler.java
server/src/main/java/io/jgitkins/server/presentation/advice/mapper/*
server/src/main/java/io/jgitkins/server/grpc/*
```

## RunnerController 스니펫

```java
package io.jgitkins.server.execution.presentation.api.rest;

import io.jgitkins.server.execution.application.contract.command.RunnerRegisterCommand;
import io.jgitkins.server.execution.application.contract.result.RunnerActivateResult;
import io.jgitkins.server.execution.application.contract.result.RunnerDetailResult;
import io.jgitkins.server.execution.application.contract.result.RunnerRegistrationResult;
import io.jgitkins.server.execution.application.port.in.RunnerActivateUseCase;
import io.jgitkins.server.execution.application.port.in.RunnerDeleteUseCase;
import io.jgitkins.server.execution.application.port.in.RunnerLoadUseCase;
import io.jgitkins.server.execution.application.port.in.RunnerRegisterUseCase;
import io.jgitkins.server.execution.presentation.dto.RunnerActivateRequest;
import io.jgitkins.server.execution.presentation.dto.RunnerCreateRequest;
import io.jgitkins.server.execution.presentation.dto.RunnerResponse;
import io.jgitkins.server.execution.presentation.mapper.RunnerRequestMapper;
import io.jgitkins.server.execution.presentation.mapper.RunnerResponseMapper;
import io.jgitkins.server.presentation.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runners")
public class RunnerController {

    private final RunnerRegisterUseCase runnerRegisterUseCase;
    private final RunnerLoadUseCase runnerLoadUseCase;
    private final RunnerDeleteUseCase runnerDeleteUseCase;
    private final RunnerActivateUseCase runnerActivateUseCase;
    private final RunnerRequestMapper runnerRequestMapper;
    private final RunnerResponseMapper runnerResponseMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RunnerRegistrationResult>> registerRunner(
            @Valid @RequestBody RunnerCreateRequest request
    ) {
        RunnerRegisterCommand command = runnerRequestMapper.toCommand(request);
        RunnerRegistrationResult result = runnerRegisterUseCase.register(command);
        return ApiResponse.created(result.runnerId(), result);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RunnerResponse>>> getRunners() {
        List<RunnerDetailResult> results = runnerLoadUseCase.getRunners();
        return ApiResponse.ok(runnerResponseMapper.toResponses(results));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<RunnerActivateResult>> activateRunner(
            @Valid @RequestBody RunnerActivateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        RunnerActivateResult result = runnerActivateUseCase.activate(request.token(), extractClientIp(httpServletRequest));
        return ApiResponse.ok(result);
    }
}
```

## JobDispatchGrpcController 스니펫

```java
package io.jgitkins.server.execution.presentation.api.grpc;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobDispatchGrpcController extends JobDispatchServiceGrpc.JobDispatchServiceImplBase {

    private final JobDispatchUseCase jobDispatchUseCase;
    private final JobResultReportUseCase jobResultReportUseCase;

    @Override
    public void requestJob(JobDispatchRequest request, StreamObserver<JobDispatchResponse> responseObserver) {
        DispatchJobCommand command = new DispatchJobCommand(request.getRunnerToken());
        Optional<JobDispatchResult> dispatchResult = jobDispatchUseCase.dispatch(command);

        JobDispatchResponse.Builder responseBuilder = JobDispatchResponse.newBuilder();
        dispatchResult.ifPresentOrElse(
                result -> responseBuilder.setHasJob(true).setJob(toPayload(result)),
                () -> responseBuilder.setHasJob(false));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void reportJobResult(JobResultRequest request, StreamObserver<JobResultResponse> responseObserver) {
        JobResultReportCommand command = new JobResultReportCommand(
                request.getRunnerToken(),
                request.getJobId(),
                convertStatus(request.getStatus()));

        jobResultReportUseCase.reportJobResult(command);

        responseObserver.onNext(JobResultResponse.newBuilder()
                .setAccepted(true)
                .setMessage("Result recorded")
                .build());
        responseObserver.onCompleted();
    }
}
```

## Mapper 스니펫

```java
package io.jgitkins.server.execution.presentation.mapper;

@Component
public class RunnerRequestMapper {

    public RunnerRegisterCommand toCommand(RunnerCreateRequest request) {
        return new RunnerRegisterCommand(request.description(), request.scopeType(), request.targetId());
    }
}
```

```java
package io.jgitkins.server.execution.presentation.mapper;

@Component
public class RunnerResponseMapper {

    public RunnerResponse toResponse(RunnerDetailResult result) {
        return new RunnerResponse(
                result.id(),
                result.description(),
                result.status(),
                result.scopeType(),
                result.scopeTargetId(),
                result.ipAddress(),
                result.lastHeartbeatAt(),
                result.createdAt());
    }

    public List<RunnerResponse> toResponses(List<RunnerDetailResult> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
```

## 구현 순서

1. runner presentation DTO/mapper를 execution presentation으로 이동한다.
2. `RunnerController`를 execution presentation REST 패키지로 이동한다.
3. `JobDispatchGrpcController`를 execution presentation gRPC 패키지로 이동한다.
4. 공통 `ApiResponse`, generated gRPC classes, exception handler는 이동하지 않는다.
5. 테스트 import와 slice scan 범위를 수정한다.

## 테스트 기준

- Runner register/list/get/delete/activate controller tests가 통과한다.
- gRPC request job은 job present/empty 응답을 유지한다.
- gRPC report result는 `accepted=true` 응답을 유지한다.
- API path `/api/runners`는 변경하지 않는다.
- gRPC service name은 변경하지 않는다.

## 완료 기준

- execution presentation package로 adapter가 이동했다.
- 공통 presentation asset은 top-level에 남아 있다.
- REST/gRPC 외부 계약은 변경되지 않았다.
- `./gradlew :server:test`가 통과한다.
