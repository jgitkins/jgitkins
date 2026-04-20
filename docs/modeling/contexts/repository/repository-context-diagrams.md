## Repository Context Diagrams

### TOC

- [목적](#목적)
- [Aggregate 구조 다이어그램](#aggregate-구조-다이어그램)
- [상태 전이 다이어그램](#상태-전이-다이어그램)
- [생성 / 초기화 시퀀스 다이어그램](#생성--초기화-시퀀스-다이어그램)

### 목적

`Repository Context`의 구조와 흐름을 정리한 문서다. 본문은 [Repository Context](./repository-context.md)를 참고한다.

### Aggregate 구조 다이어그램

```mermaid
classDiagram
    class Repository {
        <<Aggregate Root>>
        +RepositoryId id
        +OwnerType ownerType
        +OwnerId ownerId
        +RepositoryName name
        +RepositoryPath path
        +BranchName defaultBranch
        +RepositoryVisibility visibility
        +String clonePath
        +String credentialId
        +boolean requiresInitialContent
        +boolean initialized
        +markInit(syncedAt)
    }

    class Branch {
        <<Entity>>
        +Long repositoryId
        +String name
        +boolean locked
        +boolean ciEnabled
        +boolean defaultBranch
        +delete()
    }

    class RepositoryMember {
        <<Relation Model>>
        +RepositoryId repositoryId
        +UserId userId
        +RepositoryMemberRole role
        +LocalDateTime addedAt
    }

    class RepositoryId {
        <<Value Object>>
    }

    class RepositoryName {
        <<Value Object>>
    }

    class RepositoryPath {
        <<Value Object>>
    }

    class BranchName {
        <<Value Object>>
    }

    class RepositoryVisibility {
        <<Value Object>>
    }

    class OwnerType {
        <<Value Object>>
    }

    class OwnerId {
        <<Value Object>>
    }

    class RepositoryState {
        <<Proposed Enum>>
        PROVISIONED
        INITIALIZED
        DELETED
    }

    Repository *-- RepositoryId
    Repository *-- RepositoryName
    Repository *-- RepositoryPath
    Repository *-- BranchName
    Repository *-- RepositoryVisibility
    Repository *-- OwnerType
    Repository *-- OwnerId
    Repository ..> RepositoryState : future model
    Repository --> Branch : manages
    Repository --> RepositoryMember : grants access
```

`Repository Context`의 root, 내부 Entity, 관계 모델 경계를 나타낸다. `RepositoryMember`는 내부 Entity가 아니라 관계 모델이다.

### 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PROVISIONED : bare repository created
    PROVISIONED --> INITIALIZED : initial commit + HEAD updated
    PROVISIONED --> DELETED : deleted before initialization
    INITIALIZED --> DELETED : repository deleted
    DELETED --> [*]
```

상태 모델은 `PROVISIONED`, `INITIALIZED`, `DELETED`다. 현재 구현은 `initialized` boolean 중심이다.

### 생성 / 초기화 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant RMS as RepositoryManagementService
    participant RV as RepositoryValidator
    participant RNS as RepositoryNamespaceResolver
    participant RP as RepositoryPersistencePort
    participant Prov as RepositoryProvisioner
    participant RGP as RepositoryGitPort
    participant BR as BranchRepository
    participant CGP as CommitGitPort

    Client->>RMS: create(command)
    RMS->>RV: validateOwnership / validateRepositoryNameUnique
    RMS->>RNS: resolve(ownerType, ownerId)
    RMS->>RMS: Repository.create(...)
    RMS->>RP: save(repository)
    RP-->>RMS: saved repository
    RMS->>Prov: provision(saved, initialCommitOptions)

    Prov->>RGP: initialize(namespace, repoName)
    Prov->>BR: save(defaultBranch)

    alt requires initial content
        Prov->>CGP: commit(namespace, repoName, branch, ...)
        Prov->>RGP: updateHeadReference(namespace, repoName, branch)
        Prov->>RP: update(repository.markInit(...))
        RP-->>Prov: initialized repository
    else no initial content
        Prov-->>RMS: provisioned repository
    end

    RMS-->>Client: RepositoryResult
```

`RepositoryManagementService`가 aggregate를 생성·저장하고, `RepositoryProvisioner`가 Git 초기화와 기본 브랜치 생성, 선택적 초기 commit 반영을 처리한다.
