# Change Graph Context

## 목적
- 저장소의 변경 관계와 병합 의미를 해석하는 context다.
- 이 context는 Git 내부 기계어를 제품 언어로 번역한다.
- 사용자는 이 context를 통해 “지금 어디에 있고, 어디로 가려는지, 왜 막혔는지”를 이해한다.

## 핵심 질문
- source와 target의 현재 관계는 무엇인가?
- 지금 병합 가능한가?
- fast-forward가 가능한가?
- merge commit이 필요한가?
- 충돌이 발생하는가?
- target이 움직여서 PR이 stale해졌는가?

## 유비쿼터스 언어
- `Pull Request Route`
  - `source -> target` 관계를 표현하는 병합 경로.
- `Branch Head Snapshot`
  - 특정 시점의 branch head와 commit identity를 표현하는 스냅샷.
- `Mergeability Assessment`
  - 병합 가능 여부와 충돌, merge 방식 가능성을 요약한 결과.
- `Target Drift`
  - PR이 열려 있는 동안 target branch head가 이동한 상태.
- `Merge Topology Summary`
  - fast-forward 가능 여부, merge commit 필요 여부를 설명하는 요약.

## Subdomain Classification
- Type: Core Domain
- Why:
  - `jgitkins`의 제품 차별점은 병합과 변경 흐름을 설명 가능한 언어로 바꾸는 데 있다.
  - 따라서 이 context는 단순 supporting logic이 아니라 제품 핵심 문제를 담당한다.

## 책임
- branch/commit 관계 해석
- source/target mergeability 계산
- conflict detection 결과 해석
- merge 방법 설명
- target drift 감지
- PR merge-side 상태 생성

## 책임 밖
- 어떤 파이프라인을 실행할지 결정하지 않음
- runner를 선택하거나 job을 생성하지 않음
- Jenkinsfile을 파싱하지 않음
- 사용자 승인 권한을 판단하지 않음

## 주요 입력
- repository namespace / repo name
- source branch
- target branch
- branch head commit
- merge preview 요청 또는 PR 이벤트

## 주요 출력
- `MergeabilityAssessment`
- `MergeTopologySummary`
- `TargetDriftDetected`
- `PullRequestRoute`

## Aggregate
### Aggregate Root
- `PullRequestRoute`
  - 하나의 `source -> target` 경로와 그 경로의 현재 병합 상태를 대표한다.

### Entities
- 현재 v1 초안에서는 별도 entity를 강하게 두지 않는다.
- route 내부 상태는 route 자신이 직접 소유하고, 나머지는 값 객체로 둔다.

### Value Objects
- `BranchHeadSnapshot`
- `MergeabilityAssessment`
- `MergeTopologySummary`
- `TargetDrift`

## 핵심 불변식
- source와 target은 비어 있을 수 없다.
- source와 target은 동일 branch일 수 없다.
- route는 하나의 repository 안에서만 유효하다.
- 닫힌 route만 `reopen()` 가능하다.
- 열린 route만 `markMerged()` 가능하다.

## Class Diagram
```mermaid
classDiagram
    class PullRequestRoute {
        <<Aggregate Root>>
        +PullRequestRouteId id
        +RepositoryId repositoryId
        +BranchHeadSnapshot source
        +BranchHeadSnapshot target
        +RouteStatus status
        +MergeabilityAssessment lastAssessment
        +TargetDrift targetDrift
        +create()
        +updateSource()
        +refreshMergeability()
        +markTargetDrifted()
        +close()
        +reopen()
        +markMerged()
    }

    class BranchHeadSnapshot {
        <<Value Object>>
        +String branchName
        +String commitHash
    }

    class MergeabilityAssessment {
        <<Value Object>>
        +MergeabilityStatus status
        +MergeTopologySummary topology
        +List~String~ conflicts
        +String reason
    }

    class MergeTopologySummary {
        <<Value Object>>
        +Boolean fastForwardPossible
        +Boolean mergeCommitRequired
    }

    class TargetDrift {
        <<Value Object>>
        +boolean drifted
        +String previousTargetHead
        +String currentTargetHead
    }

    class MergeabilityService {
        <<Domain Service>>
        +assess(PullRequestRoute)
    }

    PullRequestRoute *-- BranchHeadSnapshot
    PullRequestRoute *-- MergeabilityAssessment
    PullRequestRoute *-- TargetDrift
    MergeabilityAssessment *-- MergeTopologySummary
    MergeabilityService ..> PullRequestRoute
    MergeabilityService ..> MergeabilityAssessment
```

## 병합 상태 초안
| 상태 | 의미 |
|---|---|
| `MERGEABLE` | 지금 바로 병합 가능함 |
| `CONFLICTING` | 병합 시 충돌이 발생함 |
| `NO_COMMON_ANCESTOR` | 공통 조상이 없어 정상적인 병합 경로가 아님 |
| `UNKNOWN` | 아직 계산되지 않았거나 정보를 신뢰할 수 없음 |

## 주요 시나리오
### 1. PR 생성
- source와 target이 정해진다.
- 현재 branch head를 기준으로 mergeability를 계산한다.
- 결과는 readiness 조합의 입력이 된다.

### 2. PR 업데이트
- source branch head가 바뀐다.
- mergeability를 다시 계산한다.
- target이 그 사이에 변했다면 stale 여부도 함께 반영한다.

### 3. 병합 직전 설명
- UI는 source 위치, target 위치, 병합 가능 여부를 이 context 결과로 설명한다.
- “FF 가능”, “merge commit 필요”, “충돌 발생”은 이 context의 언어다.

## Domain Service
- `MergeabilityService`
- `TargetDriftDetector`
- `MergeTopologyAnalyzer`

## 현재 코드 시드
- [MergeService.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/application/service/MergeService.java)
- [MergeController.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/presentation/api/rest/MergeController.java)
- [MergeGitAdapter.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/infrastructure/adapter/git/MergeGitAdapter.java)

## 현재 모델의 약점
- 현재는 `MergeResult`가 기술 결과와 제품 의미를 같이 들고 있다.
- fast-forward 가능 여부와 merge commit 필요 여부는 아직 1급 모델로 분리되지 않았다.
- stale target 개념이 아직 명시적이지 않다.

## 다음 리팩터링 힌트
- `MergeResult`를 당장 없애기보다, 그 위에 `MergeabilityAssessment` 읽기 모델을 얹는 쪽이 안전하다.
- PR 기능이 생기면 `PullRequestRoute`와 `TargetDrift`를 별도 모델로 올리는 것이 좋다.
