# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.3 계층별 ErrorCode 타입 안정성 강화 및 ProblemSpec 도입 정리 계획서다.

### 기준 커밋
- 기준 커밋은 `0d1750b88e355404c285abc0680034728bf504a7`(`working 2.3`)다.

### 배경 (왜?)
- 기존 구조는 계층별 `ErrorCode` enum을 분리해 두었지만, `JgitkinsException` 및 계층 예외 생성자가 공통 `ErrorCode`를 받아 계층 혼용을 컴파일 단계에서 막지 못했다.
- 기준 커밋은 이 문제를 해결하면서, 세부 예외 의미를 `ProblemSpec`으로 분리해 외부 응답 코드와 내부 분류 코드를 함께 다루는 구조로 확장했다.
- 따라서 이번 Task 문서는 단순 타입 제한뿐 아니라 `ProblemSpec`, 세부 custom exception, 응답 코드 정책까지 함께 설명해야 한다.

### 목표 (Goals)
- 계층 예외가 자기 계층 `ErrorCode`만 받도록 제한한다.
- `ProblemSpec`으로 세부 오류 식별자, 기본 메시지, 메시지 키를 관리한다.
- application 계층의 중복 `ErrorCode`를 `NOT_FOUND`, `ALREADY_EXISTS`, `ACCESS_DENIED`, `UNPROCESSABLE` 중심으로 축소한다.
- `GlobalExceptionHandler`가 `ProblemSpec` 우선 응답 코드를 사용하도록 정리한다.
- 배포 전 외부 API 계약 변경 여부와 호환성 리스크를 명확히 판단한다.

### 범위 (Scope)
- **수정 대상**: `server/common/error`, `server/common/exception`, `server/common/problem`.
- **수정 대상**: `server/application/domain/infrastructure/presentation` 계층의 예외 클래스와 오류 코드 정의.
- **수정 대상**: `server/presentation/advice/GlobalExceptionHandler.java`, `server/presentation/advice/mapper/*`.
- **수정 대상**: `server/presentation/common/ApiError.java`, `server/presentation/common/ApiResponse.java`.
- **수정 대상**: 예외 throw 지점과 관련 회귀 테스트.
- **수정 제외 대상**: `runner` 모듈의 독립 예외 체계 변경은 제외한다.
- **수정 제외 대상**: `ApiResponse` JSON 필드 구조 변경은 제외한다.

### 방법 조사 및 선택
- **방안 1**: 계층별 `ErrorCode`만 정리하고 외부 응답도 기존 enum code를 그대로 유지한다.
- **방안 2**: 계층별 `ErrorCode`는 단순화하고, 외부 응답은 `ProblemSpec`의 세부 code를 사용한다.
- **방안 3**: `ErrorCode`를 단일 enum으로 통합하고 계층 정보는 예외 타입만으로 관리한다.
- **선택 방안**: 방안 2를 선택한다.
- **선택 이유**: 계층 경계는 타입으로 강제하고, 외부 응답은 세부 식별자를 유지해 관측성과 문제 추적성을 확보하기에 가장 현실적이기 때문이다.

### 현재 반영 내용
- `JgitkinsException`이 `ErrorCode`와 `ProblemSpec`을 함께 보관한다.
- `ApplicationException`, `DomainException`, `InfrastructureException`, `PresentationException`이 자기 계층 타입만 받도록 변경되었다.
- `ApplicationProblemSpec`, `DomainProblemSpec`, `InfrastructureProblemSpec`, `PresentationProblemSpec`이 추가되었다.
- application 계층의 세부 `ErrorCode` 다수가 공통 분류 코드로 축소되고, 세부 의미는 custom exception과 `ProblemSpec`으로 이동되었다.
- `GlobalExceptionHandler`는 `exception.getProblemCode()`를 우선 사용해 응답 `error.code`를 생성한다.
- `ApiResponse`와 `ApiError`는 문자열 code를 직접 받는 경로를 추가해 `ProblemSpec` 응답을 지원한다.

### 핵심 설계
- 내부 분류 축은 계층별 `ErrorCode`다.
- 외부 응답 식별 축은 `ProblemSpec.code`다.
- HTTP status 결정은 여전히 계층별 `ErrorCode` mapper가 담당한다.
- custom exception은 세부 의미를 담고, `ProblemSpec`은 외부 계약과 기본 메시지 기준을 담는다.

