# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.2 application dto 단순 carrier record 전환 범위 확장 계획서

### 배경 (왜?)
- 현재 `server` 모듈의 application dto 계층에는 `@Getter`, `@Builder`, `@AllArgsConstructor`, `@Value` 중심의 단순 carrier DTO가 혼재되어 있으며, 동일 계층 안에 이미 `record`로 정리된 타입과 공존하고 있다.
- `runner` 모듈에도 `RunnerRuntimeConfigResult`, `RunnerExecutionConfigResult`, `RunnerActivateResult`, `JobRunContext`처럼 의미상 단순 운반 객체가 남아 있어 멀티모듈 기준이 일관되지 않다.
- `web` 모듈은 이미 대부분이 `record`로 정리되어 있으므로, 이번 작업에서 `web`은 주 전환 대상이 아니라 모듈 간 contract 호환성 기준선 역할을 수행한다.
- 현재 구조는 생성 방식이 `builder`, 생성자, `record`로 분산되어 있어 호출부 가독성, 테스트 fixture 작성 방식, Jackson 직렬화 규칙, accessor 사용 패턴이 모듈별로 달라진다.
- 의미 있는 생성 규칙이 없는 단순 carrier 타입까지 Lombok 기반 클래스로 유지하면 DTO 계층의 표현력이 흐려지고, 실제로는 데이터 구조인데 불필요한 보일러플레이트가 남는다.

### 목표 (Goals)
- `server`와 `runner`의 단순 carrier DTO를 선별해 `record`로 통일한다.
- 의미 있는 생성 규칙이나 mutable 상태가 있는 타입은 제외하여 과도한 일괄 변환을 방지한다.
- `builder/getter` 기반 호출부를 canonical constructor 및 `record accessor` 기반으로 정리한다.
- `web-server-runner` 간 DTO contract와 Jackson 직렬화 호환성을 명확히 검증한다.
- 후속 구현 시 변경 대상과 제외 대상을 문서 기준으로 고정하여 리팩토링 범위를 통제한다.

