# 02. Execution Application Ports 상세 계획

## 목적

Execution Context의 inbound/outbound application seam을 top-level `application.port.*`에서 `execution.application.port.*`와 `execution.domain.repository.*`로 재정렬한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/application/port/in/JobCreateUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/JobDispatchUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/JobResultReportUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/RunnerRegisterUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/RunnerLoadUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/RunnerDeleteUseCase.java
server/src/main/java/io/jgitkins/server/application/port/in/RunnerActivateUseCase.java

server/src/main/java/io/jgitkins/server/application/port/out/JobPersistencePort.java
server/src/main/java/io/jgitkins/server/application/port/out/RunnerPersistencePort.java
server/src/main/java/io/jgitkins/server/application/port/out/PipelineConfigPort.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/application/port/in/JobCreateUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/JobDispatchUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/JobResultReportUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/RunnerRegisterUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/RunnerLoadUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/RunnerDeleteUseCase.java
server/src/main/java/io/jgitkins/server/execution/application/port/in/RunnerActivateUseCase.java

server/src/main/java/io/jgitkins/server/execution/domain/repository/JobRepository.java
server/src/main/java/io/jgitkins/server/execution/domain/repository/RunnerRepository.java
server/src/main/java/io/jgitkins/server/execution/application/port/out/JobDispatchQueryPort.java
server/src/main/java/io/jgitkins/server/execution/application/port/out/PipelineConfigPort.java
server/src/main/java/io/jgitkins/server/execution/application/port/out/PipelineFileLookupPort.java
server/src/main/java/io/jgitkins/server/execution/application/contract/pipeline/PipelineConfig.java
server/src/main/java/io/jgitkins/server/execution/application/contract/pipeline/PipelineRule.java
```

## 결정 사항

- aggregate load/save/update는 domain repository가 담당한다.
- dispatch 대상 조회는 aggregate repository가 아니라 application query port다.
- `PipelineConfigPort`는 execution application port다.
- raw file lookup은 `PipelineConfigPort`에 섞지 않는다. execution이 `PipelineFileLookupPort`를 정의하고, infrastructure adapter가 1차로 기존 `FileGitPort.exists(...)`에 위임한다.
- execution application code는 repository infrastructure adapter나 git adapter 구현체를 직접 import하지 않는다.
- `JobRepository.findById(Long jobId)`는 현 DB/API 계약을 유지하기 위한 1차 선택이다. `JobId` VO 전환은 DB id와 generated domain id 정책을 먼저 정리해야 하므로 이번 단계에서 하지 않는다.
- `RunnerPersistencePort.update(...)`는 `save(...)`와 중복되므로 상세 구현 시 제거한다. 호출부가 있으면 `save(...)`로 통합한다.

## Inbound Port 스니펫

```java
package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.JobCreateCommand;

public interface JobCreateUseCase {
    void create(JobCreateCommand command);
}
```

```java
package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import java.util.Optional;

public interface JobDispatchUseCase {
    Optional<JobDispatchResult> dispatch(DispatchJobCommand command);
}
```

```java
package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.JobResultReportCommand;

public interface JobResultReportUseCase {
    void reportJobResult(JobResultReportCommand command);
}
```

```java
package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.command.RunnerRegisterCommand;
import io.jgitkins.server.execution.application.contract.result.RunnerRegistrationResult;

public interface RunnerRegisterUseCase {
    RunnerRegistrationResult register(RunnerRegisterCommand command);
}
```

```java
package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.execution.application.contract.result.RunnerDetailResult;
import java.util.List;

public interface RunnerLoadUseCase {
    RunnerDetailResult getRunner(Long runnerId);
    List<RunnerDetailResult> getRunners();
}
```

## Domain Repository 스니펫

```java
package io.jgitkins.server.execution.domain.repository;

import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import java.util.Optional;

public interface JobRepository {
    void save(Job job);
    Optional<Job> findById(Long jobId);

    /**
     * Append the latest history from job only if expectedPreviousHistory is still
     * the current latest persisted history.
     *
     * Returns generated history id when append succeeds.
     * Returns Optional.empty() when another dispatcher/reporter already advanced the job.
     */
    Optional<Long> appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory);
}
```

```java
package io.jgitkins.server.execution.domain.repository;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import java.util.List;
import java.util.Optional;

public interface RunnerRepository {
    Runner save(Runner runner);
    void deleteById(Long runnerId);
    Optional<Runner> findById(Long runnerId);
    Optional<Runner> findByToken(String token);
    List<Runner> findAll();
}
```

## Application Query Port 스니펫

```java
package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import java.util.Optional;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
}
```

```java
package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.contract.pipeline.PipelineConfig;

public interface PipelineConfigPort {
    PipelineConfig read(String namespace, String repoName, String commitHash);
}
```

```java
package io.jgitkins.server.execution.application.port.out;

public interface PipelineFileLookupPort {
    boolean exists(String namespace, String repoName, String commitHash, String path);
}
```

## 구현 순서

1. `execution.application.port.in` 패키지를 만들고 inbound port를 이동한다.
2. `execution.domain.repository.JobRepository`, `RunnerRepository`를 만든다.
3. `JobPersistencePort`를 `JobRepository`와 `JobDispatchQueryPort`로 분리한다.
4. `RunnerPersistencePort`를 `RunnerRepository`로 대체한다.
5. `PipelineConfigPort`를 execution application port로 이동한다.
6. `PipelineFileLookupPort`를 execution application port로 추가한다.
7. 기존 import를 이동된 패키지로 수정한다.
8. `ArchitecturePackageConventionTest`에 execution package convention을 단계적으로 추가한다.

## 테스트 기준

- 서비스 테스트는 새 port/repository 타입을 mock으로 사용한다.
- dispatch query는 `JobRepository`가 아니라 `JobDispatchQueryPort` mock으로 검증한다.
- `appendHistoryIfCurrent(...)` stale case는 `Optional.empty()`로 검증한다.
- `PipelineConfigPort` 이동 후 `PushJobCreationPolicyTest`가 유지된다.
- pipeline file existence check는 execution port mock으로 검증한다.

## 완료 기준

- top-level `application.port.in`에서 execution 전용 use case가 제거됐다.
- top-level `application.port.out.JobPersistencePort`, `RunnerPersistencePort`가 제거됐다.
- execution application은 infrastructure package를 import하지 않는다.
- `./gradlew :server:test`가 통과한다.
