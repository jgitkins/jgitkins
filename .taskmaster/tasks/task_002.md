# Task ID: 2

**Title:** 리팩토링

**Status:** pending

**Dependencies:** None

**Priority:** high

**Description:** 구조 개선, 품질 개선, 테스트 체계 강화 작업

**Details:**

기존 기능별 Task를 카테고리 기반(신규기능/리팩토링/보안)으로 재구성함.

**Test Strategy:**

카테고리별 우선순위에 따라 하위 작업을 순차 수행하고 회귀 테스트를 적용한다.

## Subtasks

### 2.1. [web, server] Inbound/Outbound Adapter 책임 재배치

**Status:** pending  
**Dependencies:** None  

요청 파싱/인증 컨텍스트/외부 I/O 책임이 섞인 지점을 어댑터 경계 기준으로 분리한다.

**Details:**

[source: jgitkins-server, original subtask: 2.19]

### 2.2. [web, server] application dto 단순 carrier record 전환

**Status:** pending  
**Dependencies:** None  

src/main/java/io/jgitkins/server/application/dto 이하의 단순 carrier DTO를 선별해 Java record로 전환하고, builder/getter 중심 호출부와 테스트를 함께 정리한다.

**Details:**

[source: jgitkins-server, original subtask: 2.41]
대상 범위는 src/main/java/io/jgitkins/server/application/dto, command, result 패키지이며, 이미 record인 RepositoryKey, RepositoryCreationContext, RunnerDispatchContext, PushHookRequest, PushJobPlanRequest, DispatchJobCommand, RepositoryCreateCommand, DispatchableJob, JobDispatchResult, JobCreationDecision 등은 유지한다. 후속 전환 대상은 단순 데이터 운반 역할만 하는 command/result/summary/request 객체를 우선 검토하며, 예를 들어 BranchCreateCommand, OrganizeCreationCommand, OrganizeMemberAddCommand, RepositoryMemberAddCommand, RunnerRegisterCommand, UpdateOrganizeCommand, UpdateRepositoryCommand, UserCredentialIssueCommand, OAuthLoginCommand, JobCreateCommand, JobResultReportCommand, BranchSearchResult, OrganizeCreationResult, OrganizeMemberSummary, RepositoryMemberSummary, RunnerDetailResult, RunnerRegistrationResult, RunnerActivateResult, UserAdminSummary, UserAdminDetail, UserCredentialIssueResult, UserCredentialSummary, UserIdentitySummary, UserSummary 등이 후보가 된다. 단, PipelineConfig, PipelineRule, RunnerExecutionConfig, RunnerRuntimeConfig, MergeResult, FileUploadInfo, FileUploadRequest, CommitHistory, BranchInfo처럼 컬렉션 방어 복사, 정적 팩토리, 기본값 정책, 커스텀 메서드나 의미 있는 생성 규칙이 있는 타입은 단순 carrier 여부를 먼저 검토하고 필요 시 범위에서 제외한다. 변경 시 Lombok Builder/Getter 제거, mapper/service/controller/test 호출부의 accessor 및 생성 방식 정리, JSON 직렬화와 회귀 테스트 범위 정의를 포함한다.

### 2.3. [server] ErrorCode 단순화 및 세부 Exception 클래스 설계 검토

**Status:** pending  
**Dependencies:** None  

ErrorCode를 HTTP status 중심의 단순 코드로 축소할 수 있는지 검토하고, Application 예외를 UserNotFoundException 같은 구체 타입 중심으로 상세화하는 방향의 설계/적용 기준을 정리한다.

**Details:**

[source: jgitkins-server, original subtask: 2.42]
이 서브태스크는 즉시 구현 확정이 아니라 사전 검토와 적용 기준 수립을 목표로 한다. 검토 주제 1: ErrorCode는 status 의미에 맞는 단순 코드만 유지하는 방향을 평가한다. 예를 들어 다수의 세부 NOT_FOUND 계열 코드를 개별 enum으로 늘리는 대신 NOT_FOUND, BAD_REQUEST, CONFLICT, FORBIDDEN, UNAUTHORIZED, INTERNAL_SERVER_ERROR 같은 status 중심 코드로 축소했을 때의 장단점을 분석한다. 검토 주제 2: 반대로 예외 클래스는 더 구체적으로 세분화한다. 예를 들어 Application/Domain 계층에서 UserNotFoundException, RepositoryNotFoundException, OrganizeNotFoundException, RunnerNotFoundException 같은 상세 구현체가 각자 내부적으로 ErrorCode.NOT_FOUND 를 고정 보유하는 모델을 검토한다. 함께 확인할 사항은 다음과 같다. 1) 현재 ErrorCode, JgitkinsException, DomainException, ApplicationException, InfrastructureException, GlobalExceptionHandler 구조와의 정합성. 2) ErrorCode 축소 시 외부 응답 메시지, 로깅, 관측성, 프론트엔드 분기 처리에 미치는 영향. 3) 세부 Exception 클래스 증가가 가독성/유지보수성/테스트성에 주는 이점과 비용. 4) 어떤 계층까지 세부 Exception 을 허용할지, 도메인/애플리케이션/인프라에서의 책임 경계. 5) 최종적으로 진행 여부를 너와 검토 후 결정할 수 있도록 적용안 A/B와 마이그레이션 전략을 문서화한다.

### 2.4. [web, server, runner] 멀티모듈 로깅 Java Configuration 표준화 준비

**Status:** pending  
**Dependencies:** None  

server의 2026-03-09 커밋 fc0412a79891f15d326823cf6a58963da7f1dc31을 기준선으로 삼아 runner/web 모듈에도 확장 가능한 LoggingConfigurator 전환 계획과 적용 기준을 수립한다.

**Details:**

[source: jgitkins-server, original subtask: 2.43]
참조 문서: .taskmaster/docs/refactor/task_2_43_plan.md. 1) server의 LoggingConfigurator, spring.factories 등록, XML 제거 방식을 기준선으로 삼는다. 2) runner/web의 기존 logback-spring.xml 차이를 비교하여 root level, 공통 logger preset, 로그 파일 경로/파일명, traceId 패턴, SOPS EnvironmentPostProcessor 공존 조건을 정리한다. 3) 공통 모듈 추출 여부는 비교 검토하되 1차 적용은 모듈별 configurator 도입과 XML 제거를 우선 검토한다. 4) local/non-local 프로파일별 console/file appender, 비동기 appender, spring.factories 등록, 회귀 검증 기준을 문서화한다.

### 2.5. [web] RestClient 예외 처리 횡단 관심사 중앙화 및 예외 변환 적용

**Status:** pending  
**Dependencies:** None  

**Details:**

[source: jgitkins-web, original subtask: 2.5]

### 2.6. [server] Global Exception Handler (@ControllerAdvice) 및 API 규격 표준화 적용

**Status:** pending  
**Dependencies:** None  

**Details:**

[source: jgitkins-web, original subtask: 2.6]

### 2.7. Outbound Port 설계의 객체지향적 세분화 (단일 책임, 인터페이스 분리 원칙 위배 해소)

**Status:** pending  
**Dependencies:** None  

**Details:**

[source: jgitkins-web, original subtask: 2.7]
