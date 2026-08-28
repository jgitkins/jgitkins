# Exception Handling Analysis

## 목적

이 문서는 현재 `staged` 된 예외 클래스 변경을 기준으로 `jgitkins-server`의 예외처리 구조를 분석하고, 현재 구조의 장단점과 더 이상적인 예외처리 구조를 제안하기 위해 작성했다.

분석 대상은 아래 7개 파일이다.

- `app-server/src/main/java/io/jgitkins/server/shared/application/exception/ApplicationException.java`
- `app-server/src/main/java/io/jgitkins/server/shared/domain/exception/DomainException.java`
- `app-server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerAlreadyActiveException.java`
- `app-server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerTokenMismatchException.java`
- `app-server/src/main/java/io/jgitkins/server/execution/domain/exception/RunnerTokenMissingException.java`
- `app-server/src/main/java/io/jgitkins/server/identity/access/domain/exception/UserAlreadyActivatedException.java`
- `app-server/src/main/java/io/jgitkins/server/common/infrastructure/exception/InfrastructureException.java`

함께 참고한 현재 핵심 구조는 아래 파일들이다.

- `core-common/src/main/java/io/jgitkins/core/common/exception/JgitkinsException.java`
- `app-server/src/main/java/io/jgitkins/server/common/presentation/advice/GlobalExceptionHandler.java`
- `app-server/src/main/java/io/jgitkins/server/common/presentation/advice/mapper/*`
- `core-web/src/main/java/io/jgitkins/core/web/api/response/ApiResponse.java`
- `core-web/src/main/java/io/jgitkins/core/web/api/response/ApiError.java`

## 선택한 방식

이번 프로젝트에 가장 적합한 방식은 **계층별 예외를 명확히 분리하고 현재 `ApiResponse` 계약을 유지하는 방식**이다.

이유는 아래와 같다.

- 이미 `ApiResponse` 중심 응답 규약이 존재한다.
- 이번 staged 변경도 계층별 베이스 예외 도입 방향이다.
- 전체 API 응답 계약을 깨지 않으면서 구조를 개선할 수 있다.
- 이후 필요하면 Spring 표준 오류 응답과의 정렬도 점진 검토할 수 있다.

즉, **단기적으로는 계층형 예외 체계를 완성하고**, **장기적으로는 Spring 표준 오류 응답과의 호환을 검토**하는 전략이 가장 현실적이다.

## 현재 구조 설명

현재 예외 흐름은 아래와 같다.

1. 각 계층에서 예외 발생
2. 다수의 코드가 `JgitkinsException` 또는 `IllegalArgumentException`, `IllegalStateException`을 직접 던짐
3. `GlobalExceptionHandler`가 `JgitkinsException`을 받아 `ErrorCode`를 해석
4. `CompositeErrorHttpStatusMapper`가 `ErrorCode` 타입별로 HTTP status를 결정
5. 최종적으로 `ApiResponse.failure(...)` 형태로 응답

요약하면, 현재 시스템의 중심은 **예외 타입 자체**보다 **`ErrorCode`** 이다.

### 현재 구조의 장점

- `ErrorCode`를 기준으로 응답 코드와 메시지를 일관되게 관리할 수 있다.
- `GlobalExceptionHandler`와 `CompositeErrorHttpStatusMapper`가 분리되어 있어 응답 정책을 한 곳에서 모을 수 있다.
- `ApiResponse` 포맷이 이미 통일되어 있어 클라이언트 계약이 비교적 안정적이다.

### 현재 구조의 한계

- 계층별 예외 타입이 도입되기 전까지는 거의 모든 비즈니스 예외가 `JgitkinsException`으로 평탄화되어 있었다.
- `GlobalExceptionHandler`는 예외 타입이 아니라 `ErrorCode instanceof ...` 로 source를 추론한다.
- 서비스 계층에서 도메인 예외를 잡아 다시 `JgitkinsException`으로 감싸는 패턴이 존재한다.
- 도메인과 인프라 내부에 `IllegalArgumentException`, `IllegalStateException`, `RuntimeException`이 많이 남아 있다.
- 일부 예외는 응답용 에러 코드와 실제 의미가 어긋난다.

## staged 변경의 의미

이번 변경은 예외를 계층별로 분리하려는 시도라는 점에서 방향 자체는 좋다.

- `ApplicationException`
- `DomainException`
- `InfrastructureException`
- 일부 도메인 예외를 `DomainException` 하위로 이동

이 변화가 가지는 의미는 명확하다.

- 도메인 규칙 위반은 `DomainException`
- 유스케이스 조합 및 애플리케이션 정책 위반은 `ApplicationException`
- 외부 시스템 실패는 `InfrastructureException`

이 구조가 완성되면 예외 타입만 보아도 어느 계층에서 문제가 시작되었는지 이해하기 쉬워진다.

