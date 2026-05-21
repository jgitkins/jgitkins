# Task 2.37 Plan: server/presentation 잔재 bounded context 분류

### 목적
- `server/presentation` 에 남아 있는 공통 web concern 과 feature controller 를 분리한다.
- common exception handling / error spec / mapper 는 `common/presentation` 으로 고정하고, feature controller 는 bounded context presentation 으로 이동한다.
- top-level `presentation` 이 공용 web 컨테이너인지 기능 컨트롤러인지 헷갈리지 않게 한다.

### Scope Update
- 이번 pass 는 `server/presentation` 만 다룬다.
- application / domain / infrastructure 는 별도 task 로 처리한다.
- controller route 변경과 response contract 변경은 최소화하고 package ownership 만 정리한다.

### 분류 원칙
1. API error handling, problem spec, exception base, HTTP status mapper 는 common web support 로 본다.
2. feature controller 는 자신이 서비스하는 bounded context presentation 으로 옮긴다.
3. request/response DTO 와 presentation mapper 는 controller 소유 context 로 이동한다.
4. top-level `server/presentation` 은 migration 완료 후 common/presentation 만 남기는 것을 목표로 한다.

### current -> target inventory

#### 1) presentation/advice
| Current file | 성격 | Target package |
|---|---|---|
| `advice/GlobalExceptionHandler.java` | common exception translator | `common.presentation.advice` |

#### 2) presentation/advice/mapper
| Current file | 성격 | Target package |
|---|---|---|
| `advice/mapper/ApplicationErrorHttpStatusMapper.java` | application error mapper | `common.presentation.advice.mapper` |
| `advice/mapper/CompositeErrorHttpStatusMapper.java` | mapper aggregation | `common.presentation.advice.mapper` |
| `advice/mapper/DomainErrorHttpStatusMapper.java` | domain error mapper | `common.presentation.advice.mapper` |
| `advice/mapper/ErrorHttpStatusMapper.java` | mapper contract | `common.presentation.advice.mapper` |
| `advice/mapper/InfrastructureErrorHttpStatusMapper.java` | infra error mapper | `common.presentation.advice.mapper` |
| `advice/mapper/PresentationErrorHttpStatusMapper.java` | presentation error mapper | `common.presentation.advice.mapper` |

#### 3) presentation/api
| Current file | 성격 | Target package |
|---|---|---|
| `api/rest/MergeController.java` | merge feature controller | `change.review.presentation.api.rest` |
| `api/web/WebRepositoryController.java` | repository web/BFF controller | `repository.presentation.api.web` |

#### 4) presentation/common/error
| Current file | 성격 | Target package |
|---|---|---|
| `common/error/PresentationErrorCode.java` | common web error code | `common.presentation.error` |
| `common/error/PresentationProblemSpec.java` | common web problem spec | `common.presentation.error` |

#### 5) presentation/exception
| Current file | 성격 | Target package |
|---|---|---|
| `exception/PresentationException.java` | common web exception base | `common.presentation.exception` |

### controller consolidation note
- `MergeController` 는 change/review context 의 controller 와 기능이 겹칠 가능성이 높다.
- 이 문서에서는 우선 `change.review.presentation` 으로 이동시키고, 이후 duplicate contract 는 한쪽으로 흡수하는 것을 권장한다.
- `WebRepositoryController` 는 repository presentation 의 web/BFF adapter 로 취급한다.

### 3가지 방법
#### 방법 A: top-level presentation 유지
- 장점: 가장 적게 움직인다.
- 단점: common web 과 feature controller 가 계속 섞인다.
- 평가: 3/10.

#### 방법 B: common/presentation 만 만들고 feature controller 는 나중에 정리
- 장점: 공통 web concern 을 먼저 고정할 수 있다.
- 단점: controller ownership 이 남는다.
- 평가: 7/10.

#### 방법 C: common/presentation + bounded context presentation 을 같이 정리한다
- 장점: web boundary 가 명확하다.
- 장점: route ownership 과 error handling ownership 을 동시에 고정한다.
- 단점: 이동 범위가 넓다.
- 평가: 10/10.

**권장안:** 방법 C.

### 구현 순서
1. common/presentation 으로 갈 exception / error / mapper 를 먼저 확정한다.
2. MergeController 를 change/review presentation 으로 이동한다.
3. WebRepositoryController 를 repository presentation 으로 이동한다.
4. presentation package policy test 로 top-level 잔재를 막는다.

### 검증
- `ArchitecturePackageConventionTest` 에 common/presentation 외 top-level presentation 금지 규칙을 추가한다.
- controller test 는 route contract 와 error response contract 를 유지한다.
- `./gradlew :app-server:test` 로 회귀를 검증한다.

### NOT in scope
- application service 이동
- domain 이동
- infrastructure 이동
- API contract 자체 변경
