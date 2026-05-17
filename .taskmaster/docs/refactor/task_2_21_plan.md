# Task 2.21 리팩토링 계획

## 제목

- **리팩토링 계획**: P3 Change & Review Context 패키지 승격 및 app-server 내부 경계 재편
- **후속 상세 계획 단위**: `task_2_21/` 하위에 Domain, Application, Persistence, Presentation/Test 단위로 분리 작성

## 배경

`Change & Review Context`는 `PullRequest`를 중심으로 source/target snapshot, target drift, mergeability, merge 수행 경계를 다루는 문서다.

현재 관련 코드는 `app-server` 안에 흩어져 있다.

- `app-server/src/main/java/io/jgitkins/server/domain/pr/...`
- `app-server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/...`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/pr/...`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/MergeController.java`

이 작업의 목적은 기능 추가가 아니라 bounded context 경계를 app-server 내부에서 먼저 선명하게 만드는 것이다.
즉, `Change & Review Context`를 `io.jgitkins.server.change.review` 패키지 아래로 먼저 모으고, 그 다음 단계에서 Gradle module extraction을 검토한다.

분리 축은 둘이다.

- persisted state: `PullRequest`가 직접 소유해야 하는 상태
- computed result: 조회 시점에 다시 계산해야 하는 상태

## 목표

- `Change & Review Context`를 `app-server` 내부의 `io.jgitkins.server.change.review` 패키지로 승격한다.
- `PullRequest` aggregate가 소유할 값과 조회 시점 계산값의 경계를 다시 고정한다.
- `PullRequestService`에 섞여 있는 생성/조회 책임을 분리한다.
- `MergeService`의 Git merge 책임과 `PullRequest` 상태 전이를 분리한다.
- PR 관련 presentation API를 `change.review` 패키지로 올리고, repository-scoped route는 유지한다.
- MBG 영속성 계층은 PR aggregate의 persisted state만 다루도록 맞춘다.
- 이 단계에서는 Gradle module extraction을 하지 않고, package-local bounded context를 먼저 완성한다.

## 핵심 결정

### 1. Context 이름과 Java package root는 분리해서 본다

- 문서와 도메인 모델의 이름: `Change & Review Context`
- Java package root: `io.jgitkins.server.change.review`
- 파일 경로 관례: `change/review`

이 작업에서는 `changes_review` 같은 비표준 표기를 쓰지 않는다.

### 2. app-server는 composition root로 남긴다

`app-server`는 현재 단계에서 조립자 역할을 유지한다.

- repository, execution, shared는 각자 별도 context로 남는다.
- change/review만 먼저 `app-server` 내부에서 패키지 승격을 진행한다.
- module extraction은 후속 단계다.

### 3. PR 관련 presentation은 change.review 소유로 본다

- `PullRequestController`를 새로 추가한다.
- `MergeController`는 repository-scoped route를 유지하되, ownership은 change.review presentation으로 옮긴다.
- controller는 use case 호출만 하고 계산 로직은 갖지 않는다.

## TO-BE 패키지 방향

```text
app-server/src/main/java/io/jgitkins/server/change/review/
  domain/
    aggregate/
    model/
    model/changegraph/
    repository/
    vo/
  application/
    service/
    port/in/
    port/out/
    support/
    mapper/
    exception/
  infrastructure/
    adapter/persistence/
    mapper/
  presentation/
    api/rest/
    dto/
    mapper/
```

이 구조의 핵심은 다음이다.

- `domain`은 persisted root와 값 모델을 가진다.
- `application`은 create/query/merge seam을 가진다.
- `infrastructure`는 MBG entity와 adapter를 가진다.
- `presentation`은 PR API와 merge API를 가진다.

## 구현 단위

### 1. Domain boundary audit

`PullRequest` aggregate의 상태 전이를 다시 검토한다.

정리 방향:

- `create(...)`는 source/target snapshot과 status만 초기화한다.
- `markTargetDrifted(...)`는 조회 시 계산값으로 유지하되, command path의 필수 상태처럼 보이지 않게 한다.
- `close()`, `reopen()`, `markMerged()`의 전이 조건은 aggregate 내부에서만 판단한다.
- `TargetDrift`는 read-side observation으로 취급한다.
- `MergeabilityAssessment`는 computed result다.

### 2. Application seam 분리

`PullRequestService`는 현재 생성/조회 책임을 함께 가진다. 이 계획에서는 역할을 더 얇게 나눈다.

권장 구조:

```text
app-server/src/main/java/io/jgitkins/server/change/review/application/service/
  PullRequestCreateService
  PullRequestQueryService
  MergeService
```

분리 기준:

- Create service는 repository lookup + branch head snapshot capture + save만 수행한다.
- Query service는 stored snapshot + current Git state + mergeability 계산만 수행한다.
- Merge service는 Git merge 수행과 PR 상태 전이를 분리해서 연결한다.

