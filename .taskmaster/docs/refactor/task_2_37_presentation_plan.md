# Task 2.37 Plan: server/presentation 잔재 bounded context 분류

### 목적
- `server/presentation` 은 더 이상 하나의 공용 presentation layer 로 남기지 않는다.
- top-level `presentation` 에 남은 객체를 common web support 와 feature presentation 으로 분류한다.
- 이번 작업의 목표는 HTTP concern 과 feature controller 의 소유권을 분리하는 것이다.

### Scope Update
- 이번 pass 는 `server/presentation` 만 다룬다.
- `application` 과 `domain` 은 별도 task 로 처리한다.
- infrastructure 객체나 domain aggregate 는 이번 pass 에서 손대지 않는다.

### 배경
- `presentation` 아래에는 advice / mapper / api / common error / exception / dto / validate 가 혼재해 있다.
- 일부 타입은 전역 exception handling 과 error mapping 이고, 일부는 repository / change-review 의 feature controller 이다.
- 현재 구조는 common web support 와 feature controller 의 경계가 패키지명에서 드러나지 않는다.

### 분류 원칙
1. 공통 exception handler, error mapper, presentation error contract 는 `common/presentation` 으로 보낸다.
2. feature controller 는 각 bounded context `presentation` 으로 보낸다.
3. repository 전용 request/response DTO 는 `repository/presentation` 으로 보낸다.
4. `server/presentation` 은 migration 완료 후 empty shell 이 되는 것을 목표로 한다.

### current -> target inventory

#### 1) presentation advice
| Current file | 성격 | Target package |
|---|---|---|
| `advice/GlobalExceptionHandler.java` | common web exception handler | `io.jgitkins.server.common.presentation.advice` |

#### 2) presentation advice mapper
| Current file | 성격 | Target package |
|---|---|---|
| `advice/mapper/ApplicationErrorHttpStatusMapper.java` | application error mapper | `io.jgitkins.server.common.presentation.advice.mapper` |
| `advice/mapper/CompositeErrorHttpStatusMapper.java` | composite mapper | `io.jgitkins.server.common.presentation.advice.mapper` |
| `advice/mapper/DomainErrorHttpStatusMapper.java` | domain error mapper | `io.jgitkins.server.common.presentation.advice.mapper` |
| `advice/mapper/ErrorHttpStatusMapper.java` | common mapper contract | `io.jgitkins.server.common.presentation.advice.mapper` |
| `advice/mapper/InfrastructureErrorHttpStatusMapper.java` | infrastructure error mapper | `io.jgitkins.server.common.presentation.advice.mapper` |
| `advice/mapper/PresentationErrorHttpStatusMapper.java` | presentation error mapper | `io.jgitkins.server.common.presentation.advice.mapper` |

#### 3) presentation api
| Current file | 성격 | Target package |
|---|---|---|
| `api/rest/MergeController.java` | change/review feature controller | `io.jgitkins.server.change.review.presentation.api.rest` |
| `api/web/WebRepositoryController.java` | repository feature web controller | `io.jgitkins.server.repository.presentation.api.web` |

#### 4) presentation common error
| Current file | 성격 | Target package |
|---|---|---|
| `common/error/PresentationErrorCode.java` | common presentation error code | `io.jgitkins.server.common.presentation.error` |
| `common/error/PresentationProblemSpec.java` | common presentation problem spec | `io.jgitkins.server.common.presentation.error` |

#### 5) presentation dto
| Current file | 성격 | Target package |
|---|---|---|
| `dto/FileIndexEntry.java` | repository response DTO | `io.jgitkins.server.repository.presentation.dto` |

#### 6) presentation exception
| Current file | 성격 | Target package |
|---|---|---|
| `exception/PresentationException.java` | common presentation exception base | `io.jgitkins.server.common.presentation.exception` |

### test inventory
`server/presentation` 아래 테스트도 함께 분류한다. 실제 이관 시에는 source code 와 같은 seam 기준을 따른다.

| Current file | 성격 | Target package |
|---|---|---|
| `advice/GlobalExceptionHandlerTest.java` | common presentation advice test | `io.jgitkins.server.common.presentation.advice` |

### 3가지 방법
#### 방법 A: top-level presentation 유지
- 장점: 이동량이 적다.
- 단점: common web support 와 feature controller 가 계속 섞인다.
- 평가: 2/10.

#### 방법 B: common/presentation 과 context presentation 으로 분리
- 장점: 전역 웹 concern 과 feature controller 가 분리된다.
- 단점: import 정리량이 많다.
- 평가: 10/10.

#### 방법 C: presentation 전용 공통 모듈 분리
- 장점: 공통 HTTP concern 을 더 강하게 격리할 수 있다.
- 단점: 현재 스코프를 초과한다.
- 평가: 4/10.

### 구현 순서
1. common presentation advice 와 error contract 부터 먼저 이동한다.
2. repository/change-review feature controller 를 context presentation 으로 분리한다.
3. repository presentation DTO 를 정리한다.
4. `server/presentation` 경로가 비었는지 확인한다.
5. package policy test 로 common/context 경계를 고정한다.

### 검증 포인트
- `server/presentation` 아래 Java source 가 없어야 한다.
- `GlobalExceptionHandler` 는 common presentation 에 남아야 한다.
- feature controller 는 repository 또는 change/review presentation 으로만 존재해야 한다.
- presentation error contract 는 common presentation 에서만 관리해야 한다.
