# 01. Execution Domain Model 상세 계획

## 목적

Execution Context의 핵심 도메인 모델을 top-level `domain` 패키지에서 `execution.domain` 하위로 이동한다.

대상 모델은 `Job`, `JobHistory`, `Runner`다. `JobHistory`는 독립 aggregate가 아니라 `Job` 내부 Entity로 유지한다. `RunnerAssignmentEntity`는 persistence-level relation으로 남기고 domain model로 승격하지 않는다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/domain/aggregate/Job.java
server/src/main/java/io/jgitkins/server/domain/model/JobHistory.java
server/src/main/java/io/jgitkins/server/domain/aggregate/Runner.java

server/src/main/java/io/jgitkins/server/domain/model/vo/JobId.java
server/src/main/java/io/jgitkins/server/domain/model/vo/JobHistoryId.java
server/src/main/java/io/jgitkins/server/domain/model/vo/JobStatus.java
server/src/main/java/io/jgitkins/server/domain/model/vo/RunnerId.java
server/src/main/java/io/jgitkins/server/domain/model/vo/RunnerStatus.java
server/src/main/java/io/jgitkins/server/domain/model/vo/RunnerScopeType.java

server/src/main/java/io/jgitkins/server/domain/event/JobQueuedEvent.java
server/src/main/java/io/jgitkins/server/domain/event/RunnerActivatedEvent.java

server/src/main/java/io/jgitkins/server/domain/exception/RunnerAlreadyActiveException.java
server/src/main/java/io/jgitkins/server/domain/exception/RunnerTokenMismatchException.java
server/src/main/java/io/jgitkins/server/domain/exception/RunnerTokenMissingException.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/domain/aggregate/Job.java
server/src/main/java/io/jgitkins/server/execution/domain/entity/JobHistory.java
server/src/main/java/io/jgitkins/server/execution/domain/aggregate/Runner.java

server/src/main/java/io/jgitkins/server/execution/domain/vo/JobId.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/JobHistoryId.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/JobStatus.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/RunnerId.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/RunnerStatus.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/RunnerScopeType.java

server/src/main/java/io/jgitkins/server/execution/domain/event/JobQueuedEvent.java
server/src/main/java/io/jgitkins/server/execution/domain/event/RunnerActivatedEvent.java

server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerAlreadyActiveException.java
server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerTokenMismatchException.java
server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerTokenMissingException.java
```

## 결정 사항

- `Job`은 Execution Context Aggregate Root다.
- `JobHistory`는 `execution.domain.entity`로 이동하지만 `Job` 내부 Entity다.
- 하나의 `Job` 조회 시 전체 `JobHistory`를 같이 로딩한다. Job은 실행 인스턴스이므로 이력 포함 aggregate load가 허용된다.
- `Runner`는 Execution Context Aggregate Root다.
- `Runner` aggregate root id는 현재처럼 `Long`을 유지한다. `RunnerId` VO는 `JobHistory.runnerId`, domain event, execution assignment 표현에 사용한다.
- `RunnerAssignmentEntity`는 domain model이 아니라 persistence storage detail이다.
- 상태 모델의 기능 확장은 하지 않는다. 현재 런타임 동작을 유지한다.
- 사용하지 않는 상태나 주석 불일치는 구현 단계에서 코드 주석/명명 정리로만 다룬다.

## 코드 스니펫

### Job

```java
package io.jgitkins.server.execution.domain.aggregate;

import io.jgitkins.server.domain.aggregate.AbstractAggregateRoot;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.event.JobQueuedEvent;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.domain.model.vo.UserId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Job extends AbstractAggregateRoot<JobId> {

    private final JobId id;
    private final RepositoryId repositoryId;
    private final CommitHash commitHash;
    private final BranchName branchName;
    private final UserId triggeredBy;
    private final LocalDateTime createdAt;
    private final List<JobHistory> histories;

    public static Job create(
            RepositoryId repositoryId,
            CommitHash commitHash,
            BranchName branchName,
            UserId triggeredBy
    ) {
        JobId jobId = JobId.generate();
        LocalDateTime now = LocalDateTime.now();
        List<JobHistory> histories = new ArrayList<>();
        histories.add(JobHistory.createPending(jobId, now));

        return new Job(jobId, repositoryId, commitHash, branchName, triggeredBy, now, histories);
    }

    public void publish(RunnerId runnerId) {
        validateCanDispatch();
        histories.add(JobHistory.createInProgress(id, histories.size() + 1, runnerId, LocalDateTime.now()));
        registerEvent(JobQueuedEvent.from(this, runnerId));
    }

    public void completeSuccess(RunnerId runnerId) {
        validateCanComplete();
        histories.add(JobHistory.createSuccess(id, histories.size() + 1, runnerId, LocalDateTime.now()));
    }

    public void completeFailure(RunnerId runnerId) {
        validateCanComplete();
        histories.add(JobHistory.createFailed(id, histories.size() + 1, runnerId, LocalDateTime.now()));
    }

    public JobStatus getCurrentStatus() {
        return getLatestHistory().getStatus();
    }

    public JobHistory getLatestHistory() {
        if (histories.isEmpty()) {
            throw new IllegalStateException("Job must have at least one history");
        }
        return histories.get(histories.size() - 1);
    }

    public List<JobHistory> getHistories() {
        return Collections.unmodifiableList(histories);
    }

    private void validateCanDispatch() {
        if (getCurrentStatus() != JobStatus.PENDING) {
            throw new IllegalStateException("Cannot dispatch job in status: " + getCurrentStatus());
        }
    }

    private void validateCanComplete() {
        if (getCurrentStatus() != JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete job in status: " + getCurrentStatus());
        }
    }

    public static Job reconstruct(
            JobId id,
            RepositoryId repositoryId,
            CommitHash commitHash,
            BranchName branchName,
            UserId triggeredBy,
            LocalDateTime createdAt,
            List<JobHistory> histories
    ) {
        return new Job(id, repositoryId, commitHash, branchName, triggeredBy, createdAt, new ArrayList<>(histories));
    }
}
```

### JobHistory

```java
package io.jgitkins.server.execution.domain.entity;

