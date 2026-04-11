# Pull Request Readiness Context

## 목적
- source validation, mergeability, execution 결과를 조합해 PR의 최종 준비 상태를 만든다.
- 이 context의 결과는 사용자가 UI나 CLI에서 직접 보게 되는 application data의 기준이 된다.

## 핵심 질문
- 이 PR은 지금 merge 가능한 상태인가?
- 왜 `READY`, `BLOCKED`, `FAILING`, `UNKNOWN`인가?
- 다음 사용자가 취해야 할 행동은 무엇인가?

## 유비쿼터스 언어
- `Source Validation Status`
  - source branch 검증 상태.
- `Mergeability Status`
  - 병합 가능 상태.
- `Overall Readiness`
  - source validation과 mergeability를 합친 최종 상태.
- `Reason`
  - 현재 상태의 설명.
- `Next Action`
  - 사용자가 바로 취해야 할 다음 행동.

## Subdomain Classification
- Type: Core Domain
- Why:
  - 사용자가 실제로 신뢰하게 되는 최종 제품 경험은 readiness 설명에서 완성된다.
  - web과 future CLI가 같은 진실을 보여주려면 이 context의 의미가 핵심이 된다.

## 책임
- source validation 상태 정규화
- mergeability 상태 정규화
- overall readiness 계산
- reason / next action 생성
- UI/CLI 공통 결과 형태 유지
- PR 상세 조회에 필요한 동적 상태 조합

## 책임 밖
- mergeability 자체 계산
- policy match 자체 계산
- 실제 pipeline 실행
- presentation 래핑
- `PullRequestRoute`의 생성/닫기/병합 완료 같은 영속 상태 전이

## 주요 입력
- 저장된 `PullRequestRoute`
- source validation 결과
- mergeability 결과
- matched policy
- selected pipeline
- 현재 source / target branch head

## 주요 출력
- `PullRequestReadinessResult`
  - `sourceValidationStatus`
  - `mergeabilityStatus`
  - `overallReadiness`
  - `matchedPolicy`
  - `selectedPipeline`
  - `reason`
  - `nextAction`

## Aggregate
### Aggregate Root
- 없음

### 이유
- 이 context는 쓰기 모델의 일관성 경계보다 읽기 조합 경계에 가깝다.
- 따라서 aggregate를 억지로 만들기보다 read model과 assembler 중심으로 두는 편이 자연스럽다.
- PR 자체의 영속 상태는 Change Graph Context의 `PullRequestRoute`가 관리하고, 이 context는 그 route와 동적 계산 결과를 조합한다.

### Core Models
- `PullRequestDetail`
- `PullRequestRouteSummary`
- `PullRequestReadinessInput`
- `PullRequestReadinessResult`

### Value-like Models
- `SourceValidationStatus`
- `MergeabilityStatus`
- `OverallReadinessStatus`

## 핵심 불변식
- `overallReadiness`는 source validation과 mergeability로부터 파생되어야 한다.
- source validation이 `FAILING`이면 mergeability와 무관하게 overall은 `FAILING`이다.
- source validation이 `RUNNING` 또는 `UNKNOWN`이면 overall은 `UNKNOWN`이다.
- source validation이 `PASSING`이고 mergeability가 `MERGEABLE`일 때만 overall은 `READY`다.
- PR 상세 조회는 저장된 PR route와 조회 시점의 Git 계산 결과를 함께 사용해야 한다.
- 저장된 mergeability snapshot이 있더라도 readiness 계산의 최종 입력은 조회 시점에 재계산된 mergeability다.

## 조회 조합 원칙
### 저장소에서 읽는 값
- PR id
- repository id
- source branch / target branch
- route status
- 생성 시점 source / target head snapshot
- 승인 상태 또는 승인 요약
- 연결된 CI execution/check 참조

### 조회 시점에 다시 계산하는 값
- 현재 source branch head
- 현재 target branch head
- mergeability
- merge topology
- target drift
- source validation 최신 상태

### Application Use Case 경계
- `GetPullRequestDetailUseCase`는 PR 상세 화면과 CLI 상세 조회의 application orchestration이다.
- 이 use case는 `PullRequestRoute`를 영속 저장소에서 읽고, Change Graph Context에 현재 mergeability 해석을 요청한다.
- 이 use case는 CI Policy / Pipeline Execution 결과를 함께 읽어 readiness result를 만든다.
- 이름은 기술적인 `load`보다 사용자 목적이 드러나는 `GetPullRequestDetailUseCase`를 우선한다.

