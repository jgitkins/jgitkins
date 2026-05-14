# Task 2.20 리팩토링 계획

## 제목

- **리팩토링 계획**: P2 Execution Context 기준 리팩토링 계획 수립
- **후속 상세 계획 단위**: `task_2_20/` 하위에 Job, Runner, Dispatch, Push orchestration, Pipeline policy 단위로 분리 작성

## 배경

Repository Context는 Task 2.19, 2.24~2.31을 통해 context-local package, port, support, adapter 경계가 정리됐다.

다음 리팩토링 축은 `Execution Context`다. 현재 execution 관련 코드는 top-level `application`, `domain`, `infrastructure`, `presentation` 아래에 분산되어 있다.

대표 흐름:

- Push 이후 Job 생성: `PushEventHandleService`, `ExecutionRequestService`, `JobService`
- Pipeline policy 소비: `EventPolicyResolver`, `PushJobCreationPolicy`
- Job dispatch: `JobDispatchService`, `JobPersistencePort`, `JobPersistenceAdapter`
- Job 결과 보고: `JobResultReportService`
- Runner 등록/조회/삭제/활성화: `RunnerManagementService`, `RunnerReadService`, `RunnerPersistencePort`, `RunnerPersistenceAdapter`

문서 기준 출처:

- `docs/modeling/contexts/execution/execution-context.md`

## 목표

- `Job Aggregate`, `JobHistory Entity`, `Runner Aggregate`를 Execution Context 소유 모델로 정렬한다.
- Job 생성, dispatch, 결과 보고, runner lifecycle을 execution application seam으로 묶는다.
- `PushEventHandleService`, `ExecutionRequestService`, `PushJobCreationPolicy`의 책임 경계를 다시 정의한다.
- pipeline policy는 aggregate가 아니라 application-level policy 소비 흐름으로 유지한다.
- Repository Context 정리와 같은 방식으로 큰 이동을 한 번에 하지 않고, 작은 구현 단위별 상세 계획 문서를 먼저 만든다.

## 핵심 판단

### 1. Job은 Execution Context Aggregate Root다

현재 코드:

- `server/src/main/java/io/jgitkins/server/domain/aggregate/Job.java`
- `server/src/main/java/io/jgitkins/server/domain/model/JobHistory.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/JobId.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/JobStatus.java`

목표 위치 후보:

```text
server/src/main/java/io/jgitkins/server/execution/domain/aggregate/Job.java
server/src/main/java/io/jgitkins/server/execution/domain/entity/JobHistory.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/JobId.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/JobStatus.java
server/src/main/java/io/jgitkins/server/execution/domain/repository/JobRepository.java
```

결정 사항:

- Execution Context도 도메인 모델링이 되어 있으므로 aggregate 로딩/저장/상태 이력 append는 `execution.domain.repository.JobRepository`가 담당한다.
- 기존 `JobPersistencePort`를 그대로 이름만 바꾸지 않는다. aggregate 저장/로드 계약은 domain repository로, dispatch 대상 조회는 application query port로 분리한다.
- `JobRepository`는 `JobHistory`를 포함한 `Job` aggregate 전체를 로딩한다. Job은 실행 인스턴스 성격이 강하므로 단일 Job 조회 시 실행 이력이 함께 조회되어도 괜찮다.

### 2. JobHistory는 Job 내부 Entity다

문서 기준으로 `JobHistory`는 독립 aggregate가 아니다. `Job` 상태는 최신 history로 해석한다.

결정 사항:

- Task 2.20 본 계획에서는 상태 모델 변경을 구현하지 않는다.
- 현재 런타임 동작과 API 의미를 유지한다.
- `JobHistory` append 계약은 repository처럼 보이는 범용 `saveHistory(...)`보다 optimistic locking 의도가 드러나는 `appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory)`로 설계한다.

### 3. Runner는 Execution Context Aggregate Root다

현재 코드:

- `server/src/main/java/io/jgitkins/server/domain/aggregate/Runner.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/RunnerStatus.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/RunnerScopeType.java`
- `server/src/main/java/io/jgitkins/server/domain/event/RunnerActivatedEvent.java`

