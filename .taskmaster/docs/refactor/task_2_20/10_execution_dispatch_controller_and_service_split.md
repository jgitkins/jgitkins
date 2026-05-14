# 10. Execution Dispatch Controller and Service Split 상세 계획

## 목적

Job dispatch inbound adapter를 얇게 유지하고, `JobDispatchService`의 책임을 읽기 쉬운 단위로 분리한다. token empty 검증은 현재 단계에서는 application 쪽에 유지하되, presentation validation으로 옮길 수 있는 조건도 함께 정리한다.

## 배경

현재 dispatch 흐름은 controller와 service가 모두 변환, 검증, orchestration을 나눠 갖고 있다.

```text
JobDispatchGrpcController
    -> DispatchJobCommand 생성
    -> JobDispatchUseCase.dispatch(...)
    -> JobDispatchResult -> JobPayload 변환

JobDispatchService
    -> runner token 검증
    -> runner 조회
    -> dispatch 대상 조회
    -> job publish
    -> optimistic append
    -> result 조립
```

이 구조는 동작은 맞지만 읽기 비용이 높다. 특히 controller는 gRPC adapter임에도 payload 변환 로직과 값 변환 helper를 직접 가지고 있고, service는 orchestration과 result assembly를 한 클래스에서 동시에 처리한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/execution/presentation/api/grpc/JobDispatchGrpcController.java
server/src/main/java/io/jgitkins/server/execution/application/service/JobDispatchService.java
server/src/main/java/io/jgitkins/server/execution/application/contract/command/DispatchJobCommand.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/JobDispatchResult.java
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/DispatchableJob.java
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/RunnerDispatchContext.java
server/src/main/java/io/jgitkins/server/execution/application/port/out/JobDispatchQueryPort.java
server/src/main/java/io/jgitkins/server/execution/application/support/ExecutionRequestService.java
server/src/main/java/io/jgitkins/server/execution/application/validate/JobCreationValidator.java
```

현재 controller는 다음 책임을 가진다.

- `DispatchJobCommand` 생성
- `JobDispatchResult -> JobPayload` 변환
- primitive 필드의 null fallback 변환
- gRPC request/response 흐름 제어

현재 service는 다음 책임을 가진다.

- runner token blank 검증
- runner 조회 및 dispatch scope 생성
- dispatch 대상 조회
- job publish 및 history append
- dispatch result 생성

## 문제점

- `JobDispatchGrpcController`가 thin adapter가 아니라 변환 로직을 직접 가진다.
- `JobDispatchService`가 orchestration과 result assembly를 동시에 수행한다.
- blank token 검증이 controller에 없고 service에만 있어서, presentation validation으로 옮길지 정책이 불명확하다.
- `RunnerDispatchContext` 생성과 `JobDispatchResult` 생성이 service 내부에 숨어 있어 재사용/테스트가 분리되지 않는다.

## 설계 원칙

1. controller는 요청 수신과 응답 송신만 맡는다.
2. result/command 변환은 presentation mapper로 분리한다.
3. orchestration은 application service에 남긴다.
4. runner token blank 검증은 현재 단계에서는 application에 유지한다.
5. presentation validation으로 옮기려면 gRPC invalid argument 처리 정책이 먼저 정의돼야 한다.

## TO-BE

### 1. Presentation mapper 추가

```text
server/src/main/java/io/jgitkins/server/execution/presentation/mapper/JobDispatchGrpcMapper.java
```

책임은 다음과 같다.

- `JobDispatchRequest -> DispatchJobCommand`
- `JobDispatchResult -> JobPayload`
- `JobResultRequest -> JobResultReportCommand`
- `JobResultStatus` 변환

### 2. Application support 분리

```text
server/src/main/java/io/jgitkins/server/execution/application/support/RunnerDispatchContextResolver.java
server/src/main/java/io/jgitkins/server/execution/application/support/JobDispatchResultAssembler.java
```

책임은 다음과 같다.

- `RunnerDispatchContextResolver`는 runner token으로 runner를 찾고 dispatch context를 만든다.
- `JobDispatchResultAssembler`는 publish/append 이후 결과 객체를 조립한다.

### 3. Service 단순화

`JobDispatchService`는 아래 순서만 가진다.

```text
resolve runner context
    -> fetch next job
    -> publish and append
    -> assemble result
```

## 코드 스니펫

### Presentation mapper

```java
package io.jgitkins.server.execution.presentation.mapper;

import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.command.JobResultReportCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.application.contract.result.JobResultStatus;
import io.jgitkins.server.grpc.JobDispatchRequest;
import io.jgitkins.server.grpc.JobPayload;
import io.jgitkins.server.grpc.JobResultRequest;
import io.jgitkins.server.grpc.JobResultStatus;
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

    private JobResultStatus toApplicationStatus(JobResultStatus status) {
        return switch (status) {
            case JOB_RESULT_FAILED -> JobResultStatus.FAILED;
            case JOB_RESULT_SUCCESS, UNRECOGNIZED -> JobResultStatus.SUCCESS;
        };
    }
}
```

### Context resolver

```java
package io.jgitkins.server.execution.application.support;

