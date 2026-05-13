# 06. Execution Infrastructure Adapters 상세 계획

## 목적

Execution Context의 persistence/git/config adapter와 mapper를 `execution.infrastructure`로 이동한다. MBG generated mapper/model은 공용 infrastructure persistence에 유지해 이동 폭을 줄인다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/JobPersistenceAdapter.java
server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/RunnerPersistenceAdapter.java
server/src/main/java/io/jgitkins/server/infrastructure/adapter/git/PipelineConfigGitAdapter.java
server/src/main/java/io/jgitkins/server/infrastructure/adapter/config/RunnerRuntimeConfigAdapter.java

server/src/main/java/io/jgitkins/server/infrastructure/mapper/JobDomainMapper.java
server/src/main/java/io/jgitkins/server/infrastructure/mapper/RunnerDomainMapper.java
server/src/main/java/io/jgitkins/server/infrastructure/mapper/RunnerAssignmentDomainMapper.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/JobRepositoryAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/JobDispatchQueryAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/RunnerPersistenceAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/git/PipelineConfigGitAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/git/PipelineFileLookupAdapter.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/config/RunnerRuntimeConfigAdapter.java

server/src/main/java/io/jgitkins/server/execution/infrastructure/mapper/JobDomainMapper.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/mapper/RunnerDomainMapper.java
server/src/main/java/io/jgitkins/server/execution/infrastructure/mapper/RunnerAssignmentDomainMapper.java
```

유지:

```text
server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/*
server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/*
```

## JobRepositoryAdapter 스니펫

```java
package io.jgitkins.server.execution.infrastructure.adapter.persistence;

@Component
@RequiredArgsConstructor
public class JobRepositoryAdapter implements JobRepository {

    private final JobEntityMbgMapper jobEntityMbgMapper;
    private final JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;
    private final JobDomainMapper jobDomainMapper;

    @Override
    @Transactional
    public void save(Job job) {
        JobEntity entity = jobDomainMapper.toEntity(job);
        jobEntityMbgMapper.insertSelective(entity);

        Long generatedId = entity.getId();
        for (JobHistory history : job.getHistories()) {
            jobHistoryEntityMbgMapper.insertSelective(jobDomainMapper.toHistoryEntity(history, generatedId));
        }
    }

    @Override
    public Optional<Job> findById(Long jobId) {
        JobEntity entity = jobEntityMbgMapper.selectByPrimaryKey(jobId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(jobDomainMapper.toDomain(entity, loadHistories(jobId)));
    }

    @Override
    @Transactional
    public Optional<Long> appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory) {
        Long jobIdLong = Long.parseLong(job.getId().getValue());

        Optional<JobHistoryEntity> latestPersistedHistory = findLatestHistory(jobIdLong);
        if (latestPersistedHistory.isEmpty() || !isSameHistory(latestPersistedHistory.get(), expectedPreviousHistory)) {
            return Optional.empty();
        }

        JobHistoryEntity entity = jobDomainMapper.toHistoryEntity(job.getLatestHistory(), jobIdLong);
        jobHistoryEntityMbgMapper.insertSelective(entity);
        return Optional.of(entity.getId());
    }

    private Optional<JobHistoryEntity> findLatestHistory(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT DESC, ID DESC");

        return jobHistoryEntityMbgMapper.selectByCondition(condition).stream().findFirst();
    }

    private boolean isSameHistory(JobHistoryEntity latestPersisted, JobHistory expectedPreviousHistory) {
        return String.valueOf(latestPersisted.getId()).equals(expectedPreviousHistory.getId().getValue())
                && latestPersisted.getStatus().equals(expectedPreviousHistory.getStatus().name())
                && latestPersisted.getCreatedAt().equals(expectedPreviousHistory.getCreatedAt());
    }
}
```

## JobDispatchQueryAdapter 스니펫

```java
package io.jgitkins.server.execution.infrastructure.adapter.persistence;

@Component
@RequiredArgsConstructor
public class JobDispatchQueryAdapter implements JobDispatchQueryPort {

    private final JobDispatchQueryMapper jobDispatchQueryMapper;
    private final JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;
    private final JobDomainMapper jobDomainMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context) {
        return Optional.ofNullable(jobDispatchQueryMapper.selectNextDispatchableJob(
                        context.dispatchScope().name(),
                        context.scopeTargetId()))
                .flatMap(this::toDispatchableJob);
    }

    private Optional<DispatchableJob> toDispatchableJob(DispatchableJobRow row) {
        List<JobHistory> histories = loadHistories(row.jobId());
        return Optional.of(jobDomainMapper.toDispatchableJob(row, histories));
    }

    private List<JobHistory> loadHistories(Long jobId) {
        JobHistoryEntityCondition condition = new JobHistoryEntityCondition();
        condition.createCriteria().andJobIdEqualTo(jobId);
        condition.setOrderByClause("CREATED_AT ASC, ID ASC");

        return jobHistoryEntityMbgMapper.selectByCondition(condition).stream()
                .map(jobDomainMapper::toHistoryDomain)
                .toList();
    }
}
```

`JobDomainMapper`에는 dispatch row 전용 변환 메서드를 명시적으로 추가한다. 기존 `JobPersistenceAdapter.toDispatchableJob(...)` 내부 로직을 mapper 쪽으로 이동시키는 작업이다.

```java
package io.jgitkins.server.execution.infrastructure.mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobDomainMapper {

    default DispatchableJob toDispatchableJob(DispatchableJobRow row, List<JobHistory> histories) {
        Long organizeId = "ORGANIZATION".equals(row.repositoryOwnerType()) ? row.repositoryOwnerId() : null;
        return new DispatchableJob(
                toDomain(row, histories),
                organizeId,
                row.repositoryClonePath());
    }
}
```

## RunnerPersistenceAdapter 스니펫

```java
package io.jgitkins.server.execution.infrastructure.adapter.persistence;

@Component
@RequiredArgsConstructor
public class RunnerPersistenceAdapter implements RunnerRepository {

    private final RunnerEntityMbgMapper runnerEntityMbgMapper;
    private final RunnerAssignmentEntityMbgMapper runnerAssignmentEntityMbgMapper;
    private final RunnerDomainMapper runnerDomainMapper;
    private final RunnerAssignmentDomainMapper runnerAssignmentDomainMapper;

    @Override
    @Transactional
    public Runner save(Runner runner) {
        RunnerEntity entity = runnerDomainMapper.toEntity(runner);
        if (runner.getId() == null) {
            runnerEntityMbgMapper.insertSelective(entity);
            Runner restored = restoreRunner(entity);
            runnerAssignmentEntityMbgMapper.insertSelective(runnerAssignmentDomainMapper.toEntity(restored));
            return restored;
        }

        runnerEntityMbgMapper.updateByPrimaryKeySelective(entity);
        runnerAssignmentEntityMbgMapper.updateByPrimaryKeySelective(runnerAssignmentDomainMapper.toEntity(runner));
        return restoreRunner(runnerEntityMbgMapper.selectByPrimaryKey(runner.getId()));
    }
}
```

## PipelineConfigGitAdapter 스니펫

```java
package io.jgitkins.server.execution.infrastructure.adapter.git;

@Component
@RequiredArgsConstructor
public class PipelineConfigGitAdapter implements PipelineConfigPort {

    private final RepositoryResolver repositoryResolver;

    @Override
    public PipelineConfig read(String namespace, String repoName, String commitHash) {
        // 기존 구현 유지. 패키지와 import만 execution 기준으로 이동.
    }
}
```

## PipelineFileLookupAdapter 스니펫

```java
package io.jgitkins.server.execution.infrastructure.adapter.git;

@Component
@RequiredArgsConstructor
public class PipelineFileLookupAdapter implements PipelineFileLookupPort {

    private final FileGitPort fileGitPort;

    @Override
    public boolean exists(String namespace, String repoName, String commitHash, String path) {
        return fileGitPort.exists(namespace, repoName, commitHash, path);
    }
}
```

## 구현 순서

1. mapper를 execution infrastructure mapper로 이동한다.
2. aggregate 저장/로드 책임은 `JobRepositoryAdapter implements JobRepository`로 이동한다.
3. dispatch 대상 조회 책임은 `JobDispatchQueryAdapter implements JobDispatchQueryPort`로 분리한다.
4. `saveHistory` 메서드를 `appendHistoryIfCurrent`로 rename한다.
5. `RunnerPersistenceAdapter`를 execution infrastructure로 이동하고 `RunnerRepository`를 구현하게 한다.
6. `PipelineConfigGitAdapter`를 execution infrastructure git adapter로 이동한다.
7. `PipelineFileLookupAdapter`를 추가하고 현재 코드 기준 기존 `FileGitPort.exists(...)`에 위임한다.
8. `RunnerRuntimeConfigAdapter`를 execution infrastructure config adapter로 이동한다.
9. generated mapper/model import는 기존 infrastructure persistence를 유지한다.
10. 이후 Repository Context에 file-read seam이 정리되면 `PipelineFileLookupAdapter` 내부 delegate만 교체한다.

## 테스트 기준

- `JobRepositoryAdapter`는 Job과 초기 history를 저장한다.
- `appendHistoryIfCurrent`는 expected previous history의 id/status/createdAt이 현재 최신 persisted history와 같을 때만 insert한다.
- `appendHistoryIfCurrent`는 최신 persisted history가 이미 바뀌었으면 empty를 반환한다.
- `JobDispatchQueryAdapter.findNextDispatchableJob`은 기존 SQL mapper 결과를 `DispatchableJob`으로 변환한다.
- `PipelineFileLookupAdapter`는 기존 `FileGitPort.exists(...)`에 위임하고 execution policy가 `FileGitPort`나 repository infrastructure를 직접 참조하지 않게 한다.
- `RunnerPersistenceAdapter`는 runner와 assignment를 함께 저장한다.
- runner restore 시 assignment가 없으면 `GLOBAL`로 fallback한다.

## 완료 기준

- execution persistence adapter가 execution repository/port를 구현한다.
- MBG generated code는 이동하지 않았다.
- infrastructure mapper import가 execution domain 타입을 사용한다.
- `./gradlew :server:test`가 통과한다.
