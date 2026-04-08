# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.5 `web` RestClient 예외 처리 중앙화 및 예외 변환 정리 계획서다.

### 배경 (왜?)
- 현재 `web` 모듈의 `JGitkinsServerClient`는 대부분의 메서드마다 `try/catch`, `response == null`, `response.error() != null`, 응답 바디 메시지 파싱 로직을 반복한다.
- 이로 인해 조회 메서드와 명령 메서드의 실패 처리 규칙이 일관되지 않고, `RestClientException`과 `RestClientResponseException`의 의미도 호출부마다 다르게 해석된다.
- 예외 처리와 응답 해석은 횡단 관심사이므로, 클라이언트 내부에서 공통 정책으로 모아야 유지보수와 테스트가 쉬워진다.

### 목표 (Goals)
- `JGitkinsServerClient`의 RestClient 예외 처리 중복을 제거한다.
- 원격 API 오류, 빈 응답, 연결 실패를 공통 정책으로 해석한다.
- `web` 모듈에서 사용할 예외 변환 또는 공통 결과 변환 경로를 정리한다.
- 상위 계층이 일관된 방식으로 실패를 처리할 수 있도록 한다.

### 범위 (Scope)
- **수정 대상**: `web/src/main/java/io/jgitkins/web/infrastructure/client/JGitkinsServerClient.java`
- **수정 대상**: `web/src/main/java/io/jgitkins/web/infrastructure/client` 하위의 예외 변환용 support 또는 exception 클래스가 필요하면 추가한다.
- **수정 대상**: `web`의 controller/service 중 `JGitkinsServerClient` 예외 처리 정책에 직접 영향을 받는 호출부를 함께 정리한다.
- **수정 제외 대상**: `server` 모듈의 API 응답 스키마 자체는 이번 Task 범위에서 변경하지 않는다.
- **수정 제외 대상**: `RestClient` Bean 설정 전체 재설계는 이번 Task 범위에서 제외한다.

### 계획 (Plan)
- **단계 1**: `JGitkinsServerClient` 메서드를 조회형, 결과 반환형 명령, 예외 전파형 명령으로 분류한다.
- **단계 2**: 응답 성공, 빈 응답, API 오류 응답, 연결 실패에 대한 공통 처리 규칙을 정의한다.
- **단계 3**: `execute`, `executeList`, `executeRequired`, `resolveFailureMessage` 같은 내부 helper 또는 별도 support 클래스를 도입한다.
- **단계 4**: 각 API 호출 메서드를 공통 helper 기반으로 치환하고, 필요하면 `web` 전용 예외로 변환한다.
- **단계 5**: 관련 테스트를 추가 또는 수정하여 실패 처리 계약을 고정한다.

### 기대효과 (Expected Benefits)
- `JGitkinsServerClient`의 중복 분기와 예외 처리 코드가 크게 줄어든다.
- API 오류와 연결 실패의 처리 기준이 일관되어 상위 계층의 예측 가능성이 높아진다.
- 향후 `server` 응답 정책이 바뀌더라도 `web`에서 수정해야 할 지점이 줄어든다.
- 테스트가 실패 시나리오를 공통 규칙으로 검증할 수 있어 회귀 위험이 낮아진다.

### 예시 (방안 2 기준 코드 스니펫)

#### AS-IS (현재 구조)
```java
public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
    try {
        ApiResponse<OrganizeSummary> response = restClient.post()
                .uri("/api/organizes")
                .body(request)
                .retrieve()
                .body(ORGANIZE_CREATE_TYPE);
        if (response == null) {
            return new OrganizeCreateResult(null, MESSAGE_EMPTY_RESPONSE);
        }
        if (response.error() != null) {
            return new OrganizeCreateResult(null, resolveApiErrorMessage(response));
        }
        if (response.data() == null) {
            return new OrganizeCreateResult(null, "조직 생성 응답이 비어 있습니다.");
        }
        return new OrganizeCreateResult(response.data(), null);
    } catch (RestClientResponseException ex) {
        return new OrganizeCreateResult(null,
                resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
    } catch (RestClientException ex) {
        return new OrganizeCreateResult(null, MESSAGE_SERVER_UNREACHABLE);
    }
}
```

#### TO-BE (개선 제안 구조)
```java
public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
    return executeResult(
            () -> restClient.post()
                    .uri("/api/organizes")
                    .body(request)
                    .retrieve()
                    .body(ORGANIZE_CREATE_TYPE),
            response -> new OrganizeCreateResult(requireData(response, "조직 생성 응답이 비어 있습니다."), null),
            message -> new OrganizeCreateResult(null, message)
    );
}

private <T, R> R executeResult(
        Supplier<ApiResponse<T>> call,
        Function<ApiResponse<T>, R> successMapper,
        Function<String, R> failureMapper) {
    try {
        ApiResponse<T> response = call.get();
        if (response == null) {
            return failureMapper.apply(MESSAGE_EMPTY_RESPONSE);
        }
        if (response.error() != null) {
            return failureMapper.apply(resolveApiErrorMessage(response));
        }
        return successMapper.apply(response);
    } catch (RestClientResponseException ex) {
        return failureMapper.apply(resolveErrorMessage(ex.getResponseBodyAsString(), MESSAGE_REQUEST_FAILED));
    } catch (RestClientException ex) {
        return failureMapper.apply(MESSAGE_SERVER_UNREACHABLE);
    }
}
```

### 주의사항
- **포맷팅 금지**: 리팩토링 과정에서 코드 포맷팅만을 목적으로 수정하지 않는다.
- **기존 기능 보장**: 기존 화면 흐름에서 실패 메시지와 fallback 동작이 바뀌지 않도록 테스트로 확인한다.
- **계획우선**: 계획 문서 작성 단계에서는 구현을 진행하지 않는다.
- **예시전체나열**: 반복 구조를 제거하는 대표 메서드 기준으로 AS-IS, TO-BE를 명확히 제시한다.

### 결론 (추후작성)
