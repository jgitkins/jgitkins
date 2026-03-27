# 리팩토링 계획서

### 제목
- **리팩토링 계획**: (리팩토링 대상 시스템 또는 모듈명)

### 배경 (왜?)
- 현재 시스템에서 문제가 되는 부분이나 비효율적인 코드, 성능 저하 원인 등을 설명합니다.
- 리팩토링이 필요한 이유와 이를 통해 해결하고자 하는 문제를 구체적으로 서술합니다.
- 리팩토링으로 해결하고자 하는 주요 문제(예: 코드 유지보수성, 성능, 코드 중복, 기술 부채 등)를 설명합니다.

### 목표 (Goals)
- 리팩토링을 통해 달성하고자 하는 목표를 설정합니다. 예를 들어:
    - 코드 가독성 향상
    - 시스템 성능 개선
    - 테스트 용이성 증대
    - 모듈화 및 재사용성 향상

### 범위 (Scope)
- 리팩토링할 대상 범위와 시스템의 어느 부분이 수정될 것인지 명확하게 정의합니다.
    - **수정 대상**: (예: 모듈, 클래스, 함수 등)
    - **수정 제외 대상**: 리팩토링하지 않는 부분이나 제외되는 항목을 명시

### 계획 (Plan)
- 리팩토링을 위한 단계별 계획을 설명합니다. 각 단계에서 무엇을 할지 구체적으로 나열합니다.
    - **단계 1**: 분석 및 평가 (현재 코드 상태 점검, 문제 식별)
    - **단계 2**: 리팩토링 전략 수립 (어떤 리팩토링 기법을 사용할지 정의)
    - **단계 3**: 리팩토링 작업 수행 (구체적인 리팩토링 진행)
    - **단계 4**: 테스트 및 검증 (리팩토링 후 시스템이 예상대로 동작하는지 확인)
    - **단계 5**: 문서화 (변경 사항 및 리팩토링 과정 기록)

### 기대효과 (Expected Benefits)
- 리팩토링 후 예상되는 효과를 명확히 합니다.
    - 예: 코드 가독성 향상, 성능 최적화, 버그 수정, 유지보수 용이성 증가


### 예시 (방안 2 기준 코드 스니펫)

#### AS-IS (현재 구조)
```java
public void processGitLabUserRegistration() {
    // ... 시작 마킹
    RegistrationResult result = executeRegistration(aggregate);
    // ... 결과 저장
    if (completion.requestCompleted()) {
        notifyCompletion(aggregate); // 강한 결합: 서비스 내에서 직접 로직 수행
    }
}

// 중복된 내부 선언 구조들
private String trimReason(String reason) { /* ... */ }
private record RegistrationResult(boolean success, String reason) { /* ... */ }
```

#### TO-BE (개선 제안 구조)
```java
public void processGitLabUserRegistration() {
    // 1. 공통 Result 형식 사용 (Domain Shared 계층 등에 위치)
    // 2. Event Publisher를 통한 부가 로직 분리
    
    // 연동 진행
    ExecutionResult result = executeRegistration(aggregate);
    
    // 완료 처리
    GitLabUserRegistrationCompletion completion = completionService.complete(aggregate, SCHEDULER_USER);
    if (completion.requestCompleted()) {
        // 알림이라는 관심사를 분리하여 Event Listener가 처리하도록 함 (SRP 보장)
        eventPublisher.publishEvent(new GitLabUserRegistrationCompletedEvent(aggregate));
    }
}

// 헬퍼 또는 정책 객체로 캡슐화된 예외 처리 로직 (trimReason 제거 가능)
String reason = failureReasonPolicy.mapAndTrim(e);
```

### 주의사항
- **포맷팅 금지**: 리팩토링 과정에서 코드 포맷팅 절대하지말것. 주로 코드의 기능과 구조를 개선하는 데 집중
- **기존 기능 보장**: 리팩토링 후에도 기존의 기능이 정상적으로 동작하는지 확인하는 테스트가 필요
- **계획우선**: 계획문서 작성중에 절대로 구현을 진행하지말것
- **예시전체나열**: 변경하려는 목록의 BEFORE AFTER를 모두 나열할것 (부분적 X)
- **문서체규약**:
    - 문서 작성 시, 모든 문장은 **"~~하였음"** 또는 **"~~함"** 형태로 마무리하여 공식적인 느낌을 유지합니다.
    - 구어체 표현은 피하고, 전문적이고 격식 있는 문어체를 사용합니다.
    - 문장을 간결하게 작성하되, 중요한 정보는 빠짐없이 포함시킵니다.
    - 문서의 끝맺음은 항상 "완료하였음", "수립하였음", "작성함" 등의 형식으로 마무리합니다. 
### 결론 (추후작성)