### 범위 (Scope)
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/dto`, `server/.../dto/command`, `server/.../dto/result`, `runner/src/main/java/io/jgitkins/runner/application/dto`를 대상으로 한다.
- **수정 대상**: `web/src/main/java/io/jgitkins/web/application/dto`는 직접 전환 대상이 아니라 호환성 검토, mapper 정렬, 테스트 기준선 검증 대상으로 본다.
- **수정 제외 대상**: 이미 `record`인 `RepositoryKey`, `RepositoryCreationContext`, `RunnerDispatchContext`, `PushHookRequest`, `PushJobPlanRequest`, `DispatchJobCommand`, `RepositoryCreateCommand`, `DispatchableJob`, `JobDispatchResult`, `JobCreationDecision`과 `web`의 기존 `record` DTO는 유지한다.
- **수정 제외 대상**: `BranchCreationContext`, `PushEventCommand`, `UpdateRepositoryCommand`, `JobPlan`, `OAuthLoginResult`, `PipelineConfig`, `PipelineRule`, `RunnerExecutionConfig`, `RunnerRuntimeConfig`, `MergeResult`, `FileUploadInfo`, `FileUploadRequest`, `CommitHistory`, `BranchInfo`는 정적 팩토리, setter, 도메인 객체 노출, 정책 캡슐화, 컬렉션 처리 규칙 여부를 재평가하기 전까지 제외한다.

### 방법 조사 및 선택
- **방안 1**: `server`와 `runner`의 application dto를 거의 전부 `record`로 일괄 전환하는 방식이다.
  장점은 규칙이 단순하다는 점이다.
  단점은 `UpdateRepositoryCommand`, `JobPlan`, `MergeResult`처럼 의미 있는 생성 규칙이나 mutable 상태가 있는 타입까지 무리하게 바꾸게 되어 회귀 위험이 크다는 점이다.
- **방안 2**: 단순 carrier만 선별 전환하고, 의미 있는 생성 규칙이 있는 타입은 제외 목록으로 문서화하는 방식이다.
  장점은 리팩토링 이득과 회귀 통제를 동시에 확보할 수 있다는 점이다.
  단점은 사전 분류 작업이 필요하다는 점이다.
- **방안 3**: 기존 클래스 구조를 유지하되 `builder` 사용만 제한하고 점진적으로 accessor 패턴만 정리하는 방식이다.
  장점은 코드 churn이 가장 적다는 점이다.
  단점은 DTO 표현 일관성이 확보되지 않고 기술 부채가 그대로 남는다는 점이다.
- **선택 방안**: 방안 2를 선택한다.
- **선택 이유**: `record` 전환의 목적은 보일러플레이트 제거보다 DTO 의미를 명확히 하는 데 있으므로, 단순 carrier 여부를 기준으로 선별 전환하는 편이 클린 아키텍처와 포트-어댑터 경계의 의도를 더 잘 보존한다.

### 변경 대상 BEFORE / AFTER 목록
- `server/.../command/BranchCreateCommand`는 Lombok class에서 `record BranchCreateCommand(...)`로 전환한다.
- `server/.../command/OrganizeCreationCommand`는 Lombok class에서 `record OrganizeCreationCommand(...)`로 전환한다.
- `server/.../command/OrganizeMemberAddCommand`는 Lombok class에서 `record OrganizeMemberAddCommand(...)`로 전환한다.
- `server/.../command/RepositoryMemberAddCommand`는 Lombok class에서 `record RepositoryMemberAddCommand(...)`로 전환한다.
- `server/.../command/RunnerRegisterCommand`는 Lombok class에서 `record RunnerRegisterCommand(...)`로 전환한다.
- `server/.../command/UpdateOrganizeCommand`는 Lombok class에서 `record UpdateOrganizeCommand(...)`로 전환한다.
- `server/.../command/UserCredentialIssueCommand`는 생성자 기반 class에서 `record UserCredentialIssueCommand(...)`로 전환한다.
- `server/.../command/OAuthLoginCommand`는 생성자 기반 class에서 `record OAuthLoginCommand(...)`로 전환한다.
- `server/.../command/JobCreateCommand`는 Lombok class에서 `record JobCreateCommand(...)`로 전환한다.
- `server/.../command/JobResultReportCommand`는 Lombok class에서 `record JobResultReportCommand(...)`로 전환한다.
- `server/.../command/UserLoginOrSignUpCommand`는 Lombok class에서 `record UserLoginOrSignUpCommand(...)`로 전환한다.
- `server/.../result/BranchSearchResult`는 mutable Lombok class에서 `record BranchSearchResult(...)`로 전환한다.
- `server/.../result/OrganizeCreationResult`는 Lombok class에서 `record OrganizeCreationResult(...)`로 전환한다.
- `server/.../result/OrganizeMemberSummary`는 생성자 기반 class에서 `record OrganizeMemberSummary(...)`로 전환한다.
- `server/.../result/RepositoryMemberSummary`는 생성자 기반 class에서 `record RepositoryMemberSummary(...)`로 전환한다.
- `server/.../result/RunnerDetailResult`는 Lombok class에서 `record RunnerDetailResult(...)`로 전환한다.
- `server/.../result/RunnerRegistrationResult`는 Lombok class에서 `record RunnerRegistrationResult(...)`로 전환한다.
- `server/.../result/RunnerActivateResult`는 Lombok class에서 `record RunnerActivateResult(...)`로 전환한다.
- `server/.../result/UserAdminSummary`는 생성자 기반 class에서 `record UserAdminSummary(...)`로 전환한다.
- `server/.../result/UserAdminDetail`는 생성자 기반 class에서 `record UserAdminDetail(...)`로 전환한다.
- `server/.../result/UserCredentialIssueResult`는 생성자 기반 class에서 `record UserCredentialIssueResult(...)`로 전환한다.
- `server/.../result/UserCredentialSummary`는 생성자 기반 class에서 `record UserCredentialSummary(...)`로 전환한다.
- `server/.../result/UserIdentitySummary`는 생성자 기반 class에서 `record UserIdentitySummary(...)`로 전환한다.
- `server/.../result/UserSummary`는 생성자 기반 class에서 `record UserSummary(...)`로 전환한다.
- `server/.../result/RepositoryResult`는 Lombok class에서 `record RepositoryResult(...)`로 전환한다.
- `server/.../result/RepositoryOverviewResult`는 Lombok class에서 `record RepositoryOverviewResult(...)`로 전환한다.
- `runner/.../RunnerRuntimeConfigResult`는 Lombok class에서 `record RunnerRuntimeConfigResult(...)`로 전환한다.
- `runner/.../RunnerExecutionConfigResult`는 Lombok class에서 `record RunnerExecutionConfigResult(...)`로 전환한다.
- `runner/.../RunnerActivateResult`는 `@Value @Builder` class에서 `record RunnerActivateResult(...)`로 전환한다.
- `runner/.../JobRunContext`는 `@Value @Builder` class에서 `record JobRunContext(...)`로 전환한다.
- `web` 모듈 DTO는 구조 변경 없이 server/runner 전환 이후 mapper, template, client contract 호환성만 점검한다.

### 계획 (Plan)
- **단계 1**: 분석 및 평가를 수행한다.
  `server`, `runner`, `web`의 application dto 사용처를 조회하여 단순 carrier와 제외 대상을 최종 확정한다.
- **단계 2**: 리팩토링 전략을 확정한다.
  builder 사용 제거 방식, constructor 호출 통일 방식, `record accessor` 전환 규칙, Jackson 검증 기준을 정리한다.
- **단계 3**: `server` 전환을 수행한다.
  command/result 계층의 단순 carrier를 `record`로 바꾸고 service, mapper, controller, test fixture의 생성 및 접근 코드를 함께 수정한다.
- **단계 4**: `runner` 전환을 수행한다.
  application dto의 단순 carrier를 `record`로 바꾸고 adapter, service, config loader, test fixture 호출부를 정리한다.
- **단계 5**: `web` 호환성 조정을 수행한다.
  server 응답 매핑, request 생성, 템플릿/뷰모델 조합이 `record accessor` 기준으로 계속 동작하는지 확인한다.
- **단계 6**: 테스트 및 검증을 수행한다.
  단위 테스트, Jackson 직렬화 테스트, 모듈 간 contract 검증, builder 제거에 따른 컴파일 회귀 검증을 수행한다.
- **단계 7**: 문서화를 수행한다.
  제외 목록, 변경 이유, 회귀 포인트, 후속 후보를 Task Master와 계획서에 동기화한다.

### 검증 기준
- `record` 전환 대상은 custom setter, mutable 상태, 복잡한 정적 팩토리, 컬렉션 방어 복사 요구가 없어야 한다.
- `record` 전환 후 public API의 JSON 필드명과 생성 필수값 계약이 기존과 동일해야 한다.
- builder 제거 후 테스트와 운영 코드가 모두 명시적 constructor 호출 또는 정적 팩토리로 읽히도록 정리되어야 한다.
- `web` 모듈의 request/response 매핑은 server DTO 변경과 무관하게 계약이 유지되어야 한다.
- `runner` 모듈의 실행 컨텍스트 DTO는 Docker 실행, config 로딩, activate 응답 흐름에서 null 처리와 필수값 의미가 더 명확해져야 한다.

### 개선 사항 점검
- **개선안 1**: 전환 대상별로 `record` 적합성 체크리스트를 추가한다.
- **개선안 2**: Jackson 직렬화 회귀를 DTO 단위 parameterized test로 공통화한다.
- **개선안 3**: 모듈별 전환 순서를 `server -> runner -> web 검증`으로 고정해 PR 리뷰 범위를 줄인다.
- **선택 개선안**: 개선안 1과 3을 즉시 반영한다.
- **반영 방식**: 본 계획서에 제외 기준과 단계별 순서를 명시하고, 구현 시 동일 순서를 유지한다.

### 기대효과 (Expected Benefits)
- DTO 계층의 표현 방식이 `record` 중심으로 정리되어 읽기 비용이 감소한다.
- Lombok 의존 보일러플레이트가 줄어들어 생성자 계약과 필드 구성이 더 직접적으로 드러난다.
- `server`와 `runner`의 application dto 규칙이 맞춰져 멀티모듈 유지보수성이 높아진다.
- 제외 기준을 함께 문서화하므로 과도한 일괄 변환을 피하고 회귀 위험을 줄일 수 있다.
- `web`을 기준선으로 둔 contract 검증이 가능해져 모듈 간 DTO 변경의 안전성이 높아진다.

### 예시 (방안 2 기준 코드 스니펫)

#### AS-IS (현재 구조)
```java
@Getter
@Builder
public class RunnerExecutionConfigResult {

