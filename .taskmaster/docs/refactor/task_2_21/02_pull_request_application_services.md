# 02. Pull Request Application Services

## 목적

`PullRequestService`에 섞여 있는 생성/조회 책임을 분리하고, `MergeService`와의 경계도 명확히 한다.

이 문서는 `app-server/src/main/java/io/jgitkins/server/change/review/application/**` 아래로 application seam을 옮기는 계획이다.

## 핵심 결론

- PR 생성과 PR 상세 조회는 서로 다른 service로 분리한다.
- `MergeService`는 Git merge 계산/실행만 책임진다.
- PR 상태 전이는 이번 단계에서 merge API에 직접 묶지 않는다.
- `PullRequestController`와 `MergeController`는 change.review presentation이 호출한다.

## 대상 파일

- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/PullRequestCreateService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/PullRequestQueryService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/service/MergeService.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/support/PullRequestMergeabilityResolver.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/mapper/PullRequestDetailMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/mapper/PullRequestResultMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/port/in/CreatePullRequestUseCase.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/application/port/in/GetPullRequestDetailUseCase.java`

## TO-BE 서비스 구조

```text
app-server/src/main/java/io/jgitkins/server/change/review/application/
  service/
    PullRequestCreateService.java
    PullRequestQueryService.java
    MergeService.java
  port/in/
    CreatePullRequestUseCase.java
    GetPullRequestDetailUseCase.java
    MergeabilityCheckUseCase.java
    MergeabilityEvaluationUseCase.java
    MergeUseCase.java
  support/
    PullRequestMergeabilityResolver.java
  mapper/
    PullRequestDetailMapper.java
    PullRequestResultMapper.java
```

### 1. PullRequestCreateService

책임:

- repository lookup
- source/target branch head capture
- `PullRequest.create(...)`
- save
- result mapping

핵심은 create flow가 repository context와 Git port를 읽고, change.review domain에 PR snapshot을 저장하는 것이다.

### 2. PullRequestQueryService

책임:

- PR aggregate load
- repository load
- current source/target head load
- target drift observation
- mergeability evaluation
- detail mapping

핵심은 조회 시점 계산값을 모두 여기서 조립하고, aggregate는 persisted snapshot만 돌려주게 만드는 것이다.

### 3. MergeService 경계

`MergeService`는 그대로 유지하되, PR 상태 전이를 끌어오지 않는다.

```java
@Service
@RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeabilityEvaluationUseCase, MergeUseCase {
    private final MergeGitPort mergeGitPort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;
}
```

이번 단계에서는 다음을 하지 않는다.

- `performMerge(...)`에 PR id를 추가하지 않는다.
- merge 성공 후 `PullRequest.markMerged()`를 자동 호출하지 않는다.
- repository-level merge API를 PR state transition API로 바꾸지 않는다.

## 정리 포인트

- `PullRequestService`는 더 이상 create/query를 동시에 가지지 않도록 정리한다.
- create/query service의 의존성 집합이 분리되도록 한다.
- `PullRequestMergeabilityResolver`는 query service 전용 collaborator로 유지한다.
- merge 수행은 repository-level route를 유지하되, 서비스 ownership은 change.review로 옮긴다.

## 테스트 기준

- `PullRequestCreateServiceTest`
  - repository lookup 실패
  - source/target branch missing
  - successful save/result mapping
- `PullRequestQueryServiceTest`
  - PR not found
  - repository not found
  - current head observation and mergeability mapping
- `MergeServiceTest`
  - 현재 책임 유지 확인
  - mergeability preview/evaluation/perform merge 회귀 검증

## 완료 기준

- create/query 책임이 서로 다른 service로 분리된다.
- `PullRequestService`를 제거하거나 단일 facade로 남기지 않는 방향이 명확하다.
- `MergeService`가 PR 상태 전이와 섞이지 않는다.
