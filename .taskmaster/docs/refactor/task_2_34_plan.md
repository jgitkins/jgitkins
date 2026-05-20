# Task 2.34 Plan: server/common/infrastructure 와 context infrastructure 경계 정리

### 목적
- `server/common/infrastructure` 는 공통 설정, 공통 보안/웹/예외 처리, 공통 persistence wiring 같은 기술 glue만 둔다.
- 각 bounded context 는 자기 소유의 `infrastructure` 를 가진다.
- `mapper`, `persistence adapter`, `MBG entity/model`, `context-specific support` 가 top-level `server/infrastructure` 에 남아 있으면 경계가 다시 흐려지므로, 이를 context 소유로 옮긴다.
- 이번 작업의 목표는 package tree 를 코드의 소유권과 일치시키는 것이다. 기능 추가는 없다.

### Scope Update
- 이번 진행은 `infrastructure` only 로 제한한다.
- `application`, `domain`, `presentation` 에 남아 있는 잔재는 이번 pass 에서 옮기지 않고, 이 문서에 deferred inventory 로 기록한다.
- infrastructure 정리는 계속 진행하되, non-infrastructure layer 와 섞어서 한 번에 처리하지 않는다.

### 배경
- 이미 `collaboration`, `repository`, `identity.access`, `change.review`, `execution` 은 별도 bounded context 로 나뉘어 있다.
- `core-persistence` 는 DataSource/MyBatis/transaction 같은 공통 기술 설정을 분리해 둔 상태다.
- 그럼에도 일부 handcrafted persistence adapter 와 mapper 는 아직 `io.jgitkins.server.infrastructure` 아래에 남아 있고, MBG 생성 model 도 전역 persistence package 에 남아 있다.
- 이 상태에서는 bounded context 가 package tree 에서 보이지 않는다. 다음 사람이 읽으면 “이 객체가 누구 소유인지” 다시 추측해야 한다.

### 현재 관찰
- 공통 기술 설정은 이미 `core-*` 로 분리되는 방향이 잡혀 있다.
- collaboration context 는 `application`, `domain`, `presentation` 이 정리되어 있고, infrastructure 만 잔재가 남아 있다.
- repository / identity / change.review / execution context 도 같은 패턴의 잔재가 있다.
- `ArchitecturePackageConventionTest` 는 service/controller package 와 일부 import 규칙만 보고 있어서, infrastructure ownership 까지는 아직 강제하지 못한다.

### Deferred Inventory
이번 pass 에서 의도적으로 남겨두는 객체들이다. 다음 pass 에서 처리한다.

| Root | 대표 잔재 | 이유 |
|---|---|---|
| `server/application` | `service/`, `validate/`, `mapper/`, `dto/`, `exception/` | 이번 scope 에서는 infrastructure 만 이동한다. |
| `server/domain` | `aggregate/`, `entity/`, `event/`, `vo/` | domain migration 은 infrastructure 이관과 분리한다. |
| `server/presentation` | `api/`, `dto/`, `mapper/`, `advice/` | HTTP contract 와 오류 응답은 별도 pass 로 다룬다. |
| `server/infrastructure` | repository / execution 쪽 context-specific adapter, mapper, model 잔재 | 이 pass 에서 계속 정리하되, 공통 glue 는 남긴다. |

### 이미 존재하는 것
- `core-persistence` 에서 DataSource/MyBatis 공통 설정을 분리한 기준이 있다.
- `server/shared/application/support/RepositoryNamespaceResolver.java` 와 `RepositoryAccessibilityService.java` 는 collaboration query seam 에 의존하도록 이미 정리되어 있다.
- `server/application/ArchitecturePackageConventionTest.java` 는 package 정책을 추가하기 쉬운 구조다.
- `collaboration`, `repository`, `identity.access`, `change.review`, `execution` 의 context 패키지는 이미 존재한다. 새 컨텍스트를 만드는 작업은 아니다.

