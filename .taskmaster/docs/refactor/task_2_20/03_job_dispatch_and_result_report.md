# 03. Job Dispatch / Result Report 상세 계획

## 목적

Job 생성, dispatch, 결과 보고 흐름을 Execution Context application service로 이동하고, aggregate repository와 dispatch query port를 분리한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/application/service/JobService.java
server/src/main/java/io/jgitkins/server/application/service/JobDispatchService.java
server/src/main/java/io/jgitkins/server/application/service/JobResultReportService.java

server/src/main/java/io/jgitkins/server/application/dto/DispatchableJob.java
server/src/main/java/io/jgitkins/server/application/dto/RunnerDispatchContext.java
server/src/main/java/io/jgitkins/server/application/dto/JobDispatchScope.java
server/src/main/java/io/jgitkins/server/application/dto/JobResultStatus.java
server/src/main/java/io/jgitkins/server/application/dto/command/DispatchJobCommand.java
server/src/main/java/io/jgitkins/server/application/dto/command/JobCreateCommand.java
server/src/main/java/io/jgitkins/server/application/dto/command/JobResultReportCommand.java
server/src/main/java/io/jgitkins/server/application/dto/result/JobDispatchResult.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/application/service/JobService.java
server/src/main/java/io/jgitkins/server/execution/application/service/JobDispatchService.java
server/src/main/java/io/jgitkins/server/execution/application/service/JobResultReportService.java

server/src/main/java/io/jgitkins/server/execution/application/contract/internal/DispatchableJob.java
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/RunnerDispatchContext.java
server/src/main/java/io/jgitkins/server/execution/application/contract/internal/JobDispatchScope.java
server/src/main/java/io/jgitkins/server/execution/application/contract/command/DispatchJobCommand.java
server/src/main/java/io/jgitkins/server/execution/application/contract/command/JobCreateCommand.java
server/src/main/java/io/jgitkins/server/execution/application/contract/command/JobResultReportCommand.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/JobDispatchResult.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/JobResultStatus.java
```

## 책임 분리

```text
JobService
    - Job aggregate 생성
    - JobRepository.save(job)

JobDispatchService
    - runner token -> RunnerRepository.findByToken
    - runner -> RunnerDispatchContext
    - JobDispatchQueryPort.findNextDispatchableJob
    - job.publish(runnerId)
    - JobRepository.appendHistoryIfCurrent(job, previousHistory)
    - JobDispatchResult 조립

JobResultReportService
    - runner token -> RunnerRepository.findByToken
    - job id -> JobRepository.findById
    - SUCCESS/FAILED에 따라 job.complete*
    - JobRepository.appendHistoryIfCurrent(job, previousHistory)
```

## JobService 스니펫

```java
package io.jgitkins.server.execution.application.service;

