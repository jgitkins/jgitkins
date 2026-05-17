# 01. Change & Review Domain Boundary

## 목적

`Change & Review Context`의 첫 번째 결정은 `PullRequest`가 무엇을 직접 소유하고, 무엇을 조회 시점 계산값으로 둘지 고정하는 것이다.

이 문서는 `PullRequest` aggregate를 `app-server/src/main/java/io/jgitkins/server/change/review/domain/**` 아래로 옮기기 위한 boundary 규칙을 정리한다.

## 핵심 결론

- `PullRequest`는 persisted root다.
- `BranchHeadSnapshot`은 persisted snapshot이다.
- `TargetDrift`는 조회 시 관찰 결과이지만, 현재 구조에서는 optional snapshot으로 유지한다.
- `MergeabilityAssessment`는 persisted root의 필수가 아니다.
- `lastAssessmentSnapshot`은 캐시성 snapshot으로만 취급하고, source of truth로 만들지 않는다.

## 현재 코드 기준

대상 파일:

- `app-server/src/main/java/io/jgitkins/server/domain/pr/aggregate/PullRequest.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/BranchHeadSnapshot.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/TargetDrift.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/PullRequestStatus.java`
- `app-server/src/main/java/io/jgitkins/server/domain/pr/model/vo/PullRequestId.java`

## TO-BE 패키지

```text
app-server/src/main/java/io/jgitkins/server/change/review/domain/
  aggregate/
    PullRequest.java
  model/
    BranchHeadSnapshot.java
    TargetDrift.java
    PullRequestStatus.java
  model/changegraph/
    MergeabilityAssessment.java
    MergeabilityStatus.java
    MergeTopologySummary.java
  repository/
    PullRequestRepository.java
  vo/
    PullRequestId.java
```

이 구조의 핵심은 `PullRequest`와 `MergeabilityAssessment`의 의미를 같은 context 안에 두되, persisted root와 computed result를 분리하는 것이다.

## 구현 방향

### 1. persisted state만 aggregate root 의미로 유지한다

`PullRequest`는 다음 상태만 명확히 소유해야 한다.

- PR identity
- repository identity
- source snapshot
- target snapshot
- PR status
- createdAt / updatedAt

조회 시 다시 계산되는 값은 aggregate의 핵심 진실로 보지 않는다.

### 2. TargetDrift는 관찰 결과로만 다룬다

`markTargetDrifted(...)`는 read-side observation helper로 남긴다.

```java
public PullRequest markTargetDrifted(BranchHeadSnapshot currentTarget) {
    requireOpen("mark target drifted");
    if (currentTarget == null) {
        throw new IllegalArgumentException("Current target snapshot must not be null");
    }
    if (!target.hasSameBranch(currentTarget)) {
        throw new IllegalArgumentException("Current target branch must match pull request target branch");
    }
    if (target.commitHash().equals(currentTarget.commitHash())) {
        return copy(source, currentTarget, status, lastAssessmentSnapshot, TargetDrift.none());
    }
    TargetDrift drift = TargetDrift.detected(target.commitHash(), currentTarget.commitHash());
    return copy(source, currentTarget, status, lastAssessmentSnapshot, drift);
}
```

이 메서드는 다음 원칙을 따른다.

- command input이 아니다.
- 상태 전이 메서드처럼 보이지 않게 한다.
- 조회 결과를 조립하기 위한 helper로 사용한다.

### 3. MergeabilityAssessment는 optional snapshot이다

`recordAssessmentSnapshot(...)`는 현재 구조상 존재하더라도, persisted mandatory state로 확대하지 않는다.

```java
public PullRequest recordAssessmentSnapshot(MergeabilityAssessment assessment) {
    requireOpen("record mergeability assessment");
    return copy(source, target, status, assessment, targetDrift);
}
```

정리 기준은 다음과 같다.

- 상세 조회 결과를 저장하는 정책이 필요할 때만 optional snapshot으로 활용한다.
- mergeability 자체를 aggregate source of truth로 취급하지 않는다.
- DB entity에 `MergeabilityAssessment` 전체를 강제 저장하지 않는다.

### 4. status 전이는 aggregate 내부에서만 판단한다

`OPEN`, `CLOSED`, `MERGED` 규칙은 aggregate 내부에서만 검증한다.

```java
public PullRequest close() {
    requireOpen("close");
    return copy(source, target, PullRequestStatus.CLOSED, lastAssessmentSnapshot, targetDrift);
}

public PullRequest reopen() {
    if (status != PullRequestStatus.CLOSED) {
        throw new IllegalStateException("Only closed pull requests can be reopened");
    }
    return copy(source, target, PullRequestStatus.OPEN, lastAssessmentSnapshot, targetDrift);
}
```

이 단계에서는 merge 이후 자동 전이를 API에 끼워 넣지 않는다. 그 연결은 별도 PR-aware seam이 생길 때 붙인다.

## 테스트 기준

- `PullRequestTest`에서 source/target 동일 branch 금지, open 상태 제한, target drift 계산을 검증한다.
- `PullRequestTest`에서 `TargetDrift.none()`과 detected 상태의 optional snapshot semantics를 검증한다.
- `PullRequestTest`에서 `recordAssessmentSnapshot(...)`이 source of truth를 바꾸지 않는다는 점을 검증한다.

## 완료 기준

- `PullRequest`가 branch current state를 소유하지 않는다는 점이 명시된다.
- `TargetDrift`와 `MergeabilityAssessment`의 의미가 computed result로 정리된다.
- aggregate 내부 전이와 read-side observation의 경계가 문서와 코드 둘 다에서 일치한다.