### 핵심 원칙
1. `server/common/infrastructure` 는 공통 기술 설정만 둔다.
2. context 의미가 있는 persistence model, mapper, adapter 는 각 bounded context 아래 `infrastructure` 로 간다.
3. generated MBG 산출물이라고 해서 공통 레이어에 예외를 주지 않는다.
4. `shared/application` 은 cross-context 협력자만 둔다. persistence detail 은 두지 않는다.
5. package policy 는 문서가 아니라 테스트로 강제한다.

### 패키지 정책
```text
io.jgitkins.server
├── common
│   └── infrastructure
│       ├── config
│       ├── security
│       ├── web
│       ├── persistence
│       └── exception
├── collaboration
│   ├── domain
│   ├── application
│   ├── presentation
│   └── infrastructure
├── repository
│   ├── domain
│   ├── application
│   ├── presentation
│   └── infrastructure
├── identity
│   └── access
│       ├── domain
│       ├── application
│       ├── presentation
│       └── infrastructure
├── change
│   └── review
│       ├── domain
│       ├── application
│       ├── presentation
│       └── infrastructure
└── execution
    ├── domain
    ├── application
    ├── presentation
    └── infrastructure
```

### 데이터 흐름
```text
HTTP / API
  -> context.presentation
  -> context.application
  -> context.infrastructure
  -> DB / external system

shared/application
  -> cross-context query / policy helper
  -> context.application port
  -> context.infrastructure
```

### 3가지 방법
#### 방법 A: top-level infrastructure 유지, 문서만 정리
- 장점: 변경량이 작다.
- 단점: package tree 가 계속 거짓말을 한다.
- 평가: 3/10. 문서만 예뻐지고 소유권은 안 바뀐다.

#### 방법 B: handwritten infra 만 context 로 이동
- 장점: blast radius 가 줄고, 가장 먼저 눈에 보이는 잔재를 없앨 수 있다.
- 단점: MBG generated model 은 여전히 전역에 남아 있을 수 있다.
- 평가: 7/10. 빠르지만 아직 반쪽이다.

#### 방법 C: handwritten infra + generated persistence artifacts 를 context 로 이동
- 장점: bounded context 소유권이 package tree 에서 실제로 보인다.
- 장점: 다음 module split 때 추가 청소가 적다.
- 단점: 이동 파일 수가 많아지고 import churn 이 생긴다.
- 평가: 10/10. 이번 요구사항에 가장 맞는다.

**권장안:** 방법 C.

### 구현 범위
#### 1단계: 공통 기술 경계 고정
- `server/common/infrastructure` 를 공통 glue 전용 패키지로 정의한다.
- 공통으로 남길 후보:
  - DataSource / MyBatis / transaction config
  - 공통 security filter chain 과 handler
  - 공통 web exception translation
  - 공통 Spring config, properties binding, shared technical util
- 공통으로 두지 않을 것:
  - context-specific persistence adapter
  - context-specific mapper
  - context-specific persistence model/entity
  - context-specific domain conversion helper

#### 2단계: collaboration infrastructure 정리
- `io.jgitkins.server.infrastructure.adapter.persistence.OrganizePersistenceAdapter`
- `io.jgitkins.server.infrastructure.adapter.persistence.OrganizeMemberPersistenceAdapter`
- `io.jgitkins.server.infrastructure.mapper.OrganizeDomainMapper`
- `io.jgitkins.server.infrastructure.mapper.OrganizeMemberDomainMapper`
- 위 파일들을 `io.jgitkins.server.collaboration.infrastructure` 아래로 이동한다.
- collaboration 전용 MBG model 과 mapper 도 같은 context 아래로 이동한다.

#### 3단계: 다른 bounded context infrastructure 정리
- repository context:
  - repository persistence adapter
  - repository mapper
  - repository MBG model / mapper
  - repository 전용 technical support
- identity.access context:
  - user / credential / identity persistence adapter
  - mapper / MBG model