import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerDispatchContextResolver {

    private final RunnerRepository runnerRepository;

    public Optional<RunnerDispatchContext> resolve(String runnerToken) {
        if (runnerToken == null || runnerToken.isBlank()) {
            return Optional.empty();
        }

        return runnerRepository.findByToken(runnerToken)
                .map(this::toContext);
    }

    private RunnerDispatchContext toContext(Runner runner) {
        return new RunnerDispatchContext(
                runner.getId(),
                JobDispatchScope.valueOf(runner.getScopeType().name()),
                runner.getScopeTargetId()
        );
    }
}
```

### Result assembler

```java
package io.jgitkins.server.execution.application.support;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.domain.aggregate.Job;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JobDispatchResultAssembler {

    public JobDispatchResult assemble(RunnerDispatchContext runnerContext,
                                      DispatchableJob dispatchableJob,
                                      Job job,
                                      Long jobHistoryId,
                                      String cloneUrl) {
        return new JobDispatchResult(
                dispatchableJob.jobId(),
                jobHistoryId,
                runnerContext.runnerId(),
                job.getRepositoryId().getValue(),
                dispatchableJob.organizeId(),
                job.getCommitHash().getValue(),
                job.getBranchName().getValue(),
                job.getTriggeredBy().getValue(),
                LocalDateTime.now(),
                cloneUrl
        );
    }
}
```

### Thin controller

```java
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobDispatchGrpcController extends JobDispatchServiceGrpc.JobDispatchServiceImplBase {

    private final JobDispatchUseCase jobDispatchUseCase;
    private final JobResultReportUseCase jobResultReportUseCase;
    private final JobDispatchGrpcMapper jobDispatchGrpcMapper;

    @Override
    public void requestJob(JobDispatchRequest request, StreamObserver<JobDispatchResponse> responseObserver) {
        JobDispatchResult dispatchResult = jobDispatchUseCase.dispatch(
                jobDispatchGrpcMapper.toDispatchCommand(request)
        ).orElse(null);

        JobDispatchResponse response = dispatchResult == null
                ? JobDispatchResponse.newBuilder().setHasJob(false).build()
                : JobDispatchResponse.newBuilder().setHasJob(true).setJob(jobDispatchGrpcMapper.toPayload(dispatchResult)).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

## token empty 검증 위치에 대한 결론

현재 단계의 권장안은 token empty 검증을 application에 유지하는 것이다.

이유는 다음과 같다.

- gRPC presentation 계층에 validation 실패 정책이 아직 없다.
- blank token은 단순 DTO 형식 오류라기보다 dispatch 불가 조건에 가깝다.
- service의 `Optional.empty()` 반환 흐름과 잘 맞는다.

presentation으로 옮기려면 다음이 먼저 필요하다.

- gRPC 요청 실패를 `INVALID_ARGUMENT`로 표준화할 정책
- gRPC 예외 매핑기
- request validator

따라서 이번 단계에서는 presentation validation으로 옮기지 않는다.

## 구현 순서

1. `JobDispatchGrpcMapper`를 추가한다.
2. `RunnerDispatchContextResolver`를 추가한다.
3. `JobDispatchResultAssembler`를 추가한다.
4. `JobDispatchGrpcController`에서 payload 변환과 status 변환을 제거한다.
5. `JobDispatchService`를 resolver/assembler 기반으로 단순화한다.
6. token blank 검증은 service/resolver에 유지한다.

## 테스트 기준

- controller 테스트는 request/response 흐름만 검증한다.
- mapper 테스트는 command/result 변환만 검증한다.
- service 테스트는 orchestration과 dispatch 성공/실패 분기만 검증한다.
- blank token은 application service 또는 resolver에서 여전히 empty 결과로 처리된다.

## 완료 기준

- `JobDispatchGrpcController`는 thin adapter가 된다.
- `JobDispatchService`는 orchestrator 수준으로 읽힌다.
- token validation 정책은 presentation 이동 여부와 분리되어 명확해진다.
- `./gradlew :server:test`가 통과한다.

## 네이밍 및 분리 결론

- `JobDispatchGrpcController`는 유지한다.
- 클래스명 변경은 계약 개선에 비해 영향 범위가 크고, gRPC 서비스명과 직접 연결되지 않으므로 이번 범위에서는 하지 않는다.
- dispatch와 report를 서로 다른 public controller로 나누는 것은 현재 proto `JobDispatchService` 계약과 맞지 않는다.
- 필요하면 내부적으로 `JobDispatchGrpcMapper`와 use case delegate를 분리해서 책임만 얇게 만든다.