import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.domain.model.vo.SequenceNumber;
import io.jgitkins.server.domain.model.vo.SystemUser;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobHistory {

    private final JobHistoryId id;
    private final JobId jobId;
    private final SequenceNumber seqNo;
    private final RunnerId runnerId;
    private final JobStatus status;
    private final SystemUser createdBy;
    private final LocalDateTime createdAt;

    public static JobHistory createPending(JobId jobId, LocalDateTime createdAt) {
        return new JobHistory(JobHistoryId.generate(), jobId, SequenceNumber.first(), null,
                JobStatus.PENDING, SystemUser.SYSTEM, createdAt);
    }

    public static JobHistory createInProgress(JobId jobId, int seqNo, RunnerId runnerId, LocalDateTime createdAt) {
        return new JobHistory(JobHistoryId.generate(), jobId, SequenceNumber.of(seqNo), runnerId,
                JobStatus.IN_PROGRESS, SystemUser.SYSTEM, createdAt);
    }
}
```

### Runner

```java
package io.jgitkins.server.execution.domain.aggregate;

import io.jgitkins.server.domain.aggregate.AbstractAggregateRoot;
import io.jgitkins.server.execution.domain.event.RunnerActivatedEvent;
import io.jgitkins.server.execution.domain.exception.RunnerAlreadyActiveException;
import io.jgitkins.server.execution.domain.exception.RunnerTokenMismatchException;
import io.jgitkins.server.execution.domain.exception.RunnerTokenMissingException;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Runner extends AbstractAggregateRoot<Long> {

    private final Long id;
    private final String token;
    private final String description;
    private final RunnerStatus status;
    private final RunnerScopeType scopeType;
    private final Long scopeTargetId;
    private final String ipAddress;
    private final LocalDateTime lastHeartbeatAt;
    private final LocalDateTime createdAt;

    public static Runner create(String description, RunnerScopeType scopeType, Long scopeTargetId) {
        validateRegistration(description, scopeType, scopeTargetId);
        LocalDateTime now = LocalDateTime.now();
        return new Runner(null, generateToken(), description.trim(), RunnerStatus.OFFLINE,
                scopeType, scopeTargetId, null, now, now);
    }

    public Runner activate(String providedToken, String remoteIp) {
        validateToken(providedToken);
        validateActivationState();
        Runner activated = new Runner(id, token, description, RunnerStatus.ONLINE,
                scopeType, scopeTargetId, normalizeIp(remoteIp), LocalDateTime.now(), createdAt);
        activated.copyDomainEventsFrom(this);
        activated.registerEvent(RunnerActivatedEvent.from(activated));
        return activated;
    }

    private static String generateToken() {
        return "RNR-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 24).toUpperCase();
    }
}
```

## 구현 순서

1. `execution.domain` 패키지를 만든다.
2. `Job`, `JobHistory`, `Runner`와 execution 전용 VO/event/exception을 이동한다.
3. import를 이동된 패키지로 정리한다.
4. `Job` 주석/메서드명은 현재 동작 기준으로만 정리한다.
5. `RunnerAssignmentEntity`는 이동하지 않는다.
6. domain 테스트 import를 수정한다.

## 테스트 기준

- `Job.create(...)`는 첫 history를 가진다.
- `Job.publish(...)`는 pending 상태에서만 가능하다.
- `Job.completeSuccess/Failure(...)`는 in-progress 상태에서만 가능하다.
- `Runner.create(...)`는 description/scope validation을 유지한다.
- `Runner.activate(...)`는 token 검증과 offline 상태 검증을 유지한다.

## 완료 기준

- execution domain package로 모델이 이동했다.
- `Runner` aggregate root id 타입은 `Long`으로 유지됐다.
- `RunnerAssignmentEntity`는 infrastructure persistence model에 남아 있다.
- domain behavior 테스트가 통과한다.
- `./gradlew :server:test`가 통과한다.