## staged 변경 기준에서 확인된 문제점

### 1. 새 베이스 예외가 아직 구조적으로 활용되지 않는다

현재 `GlobalExceptionHandler`는 `JgitkinsException` 하나만 받아 처리한다. 즉, `DomainException`이 생겨도 예외 타입 기반 분기가 아니라 여전히 `ErrorCode` 추론 기반이다.

결과적으로 지금 상태는 "예외 클래스는 분리되었지만 처리 전략은 아직 분리되지 않은 상태"다.

### 2. 서비스 계층에서 도메인 예외를 다시 평탄화한다

예를 들어 아래 흐름이 존재한다.

- `User` 도메인 모델이 `UserAlreadyActivatedException` 발생
- `UserProfileService`가 이를 잡고 `JgitkinsException(DomainErrorCode.USER_ALREADY_ACTIVATED, ...)`로 재포장
- `RunnerManagementService`도 동일하게 도메인 예외를 `JgitkinsException`으로 재포장

이 패턴은 도메인 예외 타입의 정보와 책임 경계를 다시 흐리게 만든다.

### 3. 도메인/인프라에 일반 런타임 예외가 많이 남아 있다

현재 코드베이스에는 아래 예외가 많이 남아 있다.

- `IllegalArgumentException`
- `IllegalStateException`
- `RuntimeException`

이 예외들은 내부 개발용 assertion에는 쓸 수 있지만, API 서버의 예외 표준으로는 의미가 약하다. 어떤 오류 코드로, 어떤 HTTP status로, 어떤 source로 응답해야 하는지 일관성이 떨어진다.

### 4. `UserAlreadyActivatedException`은 현재 staged 상태 그대로면 컴파일이 되지 않는다

`UserAlreadyActivatedException`은 현재 `DomainException`을 상속하지만 생성자에서 `super("User is already activated")`를 호출한다. 그러나 `DomainException`에는 `String`만 받는 생성자가 없다.

즉, staged 변경은 방향은 맞지만 아직 완성본은 아니다.

### 5. `source` 판정이 예외 타입이 아니라 에러 코드 타입에 묶여 있다

현재 `GlobalExceptionHandler#inferSource(...)`는 `ErrorCode` 구현체 타입으로 source를 추론한다. 이 구조는 아래 문제가 있다.

- 예외 계층이 생겨도 처리기에 반영되지 않는다.
- 같은 계층의 예외라도 다른 에러 코드를 넣으면 source가 흔들릴 수 있다.
- "어디서 발생했는가"와 "무슨 오류 코드인가"가 분리되지 않는다.

### 6. 응답 모델이 이중화되어 있다

~~현재는 `ApiResponse`/`ApiError`가 실제 응답으로 쓰이는데, presentation/exception/ErrorResponse.java 도 별도로 존재한다.~~ **해소됨 (2026-08-28 확인).** `ErrorResponse` 는 어느 모듈에도 존재하지 않는다. 응답 모델은 `ApiResponse`/`ApiError` 한 벌이며, 둘 다 `core-web` 모듈로 이동했다.

따라서 이 문서 기준 결론은 명확하다.

- `ApiResponse`/`ApiError`를 표준 응답 모델로 유지한다.
- `ErrorResponse`는 미사용 코드로 보고 삭제 대상으로 분류한다.

## 이상적인 예외처리 구조

이 프로젝트에 맞는 이상적인 구조는 아래와 같다.

### 1. 공통 루트는 유지하되, 계층별 예외를 1급 구조로 사용

권장 구조:

- `JgitkinsException` : 공통 추상 베이스
- `PresentationException`
- `ApplicationException`
- `DomainException`
- `InfrastructureException`

권장 원칙:

- Presentation 계층은 요청 파싱, 인증/인가 진입점, 입력 형식 오류를 담당
- Application 계층은 유스케이스 조합, 조회 실패, 접근 정책, 상태 전이 orchestration 실패를 담당
- Domain 계층은 순수 비즈니스 규칙 위반만 담당
- Infrastructure 계층은 DB, 파일시스템, JGit, 외부 라이브러리 실패만 담당

### 2. `GlobalExceptionHandler`는 예외 타입 기준으로 처리

현재처럼 `ErrorCode`로 source를 추론하는 대신, 아래처럼 예외 타입으로 책임을 분리하는 편이 더 명확하다.

- `@ExceptionHandler(PresentationException.class)`
- `@ExceptionHandler(ApplicationException.class)`
- `@ExceptionHandler(DomainException.class)`
- `@ExceptionHandler(InfrastructureException.class)`
- `@ExceptionHandler(JgitkinsException.class)` 는 최후 fallback

이렇게 하면 source 판정 로직이 자연스럽게 정리된다.

