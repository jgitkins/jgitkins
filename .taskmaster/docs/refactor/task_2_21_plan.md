# Task 2.21 리팩토링 계획

## 제목

- **리팩토링 계획**: P3 Change & Review Context 기준 리팩토링 계획 수립
- **후속 상세 계획 단위**: `task_2_21/` 하위에 Domain, Application, Persistence, Presentation/Test 단위로 분리 작성

## 배경

`Change & Review Context`는 `PullRequest`를 중심으로 source/target snapshot, target drift, mergeability, merge 수행 경계를 다루는 문서다.

현재 코드베이스에는 이미 다음 자산이 존재한다.

- `app-server/src/main/java/io/jgitkins/server/domain/pr/aggregate/PullRequest.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/BranchHeadSnapshot.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/TargetDrift.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/repository/PullRequestRepository.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/MergeService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestMergeabilityResolver.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/pr/PullRequestPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/PullRequestDomainMapper.java`

이 작업의 목적은 기능 추가가 아니라 경계 재정렬이다. 특히 아래 두 축을 분리한다.

- persisted state: `PullRequest`가 직접 소유해야 하는 상태
- computed result: 조회 시점에 다시 계산해야 하는 상태

## 목표

- `PullRequest` aggregate가 소유할 값과 조회 시점 계산값의 경계를 다시 고정한다.
- `PullRequestService`에 섞여 있는 생성/조회 책임을 분리한다.
- `MergeService`의 Git merge 책임과 `PullRequest` 상태 전이를 분리한다.
- MBG 영속성 계층은 PR aggregate의 persisted state만 다루도록 맞춘다.
- presentation/API는 PR 생성/상세 조회/merge 흐름에 맞는 최소 계약만 유지한다.

## 핵심 판단

### 1. PullRequest는 persisted root만 소유한다

`PullRequest`가 직접 책임질 값은 다음으로 한정한다.

- `PullRequestId`
- `RepositoryId`
- stored source snapshot
- stored target snapshot
- `PullRequestStatus`
- `createdAt`
- `updatedAt`

조회 시 재계산되는 값은 별도로 둔다.

- current source head
- current target head
- `TargetDrift`
- `MergeabilityAssessment`
- conflict files
- fast-forward 가능 여부
- merge commit 필요 여부

즉, `PullRequest` aggregate는 branch의 현재 상태를 소유하지 않는다. 현재 상태는 Git port를 통해 다시 읽는 값이다.

### 2. TargetDrift는 read-side observation으로 취급한다

현재 코드에는 `TargetDrift`가 aggregate 필드와 DB 컬럼으로도 연결되어 있지만, change-review 문서의 의미는 “조회 시점 관찰 결과”에 가깝다.

이 작업에서는 다음 원칙을 우선한다.

- `TargetDrift`를 command input으로 받지 않는다.
- 상세 조회는 `markTargetDrifted(...)` 같은 관찰 결과를 만들어 반환할 수 있다.
- 영속화가 필요하더라도 `TargetDrift`는 상태 진실(source of truth)이 아니라 관찰 스냅샷으로만 취급한다.

### 3. MergeabilityAssessment는 영속 상태가 아니라 계산 결과다

`MergeabilityAssessment`는 `PullRequest` 내부 엔티티처럼 다루지 않는다.

- `PullRequestMergeabilityResolver`가 Git state를 읽어 계산한다.
- `MergeService`는 mergeability 계산과 실제 merge command를 분리해서 다룬다.
- `lastAssessmentSnapshot`을 영속 모델로 강제하지 않는다.

### 4. merge 수행과 PR 상태 전이는 별도 경계다

현재 `MergeController`는 repository-level merge command만 제공하고, PR id를 받지 않는다.

따라서 이 계획의 기준은 다음과 같다.

- Git merge 수행은 `MergeService`가 계속 책임진다.
- `PullRequest.markMerged()`와 `PullRequestRepository.save(...)` 연결은 PR-aware command가 생기는 시점에 붙인다.
- repository-level merge API를 억지로 PR 상태 전이의 유일한 진실로 만들지 않는다.

## 구현 단위

### 1. Domain boundary audit

`PullRequest` aggregate의 상태 전이를 다시 검토한다.

현재 상태 예시:

```java
PullRequest pullRequest = PullRequest.create(repository.getId(), source, target);
PullRequest observed = pullRequest.markTargetDrifted(currentTarget);
```

정리 방향:

- `create(...)`는 source/target snapshot과 status만 초기화한다.
- `markTargetDrifted(...)`는 조회 시 계산값으로 유지하되, command path의 필수 상태처럼 보이지 않게 한다.
- `close()`, `reopen()`, `markMerged()`의 전이 조건은 aggregate 내부에서만 판단한다.

### 2. Application seam 분리

`PullRequestService`는 현재 생성/조회 책임을 함께 가진다. 이 계획에서는 역할을 더 얇게 나눈다.

권장 구조:

```text
app-server/application/service/
  PullRequestCreateService
  PullRequestQueryService
  PullRequestMergeService (필요 시 PR-aware transition seam)
```

분리 기준:

- Create service는 repository lookup + branch head snapshot capture + save만 수행한다.
- Query service는 stored snapshot + current Git state + mergeability 계산만 수행한다.
- Merge service는 Git merge 수행과 PR 상태 전이를 분리해서 연결한다.

예시 흐름:

```java
public PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId) throws IOException {
    PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
            .orElseThrow(() -> new PullRequestNotFoundException(pullRequestId));
    Repository repository = repositoryRepository.findById(pullRequest.getRepositoryId())
            .orElseThrow(() -> new RepositoryNotFoundException(pullRequest.getRepositoryId().getValue()));

    BranchHeadSnapshot currentSource = mergeabilityResolver.currentSourceHead(repository, pullRequest);
    BranchHeadSnapshot currentTarget = mergeabilityResolver.currentTargetHead(repository, pullRequest);
    PullRequest observed = pullRequest.markTargetDrifted(currentTarget);
    MergeabilityAssessment assessment = mergeabilityResolver.assess(repository, observed);

    return detailMapper.toDetail(observed, currentSource, currentTarget, assessment);
}
```

이 흐름 자체는 유지하되, read-side 조립 책임을 query service로 더 명확히 분리할 수 있는지 검토한다.

### 3. Persistence and mapper alignment

`PullRequestDomainMapper`와 `PullRequestPersistenceAdapter`는 persisted state만 안정적으로 다루도록 맞춘다.

정리 포인트:

- entity에 저장되는 필드와 aggregate root의 진짜 상태를 일치시킨다.
- `TargetDrift`가 optional snapshot인지, read-side-only 값인지 결정하고 mapper 정책을 고정한다.
- `MergeabilityAssessment`는 entity에 저장하지 않는다.
- save 시점의 `updatedAt` 정책을 aggregate 상태 변경과 일치시킨다.

예시:

```java
public PullRequestEntity toEntity(PullRequest pullRequest) {
    PullRequestEntity entity = new PullRequestEntity();
    entity.setRepositoryId(pullRequest.getRepositoryId().getValue());
    entity.setSourceBranch(pullRequest.getSource().branchName().getValue());
    entity.setSourceHead(pullRequest.getSource().commitHash().getValue());
    entity.setTargetBranch(pullRequest.getTarget().branchName().getValue());
    entity.setTargetHead(pullRequest.getTarget().commitHash().getValue());
    entity.setStatus(pullRequest.getStatus().name());
    entity.setCreatedAt(pullRequest.getCreatedAt());
    entity.setUpdatedAt(pullRequest.getUpdatedAt());
    return entity;
}
```

### 4. Presentation/API 정리

API는 PR 생성과 상세 조회를 우선 정리하고, merge 관련 API는 현재 계약을 무리하게 바꾸지 않는다.

정리 우선순위:

- PR 생성 API는 persisted snapshot만 반환한다.
- PR 상세 조회 API는 current head, target drift, mergeability를 함께 반환한다.
- merge 관련 endpoint는 PR 상태 전이가 필요한 경우에만 PR-aware command를 추가한다.

현재 `MergeController`는 repository-level merge를 담당하므로, PR 상태 전이를 붙이려면 별도 command 또는 별도 endpoint가 필요하다.

### 5. 상세 계획 문서 분리 기준

다음 4개 문서로 상세 구현안을 분리한다.

1. `task_2_21/01_pull_request_domain_boundary.md`
2. `task_2_21/02_pull_request_application_services.md`
3. `task_2_21/03_pull_request_persistence_and_mappers.md`
4. `task_2_21/04_pull_request_presentation_and_tests.md`

## 상세 파일 범위

### 우선 검토 대상

- `app-server/src/main/java/io/jgitkins/server/domain/pr/aggregate/PullRequest.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/BranchHeadSnapshot.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/TargetDrift.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/PullRequestStatus.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/MergeService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestMergeabilityResolver.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestDetailMapper.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestResultMapper.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/PullRequestDomainMapper.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/pr/PullRequestPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/MergeController.java`

### 테스트 보강 대상

- `app-server/src/test/java/io/jgitkins/server/application/service/PullRequestServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/service/MergeServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/support/pr/PullRequestMergeabilityResolverTest.java`
- `app-server/src/test/java/io/jgitkins/server/infrastructure/mapper/PullRequestDomainMapperTest.java`
- `app-server/src/test/java/io/jgitkins/server/domain/pr/aggregate/PullRequestTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`

## 구현 순서

1. `PullRequest` aggregate의 persisted/computed 경계를 재확인한다.
2. `PullRequestService`를 create/query 경계로 나눌지 결정한다.
3. `MergeService`와 PR 상태 전이의 연결 지점을 분리한다.
4. `PullRequestDomainMapper`와 `PullRequestPersistenceAdapter`를 persisted state 기준으로 정리한다.
5. presentation과 테스트를 정리하고, architecture guardrail을 추가한다.

## 테스트 전략

- 생성 테스트: PR 생성 시 source/target snapshot이 저장되고 mergeability가 저장되지 않는지 검증한다.
- 조회 테스트: 상세 조회에서 current head, target drift, mergeability가 read-side 계산으로만 조립되는지 검증한다.
- merge 테스트: Git merge 결과와 PR 상태 전이의 연결이 분리되어 있는지 검증한다.
- mapper 테스트: entity와 aggregate 간 변환에서 persisted state만 왕복하는지 검증한다.
- 구조 테스트: `change-review` 관련 package가 repository/shared seam을 무단으로 끌어오지 않는지 확인한다.

## 완료 기준

- `task 2.21`의 계획 범위가 `PullRequest` persisted state와 computed result의 경계를 명확히 설명한다.
- PR 생성/상세 조회/merge 경계가 문서와 코드 후보 기준으로 정리된다.
- MBG persistence와 application seam의 우선순위가 명시된다.
- `PullRequestService`와 `MergeService`가 각각 어떤 책임만 남길지 결정된다.
- 추후 구현이 문서만 보고도 가능할 정도로 파일 범위와 테스트 전략이 구체적이다.

## NOT in scope

- 이 단계에서 DB schema 변경은 하지 않는다.
- 이 단계에서 PR용 신규 API contract를 추가하지 않는다.
- 이 단계에서 repository context package와 shared context를 다시 이동하지 않는다.
- 이 단계에서 mergeability를 persisted mandatory state로 강제하지 않는다.