목표 위치 후보:

```text
server/src/main/java/io/jgitkins/server/execution/domain/aggregate/Runner.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/RunnerStatus.java
server/src/main/java/io/jgitkins/server/execution/domain/vo/RunnerScopeType.java
server/src/main/java/io/jgitkins/server/execution/domain/event/RunnerActivatedEvent.java
server/src/main/java/io/jgitkins/server/execution/domain/repository/RunnerRepository.java
```

`RunnerAssignmentEntity`는 persistence-level relation으로 유지한다. domain model로 승격하지 않는다.

### 4. Pipeline policy는 application-level policy다

`PushJobCreationPolicy`는 aggregate 규칙이 아니다.

이유:

- pipeline config와 pipeline file은 Git 외부 입력이다.
- policy는 “이 push에서 job을 만들 것인가”를 application flow에서 결정한다.
- `Job` aggregate는 이미 만들어질 실행 요청의 상태 전이와 history를 책임진다.

목표:

```text
shared.application.policy.PushJobCreationPolicy
    -> execution.application.policy.PushJobCreationPolicy
```

Port 소유권 결정:

- `PipelineConfigPort`는 execution application port로 둔다. pipeline config는 job 생성 여부를 결정하는 execution policy 입력이다.
- raw file lookup은 execution이 정의한 `PipelineFileLookupPort`를 통해 접근한다. 1차 구현은 기존 `FileGitPort.exists(...)`에 위임하고, 이후 repository/read seam이 정리되면 adapter 내부 delegate만 교체한다.
- execution policy가 repository git adapter 구현체나 repository infrastructure를 직접 알면 안 된다.

### 5. PushEventHandleService는 cross-context orchestration이다

현재 `PushEventHandleService`는 다음을 한 메서드에서 수행한다.

```text
PushEventCommand
    |
    +-- BranchChangeRecorder.record(command)
    +-- JobCreationValidator.validate(command)
    +-- EventPolicyResolver.resolvePushPlan(command)
    +-- ExecutionRequestService.requestPushExecution(command, jobPlan)
```

이 흐름은 Repository Context의 branch change 기록과 Execution Context의 job creation이 섞여 있다.

결정 사항:

- `PushEventHandleService`는 1차로 top-level application orchestration에 유지한다.
- `ExecutionRequestService`와 job creation 쪽만 execution context로 먼저 이동한다.
- 이후 `PushEventHandleService`를 `PushEventOrchestrationService` 같은 cross-context coordinator로 재명명할지 별도 상세 계획에서 결정한다.

## 목표 패키지 방향

1차 목표:

```text
server/src/main/java/io/jgitkins/server/execution/domain/aggregate
server/src/main/java/io/jgitkins/server/execution/domain/entity
server/src/main/java/io/jgitkins/server/execution/domain/vo
server/src/main/java/io/jgitkins/server/execution/domain/event
server/src/main/java/io/jgitkins/server/execution/domain/exception

server/src/main/java/io/jgitkins/server/execution/application/service
server/src/main/java/io/jgitkins/server/execution/application/port/in
server/src/main/java/io/jgitkins/server/execution/application/port/out
server/src/main/java/io/jgitkins/server/execution/application/contract/command
server/src/main/java/io/jgitkins/server/execution/application/contract/result
server/src/main/java/io/jgitkins/server/execution/application/contract/internal
server/src/main/java/io/jgitkins/server/execution/application/policy
server/src/main/java/io/jgitkins/server/execution/application/support
server/src/main/java/io/jgitkins/server/execution/application/mapper
server/src/main/java/io/jgitkins/server/execution/application/exception

server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/git
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/config
server/src/main/java/io/jgitkins/server/execution/infrastructure/mapper

server/src/main/java/io/jgitkins/server/execution/presentation/api/rest
server/src/main/java/io/jgitkins/server/execution/presentation/api/grpc
server/src/main/java/io/jgitkins/server/execution/presentation/dto
server/src/main/java/io/jgitkins/server/execution/presentation/mapper
```

