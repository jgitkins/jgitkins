# Task 2.36 Plan: server/domain 잔재 bounded context 분류

### 목적
- `server/domain` 에 남은 공용 도메인 primitive 와 context-specific domain 을 분리한다.
- aggregate / error / event / exception / vo 를 seam 기준으로 `shared/domain` 과 각 bounded context domain 으로 재배치한다.
- domain package 가 단일 공용 창고처럼 보이지 않게 만든다.

### Scope Update
- 이번 pass 는 `server/domain` 만 다룬다.
- application / presentation / infrastructure 는 별도 task 로 처리한다.
- domain behavior 변경은 최소화하고 package ownership 만 정렬한다.

### 분류 원칙
1. 여러 context 가 공유하는 aggregate base, event contract, domain exception base, git primitive 는 `shared/domain` 으로 보낸다.
2. repository / execution / collaboration / identity-access 전용 domain object 는 각 context domain 으로 보낸다.
3. domain base 가 공용인지 컨텍스트 전용인지는 사용처와 불변식으로 판단한다.
4. top-level `server/domain` 은 migration 완료 후 empty 를 목표로 한다.

### current -> target inventory

#### 1) domain/aggregate
| Current file | 성격 | Target package |
|---|---|---|
| `aggregate/AbstractAggregateRoot.java` | shared aggregate base | `shared.domain.aggregate` |
| `aggregate/AggregateRoot.java` | shared aggregate contract | `shared.domain.aggregate` |
| `aggregate/Identifiable.java` | shared identity contract | `shared.domain.aggregate` |

#### 2) domain/error
| Current file | 성격 | Target package |
|---|---|---|
| `error/DomainErrorCode.java` | shared domain error code | `shared.domain.error` |
| `error/DomainProblemSpec.java` | shared domain problem spec | `shared.domain.error` |

#### 3) domain/event
| Current file | 성격 | Target package |
|---|---|---|
| `event/DomainEvent.java` | shared domain event contract | `shared.domain.event` |

#### 4) domain/exception
| Current file | 성격 | Target package |
|---|---|---|
| `exception/DomainException.java` | shared domain exception base | `shared.domain.exception` |
| `exception/DefaultBranchDeletionNotAllowedException.java` | repository branch rule violation | `repository.domain.exception` |

#### 5) domain root enums
| Current file | 성격 | Target package |
|---|---|---|
| `GitAction.java` | shared git action primitive | `shared.domain.git` |
| `GitAuthority.java` | shared git authority primitive | `shared.domain.git` |

#### 6) domain/model/vo
| Current file | 성격 | Target package |
|---|---|---|
| `model/vo/BranchName.java` | shared branch primitive | `shared.domain.vo` |
| `model/vo/CommitHash.java` | shared commit primitive | `shared.domain.vo` |
| `model/vo/InitialCommitOptions.java` | repository creation primitive | `repository.domain.vo` |
| `model/vo/OwnerId.java` | shared owner primitive | `shared.domain.vo` |
| `model/vo/OwnerType.java` | shared owner primitive | `shared.domain.vo` |
| `model/vo/SequenceNumber.java` | execution sequencing primitive | `execution.domain.vo` |

### 3가지 방법
#### 방법 A: top-level domain 유지
- 장점: 가장 적게 움직인다.
- 단점: 공용 primitive 가 무엇인지 분간이 안 된다.
- 평가: 2/10.

#### 방법 B: shared/domain 만 만든다
- 장점: 공용 primitive 를 한곳에 모을 수 있다.
- 단점: context-specific domain 을 못 가른다.
- 평가: 6/10.

#### 방법 C: shared/domain + context domain 을 함께 정리한다
- 장점: 공용 primitive 와 context domain 경계가 가장 선명하다.
- 장점: aggregate/event/exception base 를 안전하게 공유할 수 있다.
- 단점: 이동 대상이 많다.
- 평가: 10/10.

**권장안:** 방법 C.

### 구현 순서
1. shared/domain 으로 보낼 base contract 를 먼저 확정한다.
2. repository / execution / collaboration / identity-access 전용 VO 와 exception 을 context 소유로 이동한다.
3. 각 context aggregate 가 shared/domain primitives 만 의존하도록 import 를 정리한다.
4. domain package policy test 로 top-level 잔재를 막는다.

### 검증
- `ArchitecturePackageConventionTest` 에 shared/domain 외 top-level domain 금지 규칙을 추가한다.
- repository/execution/collaboration/identity-access domain test 에서 VO 불변식을 유지한다.
- `./gradlew :app-server:test` 로 회귀를 검증한다.

### NOT in scope
- application/service 이동
- presentation/controller 이동
- infrastructure 이동
- domain behavior 재설계