### 3. 도메인 예외는 서비스에서 재포장하지 않고 그대로 올린다

이상적인 흐름:

- 도메인 모델이 `DomainException` 하위 예외 발생
- 애플리케이션 서비스는 필요하면 로깅이나 보상 처리만 수행
- 예외 타입은 유지한 채 presentation advice까지 전달

즉, 아래 패턴은 줄이는 것이 좋다.

- `catch (DomainException ex) { throw new JgitkinsException(...); }`

대신 아래가 더 좋다.

- `catch` 자체를 제거
- 정말 필요한 경우 `throw new ApplicationException(..., ex)` 처럼 계층 의미가 바뀌는 경우에만 변환

### 4. 일반 런타임 예외를 점진적으로 제거

권장 규칙:

- 입력 유효성 자체가 도메인 규칙이면 `DomainException`
- 유스케이스 입력 조합 문제면 `ApplicationException`
- 외부 의존성 실패면 `InfrastructureException`
- 프레임워크 요청 바인딩 문제면 `PresentationException` 또는 Spring validation 예외

`IllegalArgumentException`은 내부 value object 생성자의 사전조건 방어 정도로는 허용할 수 있다. 하지만 API로 전파되는 경로에서는 계층형 예외로 흡수하는 것이 좋다.

### 5. 에러 코드는 "무엇이 잘못됐는지", 예외 타입은 "어디서 잘못됐는지"를 표현

이 원칙이 중요하다.

- 예외 타입: 발생 계층
- 에러 코드: 문제 의미
- HTTP status: 외부 계약
- message: 사용자/클라이언트 설명
- cause: 디버깅용 원인 보존

현재 구조는 이 중에서 예외 타입의 역할이 아직 약하다. staged 변경은 이 약한 부분을 보강하는 출발점이다.

## 권장 변경 명세

### A. 단기 명세

1. `UserAlreadyActivatedException` 생성자 오류를 바로 수정한다.
2. `DomainException`, `ApplicationException`, `InfrastructureException`의 불필요한 `getErrorCode()` override를 제거한다.
3. 불필요한 import를 정리한다.
4. 신규 예외 베이스 클래스에 맞춰 대표 서비스부터 치환한다.

우선순위 대상:

- `UserProfileService`
- `RunnerManagementService`
- `BranchCreationValidator`
- `ActivationValidator`
- `RepositoryValidator`
- 주요 JGit/MyBatis adapter

### B. 중기 명세

1. `GlobalExceptionHandler`를 예외 타입 기반 처리로 변경한다.
2. `inferSource(ErrorCode)` 제거 또는 fallback 전용으로 축소한다.
3. `PresentationException`을 추가해 presentation 계층도 대칭 구조를 맞춘다.
4. `ErrorHttpStatusMapper`는 예외가 아닌 `ErrorCode` 중심 매핑만 맡게 하고, source 결정은 handler가 담당한다.

### C. 장기 명세

1. `IllegalArgumentException` / `IllegalStateException` 발생 지점을 전수 점검한다.
2. 외부 API 계약 개편 시 Spring `ProblemDetail` 도입 여부를 검토한다.
3. 운영 로그와 모니터링 기준에 맞춰 예외 로그 레벨 정책을 정교화한다.

## 구체적인 이상 구조 예시

```text
Controller
  -> Application Service
    -> Domain Model / Domain Service
    -> Infrastructure Port Adapter

예외 발생 시:

PresentationException   -> 4xx
ApplicationException    -> 4xx or 5xx
DomainException         -> 4xx
InfrastructureException -> 5xx

GlobalExceptionHandler
  -> source는 예외 타입으로 결정
  -> status는 ErrorCode 매퍼로 결정
  -> body는 ApiResponse.failure(...)
```

## 개선 제안 3가지

### 제안 1. 예외 타입 기반 처리로 완성하기


---

## 부록: 모듈 경계 이동 (2026-08-28 정정)

이 문서가 인용하던 경로 중 세 개는 단순 경로 오류가 아니라 **모듈이 바뀐 것**이다.
경로만 고치면 같은 오해가 다시 생기므로 사실을 적어 둔다.

| 타입 | 이전 (문서 서술) | 현재 모듈 |
|---|---|---|
| `JgitkinsException` | `app-server` 내부 | **`core-common`** |
| `ApiResponse` | `app-server` 내부 | **`core-web`** |
| `ApiError` | `app-server` 내부 | **`core-web`** |

`app-server` 는 `io.jgitkins.core.web.api.response.ApiResponse` 를 import 해서 쓴다.
`app-runner` 와 `app-web` 은 각자 같은 이름의 클래스를 따로 갖고 있으므로, 클래스명만으로
어느 모듈을 가리키는지 판단하면 틀린다.