## Identity & Access Context Diagrams

### TOC

- [목적](#목적)
- [Aggregate 구조 다이어그램](#aggregate-구조-다이어그램)
- [상태 전이 다이어그램](#상태-전이-다이어그램)
- [OAuth 로그인 시퀀스 다이어그램](#oauth-로그인-시퀀스-다이어그램)
- [Username 활성화 시퀀스 다이어그램](#username-활성화-시퀀스-다이어그램)
- [PAT 발급 시퀀스 다이어그램](#pat-발급-시퀀스-다이어그램)
- [PAT 조회 / 폐기 시퀀스 다이어그램](#pat-조회--폐기-시퀀스-다이어그램)

### 목적

`Identity & Access Context`의 구조와 흐름을 정리한 문서다. 본문은 [Identity & Access Context](./context.md)를 참고한다.

### Aggregate 구조 다이어그램

```mermaid
classDiagram
    class User {
        <<Aggregate Root Candidate>>
        +UserId id
        +Username username
        +String email
        +String displayName
        +String avatarUrl
        +UserAuthority authority
        +UserStatus status
        +LocalDateTime lastLoginAt
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +activateWithUsername(username)
        +login()
    }

    class UserIdentity {
        <<Entity Candidate>>
        +Long id
        +UserId userId
        +String providerName
        +String providerSub
    }

    class UserCredential {
        <<Entity Candidate>>
        +Long id
        +UserId userId
        +String name
        +String provider
        +String passwordHash
        +LocalDateTime createdAt
        +revoke()
    }

    class UserStatus {
        <<Enum>>
        PENDING
        ACTIVE
    }

    class UserAuthority {
        <<Enum>>
    }

    class UserId {
        <<Value Object>>
    }

    class Username {
        <<Value Object>>
    }

    User *-- UserId
    User *-- Username
    User *-- UserStatus
    User *-- UserAuthority
    User --> UserIdentity : linked by userId
    User --> UserCredential : linked by userId
```

`User`가 중심 모델이고, `UserIdentity`와 `UserCredential`은 현재 `userId`로 연결되는 종속 모델이다.

### 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> PENDING : OAuth sign-up or pending creation
    PENDING --> ACTIVE : activateWithUsername
    ACTIVE --> ACTIVE : login / profile update
    ACTIVE --> [*]
```

현재 문서 기준 핵심 상태는 `PENDING`, `ACTIVE`다.

### OAuth 로그인 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database
    participant External as External Auth Provider

    Client->>App: OAuth login request
    App->>External: provider token / user info 검증
    External-->>App: provider identity
    App->>DB: User / UserIdentity 조회
    DB-->>App: existing user or none
    App->>DB: 신규 User / UserIdentity 저장 또는 last login 갱신
    DB-->>App: current user
    App-->>Client: app token + login result
```

애플리케이션 내부에서는 `loginOrSignUp(...)`, identity 연결, 앱 토큰 발급을 수행한다.

- persisted
  - `User`
  - `UserIdentity`
  - `lastLoginAt`
- computed
  - 앱 토큰

### Username 활성화 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    Client->>App: activate(username)
    App->>DB: 현재 사용자 조회
    DB-->>App: current user
    App->>DB: username 중복 / organize name 충돌 / user repository 존재 여부 검증
    DB-->>App: validation basis
    App->>App: user.activateWithUsername(username)
    App->>DB: User 저장
    DB-->>App: updated user
    App-->>Client: activation result
```

애플리케이션 내부에서는 형식 검증과 상태 전이 호출을 수행한다.

- persisted
  - `username`
  - `status=ACTIVE`
  - `updatedAt`
- computed
  - 없음

### PAT 발급 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    Client->>App: issue PAT(name)
    App->>App: 현재 사용자 식별
    App->>App: random token 생성
    App->>App: token hash 생성
    App->>DB: UserCredential 저장
    DB-->>App: saved credential
    App-->>Client: plain token + credential metadata
```

애플리케이션 내부에서는 원문 토큰 생성과 해시 저장을 분리한다.

- persisted
  - `UserCredential`
  - `passwordHash`
  - `provider=PAT`
- computed
  - 원문 PAT

### PAT 조회 / 폐기 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    alt credential list
        Client->>App: list PAT credentials
        App->>App: 현재 사용자 식별
        App->>DB: userId + provider=PAT 조회
        DB-->>App: credential list
        App-->>Client: credential metadata
    else revoke credential
        Client->>App: revoke PAT(credentialId)
        App->>App: 현재 사용자 식별
        App->>DB: credentialId + userId 삭제
        DB-->>App: revoke result
        App-->>Client: revoke result
    end
```

조회와 폐기는 모두 현재 사용자 경계를 기준으로 수행한다.