이 중 presentation 이동은 초기 단계에 넣지 않는다. 먼저 domain/application/infrastructure seam을 안정화한 뒤 마지막 단계로 둔다.

## 범위

계획 대상:

- `Job`, `JobHistory`, `JobId`, `JobHistoryId`, `JobStatus`
- `Runner`, `RunnerStatus`, `RunnerScopeType`, `RunnerId`
- `JobService`, `JobDispatchService`, `JobResultReportService`
- `RunnerManagementService`, `RunnerReadService`
- `ExecutionRequestService`, `RunnerRuntimeConfigProvider`
- `JobPersistencePort`, `RunnerPersistencePort`, `PipelineConfigPort`
- `JobPersistenceAdapter`, `RunnerPersistenceAdapter`, `PipelineConfigGitAdapter`, `RunnerRuntimeConfigAdapter`
- `JobDomainMapper`, `RunnerDomainMapper`, `RunnerAssignmentDomainMapper`
- `JobDispatchGrpcController`, `RunnerController`
- `PushEventHandleService`, `PushJobCreationPolicy`, `EventPolicyResolver`
- 관련 tests

범위 밖:

- Runner 모듈 내부 실행 로직 재설계
- gRPC 공통 예외 처리 표준화
- Change & Review Context의 PR readiness와 Job 결과 결합
- Identity & Access Context의 runner token/credential 체계 재설계
- DB schema 변경

## 세부 계획 문서 분해

Task 2.20은 구현 그 자체보다 P2 리팩토링 계획 수립이다. 실제 구현은 다음 상세 계획 문서를 먼저 만든 뒤 하나씩 진행한다.

```text
.taskmaster/docs/refactor/task_2_20/
├── 01_execution_domain_model.md
├── 02_execution_application_ports.md
├── 03_job_dispatch_and_result_report.md
├── 04_runner_lifecycle.md
├── 05_push_orchestration_and_pipeline_policy.md
├── 06_execution_infrastructure_adapters.md
└── 07_execution_presentation_adapters.md
```

각 문서는 다음을 포함한다.

- AS-IS 파일 목록
- TO-BE 패키지
- 단계별 이동 순서
- 코드 스니펫
- 테스트 추가/수정 기준
- 완료 기준

추가 검수 결과, dispatch payload 정합성에 대한 후속 문서를 별도로 둔다.

```text
.taskmaster/docs/refactor/task_2_20/
├── 09_execution_dispatch_payload_integrity.md
└── 10_execution_dispatch_controller_and_service_split.md
```

이 문서들은 기존 1~7 문서의 패키지 이관이 끝난 뒤 진행하는 cleanup 계획이다.

## 단계별 계획

### 1단계: Execution domain model 이동 계획

대상:

- `Job`
- `JobHistory`
- `Runner`
- execution 전용 VO/event/exception

원칙:

- `RepositoryId`, `BranchName`, `CommitHash`, `UserId` 중 cross-context 성격이 강한 타입은 먼저 유지한다.
- `RunnerAssignmentEntity`는 domain model로 만들지 않는다.
- `JobHistory`는 `Job` 내부 entity로 둔다.

코드 스니펫:

```java
package io.jgitkins.server.execution.domain.aggregate;

public class Job extends AbstractAggregateRoot<JobId> {
    private final JobId id;
    private final RepositoryId repositoryId;
    private final CommitHash commitHash;
    private final BranchName branchName;
    private final UserId triggeredBy;
    private final List<JobHistory> histories;

    public void publish(RunnerId runnerId) {
        validateCanDispatch();
        histories.add(JobHistory.createInProgress(id, histories.size() + 1, runnerId, LocalDateTime.now()));
        registerEvent(JobQueuedEvent.from(this, runnerId));
    }
}
```

상세 계획과 구현에서는 상태 전이 기능을 확장하지 않고 현재 동작을 보존한다.

### 2단계: application inbound port 재배치

대상:

- `JobCreateUseCase`
- `JobDispatchUseCase`
- `JobResultReportUseCase`
- `RunnerRegisterUseCase`
- `RunnerLoadUseCase`
- `RunnerDeleteUseCase`
- `RunnerActivateUseCase`

목표:

```text
application.port.in
    -> execution.application.port.in
```

코드 스니펫:

```java
package io.jgitkins.server.execution.application.port.in;

public interface JobDispatchUseCase {
    Optional<JobDispatchResult> dispatch(DispatchJobCommand command);
}
```

Repository Context와 같은 방식으로 controller import만 바꾸고 URL/API 계약은 유지한다.

### 3단계: outbound port 분리

현재 `JobPersistencePort`는 aggregate 저장과 dispatch read query를 같이 가진다.

AS-IS:

```java
public interface JobPersistencePort {
    void save(Job job);
    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
    Optional<Long> saveHistory(Job job, JobHistory previousHistory);
    Optional<Job> findById(Long jobId);
}
```

TO-BE 후보:

```java
package io.jgitkins.server.execution.domain.repository;

public interface JobRepository {
    void save(Job job);
    Optional<Job> findById(Long jobId);
    Optional<Long> appendHistoryIfCurrent(Job job, JobHistory expectedPreviousHistory);
}
```

```java
package io.jgitkins.server.execution.application.port.out;

public interface JobDispatchQueryPort {
    Optional<DispatchableJob> findNextDispatchableJob(RunnerDispatchContext context);
}
```

`appendHistoryIfCurrent(...)`는 단순 저장 메서드가 아니다. dispatch/result report에서 읽은 이전 latest history가 여전히 DB의 latest history일 때만 새 history를 append하는 optimistic locking 계약이다. stale 상태면 `Optional.empty()`를 반환한다.

Runner도 같은 기준을 적용한다.

```java
package io.jgitkins.server.execution.domain.repository;

public interface RunnerRepository {
    Runner save(Runner runner);
    void deleteById(Long runnerId);
    Optional<Runner> findById(Long runnerId);
    Optional<Runner> findByToken(String token);
    List<Runner> findAll();
}
```

`RunnerPersistencePort.update(...)`는 현재 호출부가 없거나 `save(...)`와 중복될 수 있으므로 상세 계획에서 제거 여부를 확인한다.

### 4단계: Job dispatch/result report service 정리

대상:

- `JobDispatchService`
- `JobResultReportService`
- `DispatchableJob`
- `RunnerDispatchContext`
- `JobDispatchResult`
- `JobResultReportCommand`

원칙:

- dispatch query는 application read seam이다.
- `job.publish(runnerId)`와 `job.completeSuccess/Failure(runnerId)`는 domain behavior로 유지한다.
- optimistic locking은 persistence adapter 책임으로 유지한다.

코드 스니펫:

```java
@Service
@RequiredArgsConstructor
public class JobDispatchService implements JobDispatchUseCase {

    private final JobDispatchQueryPort jobDispatchQueryPort;
    private final JobRepository jobRepository;
    private final RunnerRepository runnerRepository;
    private final CloneUrlBuilder cloneUrlBuilder;

    @Transactional
    public Optional<JobDispatchResult> dispatch(DispatchJobCommand command) {
        return resolveRunnerContext(command.runnerToken())
                .flatMap(jobDispatchQueryPort::findNextDispatchableJob)
                .flatMap(dispatchableJob -> assignRunner(command.runnerToken(), dispatchableJob));
    }
}
```

위 코드는 방향 예시다. 실제 구현에서는 현재 `RunnerDispatchContext`를 재사용해 diff를 작게 유지한다.

### 5단계: Runner lifecycle 정리

대상:

- `RunnerManagementService`
- `RunnerReadService`
- `RunnerRuntimeConfigProvider`
- `RunnerApplicationMapper`
- `RunnerRegisterCommand`
- `RunnerActivateResult`, `RunnerRegistrationResult`, `RunnerDetailResult`

원칙:

- register/delete/activate는 management service.
- read는 load/read service.
- runtime config 조립은 execution application support로 이동한다.
- runner activation의 token 검증과 상태 전이는 `Runner` aggregate behavior로 유지한다.

