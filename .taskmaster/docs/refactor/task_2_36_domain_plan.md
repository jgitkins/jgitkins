# Task 2.36 Plan: server/domain 잔재 bounded context 분류

### 목적
- `server/domain` 은 더 이상 하나의 공용 domain core 로 남기지 않는다.
- top-level `domain` 에 남은 객체를 shared primitive 와 context-specific domain 으로 분류한다.
- 이번 작업의 목표는 domain model 의 소유권이 패키지명만 봐도 드러나게 만드는 것이다.

### Scope Update
- 이번 pass 는 `server/domain` 만 다룬다.
- `application` 과 `presentation` 은 별도 task 로 처리한다.
- infrastructure 객체나 controller routing 은 이번 pass 에서 손대지 않는다.

### 배경
- `domain` 아래에는 aggregate / error / event / exception / model / vo 가 혼재해 있다.
- 일부 타입은 모든 bounded context 가 공유하는 primitive 이고, 일부는 repository 전용 domain rule 이다.
- 현재 구조는 shared primitive 와 context rule 의 경계가 패키지명에서 드러나지 않는다.

### 분류 원칙
1. 공통 aggregate base, domain event contract, domain error contract, domain exception base 는 `shared/domain` 으로 보낸다.
2. repository 전용 branch / commit / initial setup primitive 는 `repository/domain` 으로 보낸다.
3. 모든 bounded context 에서 쓰는 branch / commit / owner primitive 는 `shared/domain/model/vo` 로 보낸다.
4. `server/domain` 은 migration 완료 후 empty shell 이 되는 것을 목표로 한다.

### current -> target inventory

#### 1) domain aggregate
| Current file | 성격 | Target package |
|---|---|---|
| `aggregate/AbstractAggregateRoot.java` | shared aggregate base | `io.jgitkins.server.shared.domain.aggregate` |
| `aggregate/AggregateRoot.java` | shared aggregate marker | `io.jgitkins.server.shared.domain.aggregate` |
| `aggregate/Identifiable.java` | shared identity marker | `io.jgitkins.server.shared.domain.aggregate` |

#### 2) domain error
| Current file | 성격 | Target package |
|---|---|---|
| `error/DomainErrorCode.java` | shared domain error code | `io.jgitkins.server.shared.domain.error` |
| `error/DomainProblemSpec.java` | shared domain problem spec | `io.jgitkins.server.shared.domain.error` |

#### 3) domain event
| Current file | 성격 | Target package |
|---|---|---|
| `event/DomainEvent.java` | shared domain event contract | `io.jgitkins.server.shared.domain.event` |

#### 4) domain exception
| Current file | 성격 | Target package |
|---|---|---|
| `exception/DomainException.java` | shared domain exception base | `io.jgitkins.server.shared.domain.exception` |
| `exception/DefaultBranchDeletionNotAllowedException.java` | repository domain rule | `io.jgitkins.server.repository.domain.exception` |

#### 5) domain root enums
| Current file | 성격 | Target package |
|---|---|---|
| `GitAction.java` | shared git action primitive | `io.jgitkins.server.shared.domain` |
| `GitAuthority.java` | shared git authority primitive | `io.jgitkins.server.shared.domain` |

#### 6) domain model / vo
| Current file | 성격 | Target package |
|---|---|---|
| `model/vo/BranchName.java` | shared branch primitive | `io.jgitkins.server.shared.domain.model.vo` |
| `model/vo/CommitHash.java` | shared commit hash primitive | `io.jgitkins.server.shared.domain.model.vo` |
| `model/vo/InitialCommitOptions.java` | repository initial setup primitive | `io.jgitkins.server.repository.domain.model.vo` |
| `model/vo/OwnerId.java` | shared owner primitive | `io.jgitkins.server.shared.domain.model.vo` |
| `model/vo/OwnerType.java` | shared owner type primitive | `io.jgitkins.server.shared.domain.model.vo` |
| `model/vo/SequenceNumber.java` | shared sequence primitive | `io.jgitkins.server.shared.domain.model.vo` |

### test inventory
`server/domain` 아래 테스트도 함께 분류한다. 실제 이관 시에는 source code 와 같은 seam 기준을 따른다.

| Current file | 성격 | Target package |
|---|---|---|
| `model/vo/InitialCommitOptionsTest.java` | repository initial setup primitive test | `io.jgitkins.server.repository.domain.model.vo` |

### 3가지 방법
#### 방법 A: top-level domain 유지
- 장점: 이동량이 적다.
- 단점: shared primitive 와 context domain 이 계속 섞인다.
- 평가: 2/10.

#### 방법 B: shared/domain 과 context domain 으로 분리
- 장점: 공통 도메인과 컨텍스트 소유권이 분리된다.
- 단점: import 정리량이 많다.
- 평가: 10/10.

#### 방법 C: domain 전용 공통 모듈 분리
- 장점: shared primitive 를 더 강하게 격리할 수 있다.
- 단점: 모듈 분리가 과해지고 현재 스코프를 초과한다.
- 평가: 4/10.

### 구현 순서
1. shared domain primitive 부터 먼저 이동한다.
2. repository 전용 domain primitive 와 exception 을 분리한다.
3. context domain 에 맞는 import 정리를 진행한다.
4. `server/domain` 경로가 비었는지 확인한다.
5. package policy test 로 shared/context 경계를 고정한다.

### 검증 포인트
- `server/domain` 아래 Java source 가 없어야 한다.
- shared domain primitive 는 repository, change/review, execution, identity/access, collaboration 에서 공통으로 참조 가능해야 한다.
- repository 전용 `InitialCommitOptions` 는 repository domain 으로만 남아야 한다.
- `GlobalExceptionHandler` 와 domain error mapper 가 새 `shared/domain` 경로를 참조해야 한다.