## Class Diagram
```mermaid
classDiagram
    class GetPullRequestDetailUseCase {
        <<Application Use Case>>
        +getDetail(PullRequestId)
    }

    class PullRequestDetail {
        <<Application Result>>
        +PullRequestRouteSummary route
        +PullRequestReadinessResult readiness
        +MergeabilityAssessment mergeability
        +String nextAction
    }

    class PullRequestRouteSummary {
        <<Application Result>>
        +String sourceBranch
        +String targetBranch
        +String routeStatus
        +String sourceHead
        +String targetHead
        +boolean targetDrifted
    }

    class PullRequestReadinessInput {
        <<Application Input>>
        +SourceValidationStatus sourceValidationStatus
        +MergeabilityAssessment mergeability
        +String matchedPolicy
        +String selectedPipeline
    }

    class PullRequestReadinessResult {
        <<Application Result>>
        +SourceValidationStatus sourceValidationStatus
        +MergeabilityStatus mergeabilityStatus
        +OverallReadinessStatus overallReadiness
        +String matchedPolicy
        +String selectedPipeline
        +String reason
        +String nextAction
    }

    class PullRequestReadinessAssembler {
        <<Application Support>>
        +assemble(PullRequestReadinessInput)
    }

    class PullRequestRoute {
        <<External Aggregate>>
    }

    class MergeabilityAssessment {
        <<External Value Object>>
    }

    class SourceValidationStatus {
        <<Enum>>
        PASSING
        FAILING
        RUNNING
        UNKNOWN
    }

    class MergeabilityStatus {
        <<Enum>>
        MERGEABLE
        CONFLICTING
        NO_COMMON_ANCESTOR
        UNKNOWN
    }

    class OverallReadinessStatus {
        <<Enum>>
        READY
        BLOCKED
        FAILING
        UNKNOWN
    }

    GetPullRequestDetailUseCase ..> PullRequestRoute
    GetPullRequestDetailUseCase ..> MergeabilityAssessment
    GetPullRequestDetailUseCase ..> PullRequestReadinessAssembler
    GetPullRequestDetailUseCase ..> PullRequestDetail
    PullRequestDetail ..> PullRequestRouteSummary
    PullRequestDetail ..> PullRequestReadinessResult
    PullRequestDetail ..> MergeabilityAssessment
    PullRequestReadinessInput ..> MergeabilityAssessment
    PullRequestReadinessAssembler ..> PullRequestReadinessInput
    PullRequestReadinessAssembler ..> PullRequestReadinessResult
    PullRequestReadinessResult ..> SourceValidationStatus
    PullRequestReadinessResult ..> MergeabilityStatus
    PullRequestReadinessResult ..> OverallReadinessStatus
```

## readiness 규칙 초안
| Source validation | Mergeability | Overall readiness |
|---|---|---|
| `PASSING` | `MERGEABLE` | `READY` |
| `PASSING` | `CONFLICTING` | `BLOCKED` |
| `PASSING` | `NO_COMMON_ANCESTOR` | `BLOCKED` |
| `FAILING` | any | `FAILING` |
| `RUNNING` | any | `UNKNOWN` |
| `UNKNOWN` | any | `UNKNOWN` |

## 주요 시나리오
### 1. PR detail 화면
- application use case는 저장된 PR route를 읽는다.
- source / target의 현재 head와 mergeability를 다시 계산한다.
- 사용자는 현재 PR의 overall readiness를 본다.
- 그 아래에서 source validation과 mergeability를 분리해서 확인한다.

### 2. CLI `pr checks`
- 같은 결과를 텍스트 기반으로 출력한다.
- web과 CLI는 같은 readiness result를 다른 포맷으로만 보여준다.

### 3. Merge action gate
- approval/merge action은 readiness 결과를 참고한다.
- 단, merge 권한이나 승인 정책은 이 context 밖의 문제다.

## Domain Service
- `PullRequestReadinessAssembler`

## 현재 코드 시드
- [PullRequestReadinessAssembler.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/application/support/readiness/PullRequestReadinessAssembler.java)
- [PullRequestReadinessResult.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/application/dto/readiness/PullRequestReadinessResult.java)
- [PullRequestReadinessInput.java](/Users/alzar/task/sources/jgitkins/jgitkins/server/src/main/java/io/jgitkins/server/application/dto/readiness/PullRequestReadinessInput.java)

## 현재 모델의 약점
- 아직 실제 PR read use case에 연결되지 않았다.
- stale target 상태와 partial failure 상태는 아직 명시적으로 반영되지 않았다.
- reason / next action 문구는 현재 영어 초안 수준이다.
- PR route 영속 저장소와 현재 Git 계산값을 조합하는 application orchestration이 아직 없다.

## 다음 리팩터링 힌트
- web에서 raw mergeability와 raw job result를 직접 조합하지 않도록, readiness result를 우선 API 응답 기준으로 삼아야 한다.
- 이후 CLI가 생겨도 같은 result를 재사용하면 된다.
- 다음 구현은 `PullRequestRoute` 영속 모델을 먼저 만들고, 그 다음 `GetPullRequestDetailUseCase`에서 route, mergeability, source validation을 조합하는 순서가 안전하다.