코드 스니펫:

```java
package io.jgitkins.server.execution.application.service;

@Service
@RequiredArgsConstructor
public class RunnerManagementService implements RunnerRegisterUseCase, RunnerDeleteUseCase, RunnerActivateUseCase {

    private final RunnerRepository runnerRepository;
    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRuntimeConfigProvider runtimeConfigProvider;

    @Transactional
    public RunnerActivateResult activate(String token, String remoteIp) {
        Runner runner = runnerRepository.findByToken(token)
                .orElseThrow(() -> new RunnerNotFoundException("Runner not found"));
        Runner activated = runner.activate(token, remoteIp);
        runnerRepository.save(activated);
        return new RunnerActivateResult(runtimeConfigProvider.createConfig(), RunnerExecutionConfig.defaultConfig());
    }
}
```

### 6단계: Push orchestration과 pipeline policy 경계 정리

대상:

- `PushEventHandleService`
- `BranchChangeRecorder`
- `JobCreationValidator`
- `EventPolicyResolver`
- `PushJobCreationPolicy`
- `ExecutionRequestService`

판단:

- `PushEventHandleService`는 Repository branch 기록과 Execution request 생성을 함께 조율하므로 cross-context orchestration이다.
- `ExecutionRequestService`는 execution application support로 이동한다.
- `PushJobCreationPolicy`는 execution application policy로 이동한다.
- `EventPolicyResolver`는 shared에 둘 이유가 약해지면 execution application support/policy로 이동한다.

AS-IS:

```java
branchChangeRecorder.record(command);
JobCreationDecision decision = jobCreationValidator.validate(command);
JobPlan jobPlan = eventPolicyResolver.resolvePushPlan(command);
executionRequestService.requestPushExecution(command, jobPlan);
```

TO-BE 후보:

```java
package io.jgitkins.server.execution.application.support;

@Component
@RequiredArgsConstructor
public class ExecutionRequestService {

    private final JobCreateUseCase jobCreateUseCase;

    public void requestPushExecution(PushEventCommand command, JobPlan plan) {
        jobCreateUseCase.create(JobCreateCommand.from(command, plan));
    }
}
```

`PushEventCommand`는 Git hook input에 가까워서 바로 execution 전용으로 옮기지 않는다. 상세 계획에서 repository/change-review와의 소유권을 재검토한다.

### 7단계: infrastructure adapter 이동

대상:

- `JobPersistenceAdapter`
- `RunnerPersistenceAdapter`
- `PipelineConfigGitAdapter`
- `RunnerRuntimeConfigAdapter`
- `JobDomainMapper`
- `RunnerDomainMapper`
- `RunnerAssignmentDomainMapper`

목표:

```text
infrastructure.adapter.persistence.JobPersistenceAdapter
    -> execution.infrastructure.adapter.persistence.JobPersistenceAdapter

infrastructure.adapter.persistence.RunnerPersistenceAdapter
    -> execution.infrastructure.adapter.persistence.RunnerPersistenceAdapter

infrastructure.mapper.JobDomainMapper
    -> execution.infrastructure.mapper.JobDomainMapper
```

주의:

- MBG generated mapper/model은 `infrastructure.persistence.*`에 유지한다.
- adapter만 execution infrastructure로 이동해 generated code 이동 폭을 줄인다.

### 8단계: presentation adapter 이동

대상:

- `RunnerController`
- `JobDispatchGrpcController`
- `RunnerCreateRequest`
- `RunnerActivateRequest`
- `RunnerResponse`
- `RunnerRequestMapper`
- `RunnerResponseMapper`

목표:

```text
server/presentation/api/rest/RunnerController.java
    -> server/execution/presentation/api/rest/RunnerController.java

server/presentation/api/grpc/JobDispatchGrpcController.java
    -> server/execution/presentation/api/grpc/JobDispatchGrpcController.java
```

공통 유지:

- `ApiResponse`
- `GlobalExceptionHandler`
- 공통 error mapper
- generated gRPC proto classes

