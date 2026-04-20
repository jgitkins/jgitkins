## Collaboration Context Diagrams

### TOC

- [목적](#목적)
- [Aggregate 구조 다이어그램](#aggregate-구조-다이어그램)
- [관계 다이어그램](#관계-다이어그램)
- [Organization 생성 시퀀스 다이어그램](#organization-생성-시퀀스-다이어그램)
- [Organization Member 추가 / 제거 시퀀스 다이어그램](#organization-member-추가--제거-시퀀스-다이어그램)
- [접근 가능 Organization 조회 시퀀스 다이어그램](#접근-가능-organization-조회-시퀀스-다이어그램)

### 목적

`Collaboration Context`의 구조와 흐름을 정리한 문서다. 본문은 [Collaboration Context](./collaboration-context.md)를 참고한다.

### Aggregate 구조 다이어그램

```mermaid
classDiagram
    class Organization {
        <<Aggregate Root>>
        +OrganizeId id
        +OrganizeName name
        +String description
        +UserId ownerId
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class OrganizationMember {
        <<Relation Model Candidate>>
        +OrganizeId organizeId
        +UserId userId
        +OrganizeMemberRole role
        +LocalDateTime addedAt
    }

    class OrganizeId {
        <<Value Object>>
    }

    class OrganizeName {
        <<Value Object>>
    }

    class UserId {
        <<Value Object>>
    }

    class OrganizeMemberRole {
        <<Enum>>
        MEMBER
    }

    Organization *-- OrganizeId
    Organization *-- OrganizeName
    Organization --> UserId : ownerId
    OrganizationMember *-- OrganizeId
    OrganizationMember *-- UserId
    OrganizationMember *-- OrganizeMemberRole
    Organization --> OrganizationMember : membership rows
```

현재 구현은 `Organization Member`를 aggregate 내부 collection보다 관계 모델에 가깝게 다룬다.

### 관계 다이어그램

```mermaid
flowchart LR
    User[User<br/>Identity & Access]
    Org[Organization<br/>Collaboration]
    Member[Organization Member<br/>Collaboration]
    Repo[Repository<br/>Repository Context]

    User -->|ownerId| Org
    User -->|userId| Member
    Org -->|organizeId| Member
    Org -->|can own| Repo
```

조직은 사용자와 저장소 사이의 협업 단위다.

### Organization 생성 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    Client->>App: create organization(name, description)
    App->>App: 현재 사용자 식별
    App->>DB: name 중복 검증
    DB-->>App: validation basis
    App->>App: Organization 생성
    App->>DB: Organization 저장
    DB-->>App: saved organization
    App-->>Client: organization result
```

애플리케이션 내부에서는 owner를 현재 사용자로 결정하고 `Organize.create(...)`를 호출한다.

- persisted
  - `Organization`
  - `ownerId`
  - `name`
- computed
  - 없음

### Organization Member 추가 / 제거 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    alt add member
        Client->>App: add member(organizeId, userId, role?)
        App->>DB: 중복 멤버 여부 검증
        DB-->>App: validation basis
        App->>App: OrganizationMember 생성
        App->>DB: membership 저장
        DB-->>App: saved membership
        App-->>Client: membership result
    else remove member
        Client->>App: remove member(organizeId, userId)
        App->>DB: membership 삭제
        DB-->>App: delete result
        App-->>Client: removal result
    end
```

role이 없으면 기본값 `MEMBER`를 사용한다.

### 접근 가능 Organization 조회 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor Client
    participant App as Server(Application)
    participant DB as Database

    Client->>App: list accessible organizations
    App->>App: 현재 사용자 식별
    App->>DB: owner 조직 조회
    DB-->>App: owned organizations
    App->>DB: membership 조직 조회
    DB-->>App: member organizations
    App-->>Client: accessible organizations
```

현재 접근 가능 여부는 owner 또는 member 여부를 기준으로 계산한다.
