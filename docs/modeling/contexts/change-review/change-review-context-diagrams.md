## Change & Review Context Diagrams

### TOC

- [목적](#목적)
- [Aggregate 구조 다이어그램](#aggregate-구조-다이어그램)
- [상태 전이 다이어그램](#상태-전이-다이어그램)
- [Pull Request 생성 시퀀스 다이어그램](#pull-request-생성-시퀀스-다이어그램)
- [Pull Request 상세 조회 시퀀스 다이어그램](#pull-request-상세-조회-시퀀스-다이어그램)
- [Merge 수행 시퀀스 다이어그램](#merge-수행-시퀀스-다이어그램)

### 목적

`Change & Review Context`의 구조와 흐름을 정리한 문서다. 본문은 [Change & Review Context](./change-review-context.md)를 참고한다.

### Aggregate 구조 다이어그램

```mermaid
classDiagram
    class PullRequest {
        <<Aggregate Root>>
        +PullRequestId id <<persisted>>
        +RepositoryId repositoryId <<persisted>>
        +BranchHeadSnapshot source <<persisted>>
        +BranchHeadSnapshot target <<persisted>>
        +PullRequestStatus status <<persisted>>
        +MergeabilityAssessment lastAssessmentSnapshot <<persisted optional>>
        +TargetDrift targetDrift <<persisted optional>>
        +close()
        +reopen()
        +markMerged() 
        +markTargetDrifted(currentTarget)
    }

    class BranchHeadSnapshot {
        <<Value Model>>
        +BranchName branchName
        +CommitHash commitHash
    }

    class TargetDrift {
        <<Value Model>>
        +boolean drifted
        +CommitHash previousTargetHead
        +CommitHash currentTargetHead
    }

    class PullRequestStatus {
        <<Enum>>
        OPEN
        CLOSED
        MERGED
    }

    class PullRequestId {
        <<Value Object>>
    }

    class MergeabilityAssessment {
        <<Read-Side Result>>
        +MergeabilityStatus status <<computed>>
        +MergeTopologySummary topology <<computed>>
        +List~String~ conflicts <<computed>>
        +String reason <<computed>>
    }

    class MergeabilityStatus {
        <<Enum>>
        MERGEABLE
        CONFLICTING
        NO_COMMON_ANCESTOR
        UNKNOWN
    }

    class RepositoryId {
        <<Value Object>>
    }

    PullRequest *-- PullRequestId
    PullRequest *-- RepositoryId
    PullRequest *-- BranchHeadSnapshot : stored source
    PullRequest *-- BranchHeadSnapshot : stored target
    PullRequest *-- TargetDrift
    PullRequest *-- PullRequestStatus
    PullRequest ..> MergeabilityAssessment : observes / snapshots
    MergeabilityAssessment *-- MergeabilityStatus
```

`Change & Review Context`의 aggregate, 내부 값 모델, read-side 결과 경계를 나타낸다.
- `persisted`: 저장 대상
- `persisted optional`: 도메인 모델에는 있으나 현재 구현에서 항상 저장하지는 않음
- `computed`: 조회 시 계산값
- `markMerged()`: merge command 자체가 아니라 aggregate 상태 전이 메서드다.

### 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> OPEN : pull request created
    OPEN --> CLOSED : close
    CLOSED --> OPEN : reopen
    OPEN --> MERGED : merge completed
    MERGED --> [*]
    CLOSED --> [*]
```

상태는 `OPEN`, `CLOSED`, `MERGED`다.

### Pull Request 생성 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant PRS as PullRequestService
    participant RLS as RepositoryLookupService
    participant RNS as RepositoryNamespaceResolver
    participant BGP as BranchGitPort
    participant PRR as PullRequestRepository

    Client->>PRS: createPullRequest(command)
    PRS->>RLS: findByPath(namespace, repoName)
    RLS-->>PRS: repository
    PRS->>RNS: resolve(repository)
    RNS-->>PRS: actual namespace
    PRS->>BGP: getHeadCommitHash(namespace, repoName, sourceBranch)
    BGP-->>PRS: source head
    PRS->>BGP: getHeadCommitHash(namespace, repoName, targetBranch)
    BGP-->>PRS: target head
    PRS->>PRS: PullRequest.create(repositoryId, sourceSnapshot, targetSnapshot)
    PRS->>PRR: save(pullRequest)
    PRR-->>PRS: saved pull request
    PRS-->>Client: PullRequestResult
```

Pull Request는 branch 자체가 아니라 생성 시점 source/target snapshot을 저장한다.

- persisted
  - `repositoryId`
  - source branch name / source head
  - target branch name / target head
  - `status=OPEN`
  - `createdAt`, `updatedAt`
- computed
  - 없음

### Pull Request 상세 조회 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant PRS as PullRequestService
    participant PRR as PullRequestRepository
    participant RPP as RepositoryPersistencePort
    participant PMR as PullRequestMergeabilityResolver
    participant BGP as BranchGitPort
    participant MGP as MergeGitPort

    Client->>PRS: getPullRequestDetail(pullRequestId)
    PRS->>PRR: findById(id)
    PRR-->>PRS: pull request
    PRS->>RPP: findById(repositoryId)
    RPP-->>PRS: repository

    PRS->>PMR: currentSourceHead(repository, pullRequest)
    PMR->>BGP: getHeadCommitHash(namespace, repoName, sourceBranch)
    BGP-->>PMR: current source head
    PMR-->>PRS: current source snapshot

    PRS->>PMR: currentTargetHead(repository, pullRequest)
    PMR->>BGP: getHeadCommitHash(namespace, repoName, targetBranch)
    BGP-->>PMR: current target head
    PMR-->>PRS: current target snapshot

    PRS->>PRS: pullRequest.markTargetDrifted(currentTarget)
    PRS->>PMR: assess(repository, observedPullRequest)
    PMR->>MGP: previewMergeability(namespace, repoName, source, target)
    MGP-->>PMR: MergeResult
    PMR-->>PRS: MergeabilityAssessment

    PRS-->>Client: PullRequestDetailResult
```

상세 조회는 저장된 기준점과 현재 Git 상태를 함께 반환한다.

- persisted
  - 기존 `PullRequest`
  - stored source snapshot
  - stored target snapshot
  - `status`
- computed
  - current source snapshot
  - current target snapshot
  - `TargetDrift`
  - `MergeabilityAssessment`
  - 충돌 파일 목록
  - fast-forward 가능 여부
  - merge commit 필요 여부

### Merge 수행 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant MS as MergeService
    participant MGP as MergeGitPort

    Client->>MS: performMerge(namespace, repoName, request)
    MS->>MGP: merge(namespace, repoName, request)
    MGP-->>MS: MergeResult
    MS-->>Client: MergeResult
```

현재 구현은 merge command와 결과 반환까지만 연결한다. merge 성공 후 `MERGED` 상태 저장은 별도 유스케이스로 통합되지 않았다.

- persisted
  - 현재 구현 기준 없음
- computed
  - `MergeResult`
  - new commit id
  - result tree id
  - 충돌 파일 목록