- change.review context:
  - pull request persistence adapter
  - mapper / MBG model
- execution context:
  - job / runner persistence adapter
  - mapper / MBG model
- 이동 순서는 “현재 잔재가 많은 context -> 의존성이 적은 context” 순으로 잡는다.

#### 4단계: common test gate 추가
- `ArchitecturePackageConventionTest` 에 다음 정책을 추가한다.
  - `server.common.infrastructure` 외에는 공통 기술 설정이 흘러가지 않아야 한다.
  - `server.infrastructure` 는 business persistence model / adapter / mapper 를 소유하지 않아야 한다.
  - 각 bounded context 는 자기 context 밖의 infrastructure package 를 import 하지 않아야 한다.
- context 별 infrastructure 테스트는 adapter unit test 로 mapping / persistence query / error handling 을 검증한다.

### 이동 대상 분류
| 범주 | 처리 |
|---|---|
| 공통 설정 | `server/common/infrastructure` 로 유지 또는 이동 |
| context adapter | 각 context `infrastructure/adapter` 로 이동 |
| context mapper | 각 context `infrastructure/mapper` 로 이동 |
| context persistence model | 각 context `infrastructure/persistence/model` 로 이동 |
| MBG mapper | 각 context `infrastructure/persistence/mapper` 로 이동 |
| shared business helper | `server/shared/application` 유지 |

### 우선순위
1. collaboration context 잔재를 먼저 옮긴다. 잔재가 가장 눈에 띄고, shared/application 의존도도 이미 낮다.
2. repository context 를 다음으로 옮긴다. 화면/조회 흐름과 맞물려 있지만 구조가 비교적 안정적이다.
3. identity.access, change.review, execution 을 context 별로 마무리한다.
4. 마지막에 common infrastructure 만 남기고 top-level `server/infrastructure` 의 역할을 축소한다.

### 테스트 계획
- `ArchitecturePackageConventionTest`
  - context infrastructure ownership 가드 추가
  - common infrastructure 예외 규칙 추가
- collaboration context
  - persistence adapter test
  - mapper test
  - service test 에서 adapter import 경계 확인
- repository / identity.access / change.review / execution
  - 각 context 별 adapter and mapper test
  - package import drift 확인

### 실패 모드
1. package 이동 후 bean scan 이 누락되면 Spring 이 mapper/adapter 를 못 찾는다.
2. generated MBG package 를 바꾸지 않으면 compile import drift 가 대량 발생한다.
3. common infrastructure 를 너무 넓히면 다시 cross-context 잡동사니가 된다.

### NOT in scope
- Organize -> Organization rename
- API route 변경
- Gradle module split 추가 작업
- persistence schema 변경
- business behavior 변경
- `application`, `domain`, `presentation` 의 잔재 이동
- `server/common/infrastructure` 를 넘어선 공통 레이어 확장

### 병렬화 전략
```text
Lane A: common infrastructure policy + architecture test
Lane B: collaboration infrastructure move
Lane C: repository infrastructure move
Lane D: identity.access / change.review / execution infrastructure move
```

| Lane | 대상 | 이유 |
|---|---|---|
| A | 공통 정책, 테스트 | 다른 lane 의 기준선이다 |
| B | collaboration | 가장 먼저 정리해야 할 잔재가 많다 |
| C | repository | collaboration 과 파일이 거의 안 겹친다 |
| D | identity.access / change.review / execution | 서로 다른 context 로 나눠서 처리 가능하다 |

### 기대 결과
- `server/common/infrastructure` 는 정말 공통 glue 만 남는다.
- persistence model / mapper / adapter 는 context 소유로 보인다.
- package tree 가 bounded context 소유권을 숨기지 않는다.
- 다음 module split 작업이 쉬워진다. 지금 안 하면 다음에도 또 같은 정리를 한다. 소프트웨어는 같은 청소를 이름만 바꿔서 두 번 시키는 데 재능이 있다.