### 3. Persistence and mapper alignment

`PullRequestDomainMapper`와 `PullRequestPersistenceAdapter`는 persisted state만 안정적으로 다루도록 맞춘다.

정리 포인트:

- entity에 저장되는 필드와 aggregate root의 진짜 상태를 일치시킨다.
- `TargetDrift`가 optional snapshot인지, read-side-only 값인지 결정하고 mapper 정책을 고정한다.
- `MergeabilityAssessment`는 entity에 저장하지 않는다.
- save 시점의 `updatedAt` 정책을 aggregate 상태 변경과 일치시킨다.

### 4. Presentation/API 정리

API는 PR 생성과 상세 조회를 추가하고, merge 관련 API는 repository-scoped route를 유지한다.

정리 우선순위:

- PR 생성 API는 persisted snapshot만 반환한다.
- PR 상세 조회 API는 current head, target drift, mergeability를 함께 반환한다.
- merge 관련 endpoint는 repository path를 유지하되 ownership은 change.review presentation으로 옮긴다.

### 5. 상세 계획 문서 분리 기준

다음 4개 문서로 상세 구현안을 분리한다.

1. `task_2_21/01_pull_request_domain_boundary.md`
2. `task_2_21/02_pull_request_application_services.md`
3. `task_2_21/03_pull_request_persistence_and_mappers.md`
4. `task_2_21/04_pull_request_presentation_and_tests.md`

## 상세 파일 범위

### 우선 검토 대상

- `app-server/src/main/java/io/jgitkins/server/change/review/domain/aggregate/PullRequest.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/domain/model/BranchHeadSnapshot.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/domain/model/TargetDrift.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/domain/model/PullRequestStatus.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/domain/repository/PullRequestRepository.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/PullRequestCreateService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/PullRequestQueryService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/MergeService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/support/PullRequestMergeabilityResolver.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/mapper/PullRequestDetailMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/mapper/PullRequestResultMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/mapper/PullRequestDomainMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/adapter/persistence/PullRequestPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/presentation/api/rest/MergeController.java`

### 테스트 보강 대상

- `app-server/src/test/java/io/jgitkins/server/change/review/application/service/PullRequestCreateServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/change/review/application/service/PullRequestQueryServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/change/review/application/service/MergeServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/change/review/application/support/PullRequestMergeabilityResolverTest.java`
- `app-server/src/test/java/io/jgitkins/server/change/review/infrastructure/mapper/PullRequestDomainMapperTest.java`
- `app-server/src/test/java/io/jgitkins/server/change/review/domain/aggregate/PullRequestTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`

## 구현 순서

1. `PullRequest` aggregate의 persisted/computed 경계를 재확인한다.
2. `PullRequestCreateService`와 `PullRequestQueryService`로 application seam을 나눈다.
3. `MergeService`와 PR 상태 전이의 연결 지점을 분리한다.
4. `PullRequestDomainMapper`와 `PullRequestPersistenceAdapter`를 persisted state 기준으로 정리한다.
5. presentation과 테스트를 정리하고, architecture guardrail을 추가한다.
6. package-local bounded context가 안정화되면 module extraction 후보를 별도 계획으로 분리한다.

## 테스트 전략

- 생성 테스트: PR 생성 시 source/target snapshot이 저장되고 mergeability가 저장되지 않는지 검증한다.
- 조회 테스트: 상세 조회에서 current head, target drift, mergeability가 read-side 계산으로만 조립되는지 검증한다.
- merge 테스트: Git merge 결과와 PR 상태 전이의 연결이 분리되어 있는지 검증한다.
- mapper 테스트: entity와 aggregate 간 변환에서 persisted state만 왕복하는지 검증한다.
- 구조 테스트: `change.review` 관련 package가 repository/shared seam을 무단으로 끌어오지 않는지 확인한다.

## 완료 기준

- `Change & Review Context`가 `io.jgitkins.server.change.review` 패키지로 승격된다.
- PR 생성/상세 조회/merge 경계가 문서와 코드 후보 기준으로 정리된다.
- MBG persistence와 application seam의 우선순위가 명시된다.
- `PullRequestService`와 `MergeService`가 각각 어떤 책임만 남길지 결정된다.
- 추후 구현이 문서만 보고도 가능할 정도로 파일 범위와 테스트 전략이 구체적이다.

## NOT in scope

- 이 단계에서 DB schema 변경은 하지 않는다.
- 이 단계에서 Gradle module extraction은 하지 않는다.
- 이 단계에서 repository context package와 shared context를 다시 이동하지 않는다.
- 이 단계에서 execution context를 change.review 안으로 끌어오지 않는다.