### 응답 계약 정리
- `error.source`와 HTTP status는 계층 mapper 기준을 유지한다.
- `error.code`는 예외가 `ProblemSpec`을 가지면 enum code가 아니라 `ProblemSpec.code`를 반환한다.
- 예시로 application not found 응답은 `NOT_FOUND` 대신 `REPO-404`, `USER-404` 같은 세부 code가 될 수 있다.
- 예외 메시지는 현재 구현상 `exception.getMessage()`를 우선 노출하므로, 내부 상세 메시지가 그대로 응답에 포함될 수 있다.

### 배포 판단
- **판단**: 기준 커밋 상태 그대로의 즉시 배포는 권장하지 않는다.
- **이유 0**: `:server:test` 단계에서 `DomainErrorCode.USER_ALREADY_ACTIVATED` 잔존 참조로 테스트 컴파일이 실패하므로, 현재 브랜치는 기본 검증선도 통과하지 못한 상태다.
- **이유 1**: `GlobalExceptionHandler` 경로는 `ProblemSpec.code`를 응답하지만, `ApiAccessDeniedHandler`, `ApiAnauthorizeHandler`는 여전히 `ApplicationErrorCode.ACCESS_DENIED`, `PresentationErrorCode.UNAUTHORIZED`를 직접 응답해 동일 성격의 오류 코드 체계가 일관되지 않다.
- **이유 2**: 테스트 기대값도 `NOT_FOUND`, `ACCESS_DENIED`, `UNAUTHORIZED`, `REPOSITORY_NOT_FOUND`가 혼재되어 있어 외부 계약이 최종 확정되지 않았다.
- **이유 3**: `error.message`가 개발자용 상세 메시지를 그대로 노출할 수 있어, 사용자 메시지 정책이 아직 배포 기준으로 정리되지 않았다.
- **이유 4**: `web` 모듈은 현재 주로 `error.message`만 사용하지만, 외부 클라이언트나 테스트가 기존 `error.code` 문자열에 의존한다면 회귀가 발생할 수 있다.

### 배포 전 필수 정리
- `error.code`의 최종 계약을 `enum code 유지` 또는 `ProblemSpec code 전환` 중 하나로 확정한다.
- Spring Security 예외 핸들러도 `ProblemSpec` 기준으로 통일하거나, 반대로 전체 응답을 enum code로 되돌린다.
- 대표 API 테스트를 `GlobalExceptionHandler`, security handler, controller slice 기준으로 다시 맞춘다.
- `error.message` 노출 정책을 사용자 메시지와 내부 로그 메시지로 구분할지 결정한다.

### 검증 기준
- 계층 혼용 `ErrorCode` 전달이 컴파일 단계에서 차단되어야 한다.
- application/domain/infrastructure/presentation 예외 응답의 `status`, `source`, `code`가 문서와 동일해야 한다.
- security handler와 `GlobalExceptionHandler`의 오류 코드 형식이 일치해야 한다.
- 기존 소비자가 사용하는 주요 오류 응답 계약이 깨지지 않거나, 깨진다면 마이그레이션 공지가 가능해야 한다.

### 개선 사항 점검
- **개선안 1**: security handler를 `ProblemSpec` 기반 응답으로 통일한다.
- **개선안 2**: `error.message`를 사용자 메시지와 로그 메시지로 분리한다.
- **개선안 3**: 응답 계약 테스트를 `error.code` 중심으로 명시한다.
- **선택 개선안**: 개선안 1, 2를 우선 반영한다.

### 기대효과 (Expected Benefits)
- 계층 예외와 계층 오류 코드의 소속이 타입 수준에서 고정된다.
- 세부 예외 의미를 유지하면서 공통 상태 분류 코드를 단순화할 수 있다.
- 관측성과 문제 추적을 위한 세부 응답 코드를 제공할 수 있다.
- 다만 외부 계약을 명확히 고정하지 않으면 배포 리스크가 커지므로, 응답 코드 정책 통일이 선행되어야 한다.

### 주의사항
- 이번 Task는 내부 구조 개선과 외부 오류 계약 조정이 함께 묶여 있으므로, 단순 리팩토링 커밋으로 간주하면 안 된다.
- 후속 커밋 전에는 테스트 기대값과 보안 핸들러 응답을 반드시 함께 확인해야 한다.