이 단계는 domain/application/infrastructure 이동 후 마지막에 수행한다.

## 점진 이관 순서

1. `task_2_20/01_execution_domain_model.md` 작성
2. `task_2_20/02_execution_application_ports.md` 작성
3. `task_2_20/03_job_dispatch_and_result_report.md` 작성
4. `task_2_20/04_runner_lifecycle.md` 작성
5. `task_2_20/05_push_orchestration_and_pipeline_policy.md` 작성
6. `task_2_20/06_execution_infrastructure_adapters.md` 작성
7. `task_2_20/07_execution_presentation_adapters.md` 작성
8. 각 상세 계획을 하나씩 구현하고 `./gradlew :server:test`로 회귀 검증

## 테스트 전략

기존 테스트 축:

- `JobServiceTest`
- `JobDispatchServiceTest`
- `JobResultReportServiceTest`
- `RunnerManagementServiceTest`
- `RunnerReadServiceTest`
- `PushEventHandleServiceTest`
- `PushEventHandleServiceIntegrationTest`
- `RunnerControllerTest`
- `JobDispatchGrpcController` 관련 테스트 후보
- `JobRunnerPortTest`는 runner 모듈이므로 직접 이동 대상 아님

추가/보강 기준:

- Job 생성 시 첫 history가 `PENDING`인지 검증한다.
- dispatch 성공 시 이전 latest history 기준 optimistic locking이 유지되는지 검증한다.
- dispatch stale history면 empty result가 유지되는지 검증한다.
- `appendHistoryIfCurrent(...)`는 expected previous history가 최신일 때만 새 history를 append하는지 검증한다.
- result report는 `SUCCESS/FAILED` 이력을 추가하고, `IN_PROGRESS`가 아닌 상태에서는 실패하는지 검증한다.
- runner activation은 `OFFLINE -> ONLINE`만 허용하고 token mismatch/missing domain exception을 유지한다.
- pipeline policy error는 job 생성 실패가 아니라 skip decision으로 남는지 검증한다.
- pipeline config 조회는 execution-owned `PipelineConfigPort`로 수행하고, raw file existence lookup은 execution-owned `PipelineFileLookupPort`를 통해 수행하는지 architecture regression으로 확인한다.
- package convention test에 execution context service/domain/support 위치를 단계별로 추가한다.

## Architecture regression 기준

상세 구현 단계마다 다음 규칙을 추가한다.

```java
@Test
void executionContextServices_resideInExecutionServicePackage() {
    List<Class<?>> serviceClasses = List.of(
            JobService.class,
            JobDispatchService.class,
            JobResultReportService.class,
            RunnerManagementService.class,
            RunnerReadService.class);

    serviceClasses.forEach(serviceClass ->
            assertEquals("io.jgitkins.server.execution.application.service", serviceClass.getPackageName()));
}
```

단, migration 중에는 한 번에 켜지 않는다. 해당 파일 이동 PR에서만 규칙을 확장한다.

## 완료 기준

- Task 2.20 상태가 `done`이다.
- `.taskmaster/docs/refactor/task_2_20_plan.md`가 존재한다.
- Execution Context 리팩토링을 7개 이하의 작은 상세 계획 단위로 나눴다.
- 각 상세 계획 단위가 구현 가능한 파일 범위와 테스트 기준을 가진다.
- Repository Context와 마찬가지로 context-local package 방향이 명시됐다.
- pipeline policy가 aggregate가 아니라 application-level policy라는 판단이 문서에 반영됐다.
- `PushEventHandleService`를 무리하게 execution 내부로 끌어들이지 않고 cross-context orchestration 후보로 남겼다.

## NOT in scope

- 지금 단계에서 소스 파일 이동 구현은 하지 않는다.
- 지금 단계에서 DB schema 변경은 하지 않는다.
- 지금 단계에서 Runner module 실행 로직은 수정하지 않는다.
- 지금 단계에서 gRPC 예외 표준화는 하지 않는다.
- 지금 단계에서 PR readiness와 Job 결과 결합은 하지 않는다.