import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.execution.application.contract.command.JobCreateCommand;
import io.jgitkins.server.execution.application.port.in.JobCreateUseCase;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService implements JobCreateUseCase {

    private final JobRepository jobRepository;

    @Override
    @Transactional
    public void create(JobCreateCommand command) {
        Job job = Job.create(
                RepositoryId.of(command.repositoryId()),
                CommitHash.of(command.commitHash()),
                BranchName.of(command.branchName()),
                UserId.of(command.triggeredBy()));

        jobRepository.save(job);
        log.info("Job created. repositoryId={}, commitHash={}, branchName={}",
                command.repositoryId(), command.commitHash(), command.branchName());
    }
}
```

## JobDispatchService 스니펫

```java
package io.jgitkins.server.execution.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDispatchService implements JobDispatchUseCase {

    private final JobDispatchQueryPort jobDispatchQueryPort;
    private final JobRepository jobRepository;
    private final RunnerRepository runnerRepository;
    private final CloneUrlBuilder cloneUrlBuilder;

    @Override
    @Transactional
    public Optional<JobDispatchResult> dispatch(DispatchJobCommand command) {
        Optional<RunnerDispatchContext> runnerContext = resolveRunnerContext(command.runnerToken());
        if (runnerContext.isEmpty()) {
            return Optional.empty();
        }

        Optional<DispatchableJob> dispatchableJob = jobDispatchQueryPort.findNextDispatchableJob(runnerContext.get());
        if (dispatchableJob.isEmpty()) {
            return Optional.empty();
        }

        return assignRunner(runnerContext.get(), dispatchableJob.get());
    }

    private Optional<RunnerDispatchContext> resolveRunnerContext(String runnerToken) {
        if (runnerToken == null || runnerToken.isBlank()) {
            log.warn("Runner token is missing");
            return Optional.empty();
        }

        return runnerRepository.findByToken(runnerToken)
                .map(this::toDispatchContext);
    }

    private Optional<JobDispatchResult> assignRunner(RunnerDispatchContext runnerContext, DispatchableJob dispatchableJob) {
        Job job = dispatchableJob.job();
        JobHistory previousHistory = job.getLatestHistory();
        RunnerId runnerId = RunnerId.of(String.valueOf(runnerContext.runnerId()));

        job.publish(runnerId);

        Optional<Long> historyId = jobRepository.appendHistoryIfCurrent(job, previousHistory);
        if (historyId.isEmpty()) {
            log.debug("Job {} was already processed by another dispatcher", job.getId().getValue());
            return Optional.empty();
        }

        return Optional.of(buildDispatchResult(runnerContext, dispatchableJob, job, historyId.get()));
    }
}
```

## JobResultReportService 스니펫

```java
package io.jgitkins.server.execution.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultReportService implements JobResultReportUseCase {

    private final JobRepository jobRepository;
    private final RunnerRepository runnerRepository;

    @Override
    @Transactional
    public void reportJobResult(JobResultReportCommand command) {
        Runner runner = runnerRepository.findByToken(command.runnerToken())
                .orElseThrow(() -> new IllegalArgumentException("Runner not found for token"));

        Job job = jobRepository.findById(command.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found for id " + command.jobId()));

        JobHistory previousHistory = job.getLatestHistory();
        RunnerId runnerId = RunnerId.of(String.valueOf(runner.getId()));

        if (command.status() == JobResultStatus.SUCCESS) {
            job.completeSuccess(runnerId);
        } else {
            job.completeFailure(runnerId);
        }

        Optional<Long> persistedId = jobRepository.appendHistoryIfCurrent(job, previousHistory);
        if (persistedId.isEmpty()) {
            throw new IllegalStateException("Failed to persist job result history for job " + command.jobId());
        }
    }
}
```

## 구현 순서

1. contract command/result/internal 패키지를 만든다.
2. dispatch/result 관련 DTO를 execution contract로 이동한다.
3. `JobService`, `JobDispatchService`, `JobResultReportService`를 execution service로 이동한다.
4. `JobPersistencePort.saveHistory(...)` 호출을 `JobRepository.appendHistoryIfCurrent(...)`로 바꾼다.
5. `JobPersistencePort.findNextDispatchableJob(...)` 호출을 `JobDispatchQueryPort`로 바꾼다.
6. 테스트 import와 mock 타입을 수정한다.

## 테스트 기준

- runner token missing이면 dispatch 결과 empty.
- runner not found이면 dispatch 결과 empty.
- dispatch 대상 job이 없으면 empty.
- dispatch 성공 시 `appendHistoryIfCurrent(...)`가 호출되고 result가 생성된다.
- stale dispatch면 empty.
- result report success/failure는 각각 history append를 호출한다.
- result report stale append는 예외를 유지한다.

## 완료 기준

- dispatch query와 aggregate repository가 분리됐다.
- `saveHistory` 이름이 사라지고 `appendHistoryIfCurrent`가 사용된다.
- `JobDispatchServiceTest`, result report 테스트가 통과한다.
- `./gradlew :server:test`가 통과한다.
