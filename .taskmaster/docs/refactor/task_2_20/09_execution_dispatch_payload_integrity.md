# 09. Execution Dispatch Payload Integrity 상세 계획

## 목적

Job dispatch 경로에서 `JobId` 문자열을 억지로 `Long`으로 변환하는 가정을 제거하고, dispatch payload가 실제 persistence id와 일치하도록 정리한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/execution/domain/aggregate/Job.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/JobId.java
server/src/main/java/io/jgitkins/server/execution/application/service/JobDispatchService.java
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/DispatchableJob.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/JobDispatchResult.java
server/src/main/java/io/jgitkins/server/execution/presentation/api/grpc/JobDispatchGrpcController.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/JobDispatchQueryAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/JobRepositoryAdapter.java
```

현재 구조는 다음 흐름을 가진다.

```java
private Long parseJobId(Job job) {
    try {
        return Long.parseLong(job.getId().getValue());
    } catch (NumberFormatException ex) {
        return null;
    }
}
```

```java
private long toLong(Long value) {
    return value == null ? 0L : value;
}
```

```java
private String toString(String value) {
    return value == null ? "" : value;
}
```

문제는 `JobId.generate()`가 `JOB_` prefix를 포함하는 문자열을 생성한다는 점이다. 즉 `JobId`는 생성 시점과 persistence 복원 시점에 서로 다른 의미를 가진다. 이 가정이 응답 payload와 repository adapter 곳곳에 흩어져 있다.

## 문제점

- `JobDispatchService`가 dispatch domain object에서 다시 숫자 id를 파싱한다.
- `JobDispatchGrpcController`가 null을 0과 빈 문자열로 바꾸는 처리로 잘못된 상태를 숨긴다.
- `DispatchableJob`가 numeric job id를 별도로 가지지 않아서, read seam이 이미 알고 있는 DB id를 버린다.
- `JobRepositoryAdapter.appendHistoryIfCurrent(...)`와 `JobDispatchService`가 동일한 job id 해석을 각자 수행한다.
- payload가 domain model과 persistence model 사이의 차이를 명시하지 않는다.

## TO-BE

dispatch read seam이 numeric job id를 명시적으로 전달한다.

```text
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/DispatchableJob.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/JobDispatchQueryAdapter.java
server/src/main/java/io/jgitkins/server/execution/application/service/JobDispatchService.java
server/src/main/java/io/jgitkins/server/execution/presentation/api/grpc/JobDispatchGrpcController.java
```

기준은 다음과 같다.

- `DispatchableJob`는 `Long jobId`를 명시적으로 포함한다.
- `JobDispatchQueryAdapter`는 query row의 numeric id를 `DispatchableJob`에 넣는다.
- `JobDispatchService`는 `job.getId()`를 다시 파싱하지 않는다.
- `JobDispatchGrpcController`는 `jobId`에 대한 fallback을 제거하고, proto primitive 필드에 대한 기존 변환 규칙은 유지한다.
- `JobDispatchResult.jobId`는 dispatch 성공 시 항상 실제 numeric id를 담는다.

## 코드 스니펫

```java
package io.jgitkins.server.execution.application.contract.internal;

import io.jgitkins.server.execution.domain.aggregate.Job;

public record DispatchableJob(Long jobId,
                              Job job,
                              Long organizeId,
                              String repositoryClonePath) {
}
```

```java
public Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context) {
    DispatchableJobRow row = jobDispatchQueryMapper.selectNextDispatchableJob(
            context.dispatchScope().name(),
            context.scopeTargetId()
    );
    if (row == null) {
        return Optional.empty();
    }

    List<JobHistory> histories = loadHistories(row.jobId());
    Long organizeId = "ORGANIZATION".equals(row.repositoryOwnerType()) ? row.repositoryOwnerId() : null;
    Job job = jobDomainMapper.toDomain(row, histories);

    return Optional.of(new DispatchableJob(row.jobId(), job, organizeId, row.repositoryClonePath()));
}
```

```java
private JobDispatchResult buildDispatchResult(RunnerDispatchContext runnerContext,
                                              DispatchableJob dispatchableJob,
                                              Job job,
                                              Long jobHistoryId) {
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
            cloneUrlBuilder.build(dispatchableJob.repositoryClonePath())
    );
}
```

```java
private JobPayload toPayload(JobDispatchResult result) {
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
```

`parseJobId(...)`는 삭제한다.

`toLong(...)`, `toString(...)`는 proto primitive 필드를 채우기 위한 기존 변환 규칙으로 유지한다.

## 네이밍 정리

payload 정합성 이슈와 별개로, dispatch/read 경로의 의도를 더 분명하게 드러내기 위해 다음 rename을 반영한다.

- `findNextDispatchableJob`는 `fetchNextJob`로 바꾼다.
- `loadHistories`는 `getHistories`로 바꾼다.

이 rename은 동작 변경이 아니라 표현 정리다. 따라서 payload 타입 정합성 작업과 분리해서 적용해도 된다.

## 구현 순서

1. `DispatchableJob`에 numeric `jobId`를 추가한다.
2. `JobDispatchQueryAdapter`에서 query row id를 함께 전달한다.
3. `JobDispatchService`에서 parse helper를 제거한다.
4. `JobDispatchGrpcController`에서 `jobId` fallback을 제거하고, proto primitive 필드 변환은 유지한다.
5. 테스트에서 numeric id가 그대로 내려가는지 검증한다.

## 테스트 기준

- dispatch 성공 시 gRPC payload의 `jobId`가 query row id와 일치한다.
- dispatch 성공 시 `jobId=0` 같은 대체값이 더 이상 발생하지 않는다.
- dispatch 실패 시 payload는 `hasJob=false`만 반환한다.
- `JobDispatchServiceTest`와 `JobDispatchGrpcControllerTest`가 id 정합성을 확인한다.

## 완료 기준

- job dispatch 경로에 문자열 job id를 숫자로 다시 바꾸는 코드가 남지 않는다.
- `jobId`에 대한 null fallback이 삭제된다.
- payload가 persistence id와 domain result를 일관되게 반영한다.
