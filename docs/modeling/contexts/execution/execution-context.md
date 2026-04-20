## Execution Context

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념과 유비쿼터스 언어](#핵심-개념과-유비쿼터스-언어)
- [Job](#job)
- [Job History](#job-history)
- [Runner](#runner)
- [Runner Scope](#runner-scope)
- [Aggregate / Entity / Value Object 경계](#aggregate--entity--value-object-경계)
- [Aggregate Root: Job](#aggregate-root-job)
- [Entity: Job History](#entity-job-history)
- [Aggregate Root: Runner](#aggregate-root-runner)
- [Persistence-Level Relation: Runner Assignment](#persistence-level-relation-runner-assignment)
- [주요 Value Objects](#주요-value-objects)
- [상태](#상태)
- [불변식](#불변식)
- [주요 시나리오](#주요-시나리오)
- [1. Push 이후 Job 생성](#1-push-이후-job-생성)
- [2. Job Dispatch](#2-job-dispatch)
- [3. Job 결과 보고](#3-job-결과-보고)
- [4. Runner 등록과 활성화](#4-runner-등록과-활성화)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [미확정 쟁점](#미확정-쟁점)

### 문제 정의

`Execution Context`는 실행 요청 생성, runner dispatch, 실행 결과 보고를 다룬다. 현재 중심 모델은 `Job`과 `Runner`다.

이 문서의 목적은 다음 질문에 답하는 것이다.

- `Job`이 직접 소유하는 상태는 무엇인가
- `JobHistory`는 `Job` 내부 Entity인가
- `Runner`의 scope와 activation은 어디서 관리되는가
- dispatch와 결과 보고는 어느 경계에서 처리되는가

### 책임 범위

이 context의 책임은 다음과 같다.

- push 이후 실행 요청 생성
- `Job` 저장
- dispatch 가능한 `Job` 조회와 runner 할당
- 실행 결과 보고
- `Runner` 등록, 조회, 삭제, 활성화

직접 소유하지 않는 책임은 다음과 같다.

- 저장소 생성과 branch lifecycle 관리
- Pull Request 상태 전이와 mergeability 계산
- pipeline 파일 자체의 저장
- runner 프로세스 내부 실행 로직

### 핵심 개념과 유비쿼터스 언어

#### Job

특정 repository, branch, commit에 대한 실행 요청이다. 이 context의 Aggregate Root다.

#### Job History

Job 상태 변화 기록이다. `Job` 내부 Entity다.

#### Runner

Job을 실제로 실행하는 작업자다. 등록, 활성화, 상태, scope를 가진다.

#### Runner Scope

Runner가 처리할 수 있는 범위다. 현재는 `GLOBAL`, `ORGANIZE`, `REPOSITORY`가 있다.

### Aggregate / Entity / Value Object 경계

#### Aggregate Root: Job

`Job`은 이 context의 root다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/aggregate/Job.java`
- 유스케이스 근거: `JobCreateUseCase`, `JobDispatchUseCase`, `JobResultReportUseCase`
- 서비스 근거: `JobService`, `JobDispatchService`, `JobResultReportService`

`Job`이 직접 소유하거나 결정하는 값은 다음과 같다.

- `JobId`
- `RepositoryId`
- `CommitHash`
- `BranchName`
- `UserId triggeredBy`
- `createdAt`
- `List<JobHistory>`

#### Entity: Job History

`JobHistory`는 `Job` 내부 Entity다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/JobHistory.java`
- 식별: `JobHistoryId`
- 상태 값: `JobStatus`

`Job`은 현재 상태를 별도 필드로 가지지 않고, 최신 `JobHistory`로 상태를 해석한다.

#### Aggregate Root: Runner

`Runner`는 이 context의 root다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/aggregate/Runner.java`
- 유스케이스 근거: `RunnerRegisterUseCase`, `RunnerLoadUseCase`, `RunnerDeleteUseCase`, `RunnerActivateUseCase`
- 서비스 근거: `RunnerManagementService`, `RunnerReadService`

`Runner`가 직접 소유하거나 결정하는 값은 다음과 같다.

- `id`
- `token`
- `description`
- `RunnerStatus`
- `RunnerScopeType`
- `scopeTargetId`
- `ipAddress`
- `lastHeartbeatAt`
- `createdAt`

#### Persistence-Level Relation: Runner Assignment

현재 구현에는 `RUNNER_ASSIGNMENT` 테이블과 `RunnerAssignmentEntity`가 있다. 하지만 도메인 모델에는 별도 `RunnerAssignment` aggregate나 entity가 없다.

현재 문서 기준 해석:

- scope 정보는 `Runner` aggregate가 가진다.
- `RunnerAssignmentEntity`는 이를 저장하기 위한 persistence 구조다.
- 별도 도메인 모델로 승격하지 않는다.

#### 주요 Value Objects

이 context의 주요 Value Object는 다음과 같다.

- `JobId`
- `JobHistoryId`
- `JobStatus`
- `RunnerId`
- `RunnerStatus`
- `RunnerScopeType`
- `RepositoryId`
- `CommitHash`
- `BranchName`

### 상태

`JobStatus`는 다음 값을 가진다.

- `PENDING`
- `IN_QUEUE`
- `IN_PROGRESS`
- `SUCCESS`
- `FAILED`
- `CANCELED`

현재 코드 기준 `Job` 흐름은 다음과 같다.

- 생성 시 첫 `JobHistory`는 `PENDING`
- dispatch 시 `publish()`가 새 이력을 추가
- 결과 보고 시 `SUCCESS` 또는 `FAILED` 이력을 추가

`RunnerStatus`는 다음 값을 가진다.

- `OFFLINE`
- `ONLINE`
- `PAUSED`

### 불변식

현재 기준 불변식은 다음과 같다.

1. `Job`은 최소 하나의 `JobHistory`를 가져야 한다.
2. `Job` 생성 시 첫 `JobHistory`는 `PENDING`이다.
3. `Job`은 현재 상태가 `PENDING`일 때만 dispatch할 수 있다.
4. `Job`은 현재 상태가 `IN_PROGRESS`일 때만 성공 또는 실패 완료가 가능하다.
5. `Runner` 등록 시 `description`과 `scopeType`은 필수다.
6. `RunnerScopeType`이 target id를 요구하면 `scopeTargetId`가 있어야 한다.
7. `Runner`는 `OFFLINE` 상태일 때만 activate할 수 있다.
8. activate 시 token이 일치해야 한다.

### 주요 시나리오

#### 1. Push 이후 Job 생성

현재 흐름은 다음과 같다.

1. `PushEventHandleService`가 push event를 받는다.
2. branch 변경을 기록한다.
3. job 생성 가능 여부를 검증한다.
4. `PushJobCreationPolicy`가 pipeline rule과 pipeline 파일 존재 여부를 확인한다.
5. `ExecutionRequestService`가 `JobCreateCommand`를 만든다.
6. `JobService`가 `Job.create(...)`로 `Job`을 만들고 저장한다.

#### 2. Job Dispatch

현재 흐름은 다음과 같다.

1. runner token으로 `Runner`를 찾는다.
2. runner scope로 dispatch context를 만든다.
3. `JobPersistencePort.findNextDispatchableJob(...)`로 dispatch 가능한 job을 찾는다.
4. `job.publish(runnerId)`를 호출한다.
5. 새 `JobHistory`를 저장한다.
6. `JobDispatchResult`를 반환한다.

#### 3. Job 결과 보고

현재 흐름은 다음과 같다.

1. runner token으로 `Runner`를 찾는다.
2. job id로 `Job`을 찾는다.
3. 현재 `JobHistory`를 읽는다.
4. 성공이면 `completeSuccess`, 실패면 `completeFailure`를 호출한다.
5. 새 `JobHistory`를 저장한다.

#### 4. Runner 등록과 활성화

현재 흐름은 다음과 같다.

1. 등록 시 `Runner.create(...)`로 `Runner`를 만든다.
2. persistence adapter가 `RUNNER`와 `RUNNER_ASSIGNMENT`를 저장한다.
3. 활성화 시 token으로 runner를 찾는다.
4. `runner.activate(token, remoteIp)`를 호출한다.
5. 변경된 runner를 저장한다.

### 외부 시스템과의 경계

외부 경계는 다음과 같다.

- `JobPersistencePort`
  - `Job` 저장, dispatch 대상 조회, `JobHistory` 저장
- `RunnerPersistencePort`
  - `Runner` 저장, 조회, 삭제
- `PipelineConfigPort`
  - pipeline config 조회
- `FileGitPort`
  - pipeline 파일 존재 여부 확인
- `RunnerRuntimeConfigProvider`
  - runner activation 결과용 runtime config 생성
- `CloneUrlBuilder`
  - dispatch 결과의 clone URL 생성

원칙:

- pipeline rule 판단은 `Execution Context`가 사용하는 애플리케이션 정책 해석이다.
- pipeline config와 pipeline 파일 자체는 Git 기반 외부 입력이다.
- `Job` 상태는 별도 current status 컬럼보다 `JobHistory` 누적으로 해석한다.
- runner scope 저장은 현재 도메인 모델보다 persistence 구조에 더 강하게 나타난다.

### 다른 Context와의 연결

- `Repository Context`
  - `Job`은 특정 repository, branch, commit에 대해 생성된다.
- `Change & Review Context`
  - PR readiness는 이후 `Job` 결과와 결합될 수 있다.
- `Identity & Access Context`
  - `triggeredBy`, runner activation, runner 조회 권한과 연결된다.
- `Shared / Cross-Cutting Topics`
  - pipeline policy, dispatch policy, readiness 해석과 연결된다.

### 미확정 쟁점

1. `IN_QUEUE` 상태를 실제로 사용할지
   - `JobStatus`에는 `IN_QUEUE`가 있지만 현재 `Job.publish()`는 `IN_PROGRESS` 이력을 추가한다.
2. `pipelineFilePath`를 `Job`에 저장할지
   - `JobCreateCommand`에는 `pipelineFilePath`가 있지만 현재 `Job` aggregate에는 저장되지 않는다.
3. `RunnerAssignment`를 별도 도메인 모델로 둘지
   - 현재는 persistence 구조로만 존재한다.
