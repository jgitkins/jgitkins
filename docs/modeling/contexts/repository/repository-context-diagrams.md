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
    participant App as Server(Application)
    participant FS as File System
    participant DB as Database

    Client->>App: create(command)
    App->>DB: owner / name 중복 검증
    DB-->>App: validation basis
    App->>DB: Repository 저장
    DB-->>App: saved repository
    App->>FS: bare repository 생성
    FS-->>App: initialized repository space
    App->>DB: default branch 저장

    alt requires initial content
        App->>FS: 초기 commit 생성 + HEAD 갱신
        FS-->>App: initialized state
        App->>DB: Repository initialized 상태 반영
        DB-->>App: updated repository
    else no initial content
        App->>App: 초기화 단계 생략
    end
    App-->>Client: RepositoryResult
```

애플리케이션 내부에서는 ownership 검증, namespace 해석, `Repository.create(...)`, provision orchestration을 수행한다.