    private final String runnerImageName;
    private final String jenkinsPluginConfig;
}
```

```java
RunnerExecutionConfigResult result = RunnerExecutionConfigResult.builder()
        .runnerImageName(imageName)
        .jenkinsPluginConfig(pluginConfig)
        .build();
String image = result.getRunnerImageName();
```

#### TO-BE (개선 제안 구조)
```java
public record RunnerExecutionConfigResult(
        String runnerImageName,
        String jenkinsPluginConfig
) {
}
```

```java
RunnerExecutionConfigResult result = new RunnerExecutionConfigResult(imageName, pluginConfig);
String image = result.runnerImageName();
```

### 주의사항
- **포맷팅 금지**: 리팩토링 과정에서 코드 포맷팅은 수행하지 않는다.
- **기존 기능 보장**: 리팩토링 후에도 기존 기능이 정상적으로 동작하는지 테스트로 검증한다.
- **계획우선**: 계획 문서 작성 단계에서는 구현을 진행하지 않는다.
- **예시전체나열**: 변경하려는 목록의 BEFORE / AFTER를 계획서에 모두 명시한다.
- **문서체규약**: 모든 문장은 공식 문서체로 유지한다.

### 결론
- Task 2.2는 `web, server` 범위만으로 정의하기보다 `runner`를 포함한 멀티모듈 DTO 규칙 정렬 작업으로 다루는 편이 타당하다.
- 구현은 방안 2의 선별 전환 전략을 기준으로 진행하며, `web`은 직접 전환보다 호환성 검증 축으로 활용한다.
