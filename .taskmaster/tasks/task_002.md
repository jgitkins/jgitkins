# Task ID: 2

**Title:** 리팩토링

**Status:** in-progress

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

### 2.2. [web, server, runner] application dto 단순 carrier record 전환

**Status:** done
**Dependencies:** None  

server/runner application dto 이하의 단순 carrier DTO를 선별해 Java record로 전환하고, web 호환성 검토와 builder/getter 중심 호출부 및 테스트 정리를 함께 수행한다.

**Details:**

[source: jgitkins-server, original subtask: 2.41]
참조 문서: .taskmaster/docs/refactor/task_2_2_plan.md. 대상 범위는 server의 src/main/java/io/jgitkins/server/application/dto, command, result 패키지와 runner의 src/main/java/io/jgitkins/runner/application/dto 패키지다. web 모듈은 현재 application dto 대부분이 이미 record이므로 직접 전환 대상보다는 server/runner 변경 이후 mapper/controller/view model 호환성 검토와 회귀 검증 범위에 포함한다. 유지 대상은 이미 record인 RepositoryKey, RepositoryCreationContext, RunnerDispatchContext, PushHookRequest, PushJobPlanRequest, DispatchJobCommand, RepositoryCreateCommand, DispatchableJob, JobDispatchResult, JobCreationDecision 등이며, web의 기존 record DTO도 동일하게 유지한다. 우선 검토 후보는 server 쪽 BranchCreateCommand, OrganizeCreationCommand, OrganizeMemberAddCommand, RepositoryMemberAddCommand, RunnerRegisterCommand, UpdateOrganizeCommand, UserCredentialIssueCommand, OAuthLoginCommand, JobCreateCommand, JobResultReportCommand, UserLoginOrSignUpCommand, BranchSearchResult, OrganizeCreationResult, OrganizeMemberSummary, RepositoryMemberSummary, RunnerDetailResult, RunnerRegistrationResult, RunnerActivateResult, UserAdminSummary, UserAdminDetail, UserCredentialIssueResult, UserCredentialSummary, UserIdentitySummary, UserSummary, RepositoryResult, RepositoryOverviewResult 등과 runner 쪽 RunnerRuntimeConfigResult, RunnerExecutionConfigResult, RunnerActivateResult, JobRunContext 등이다. 단, BranchCreationContext, PushEventCommand, UpdateRepositoryCommand, JobPlan, OAuthLoginResult, PipelineConfig, PipelineRule, RunnerExecutionConfig, RunnerRuntimeConfig, MergeResult, FileUploadInfo, FileUploadRequest, CommitHistory, BranchInfo처럼 정적 팩토리, setter, 도메인 객체 노출, 컬렉션 방어 복사, 기본값 정책, 커스텀 메서드나 의미 있는 생성 규칙이 있는 타입은 단순 carrier 여부를 먼저 검토하고 필요 시 범위에서 제외한다. 변경 시 Lombok Builder/Getter 제거, mapper/service/controller/runner adapter/test 호출부의 accessor 및 생성 방식 정리, Jackson 직렬화와 모듈 간 회귀 테스트 범위 정의를 포함한다.

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

### 2.9. [server] Repository 생성 프로비저닝 이벤트 제거 및 UseCase 오케스트레이션 정리

**Status:** pending  
**Dependencies:** None  

Repository 생성 후처리에서 `RepositoryProvisionedEventListener` 기반 흐름을 제거하고, `application.support` 패키지의 `RepositoryProvisioner`로 프로비저닝 로직을 이동한 뒤 `RepositoryCreateUseCase` 구현체가 이를 직접 호출하도록 정리한다.

**Details:**

[source: jgitkins-server, original subtask: custom]
참조 문서: .taskmaster/docs/refactor/task_2_9_plan.md. 검토 범위는 `server/src/main/java/io/jgitkins/server/application/event/RepositoryProvisionedEventListener.java`, `server/src/main/java/io/jgitkins/server/domain/event/RepositoryProvisionedEvent.java`, `server/src/main/java/io/jgitkins/server/domain/aggregate/Repository.java`, `server/src/main/java/io/jgitkins/server/application/service/RepositoryLifecycleService.java`, `server/src/main/java/io/jgitkins/server/application/support` 패키지다. 목표는 `RepositoryProvisionedEventListener`가 담당하던 기본 브랜치 생성, 초기 커밋, HEAD 갱신, 초기화 상태 반영 절차를 `RepositoryProvisioner`로 이동하고, `RepositoryLifecycleService.create()`가 저장소 저장 및 git 초기화 직후 이를 직접 오케스트레이션하도록 정리하는 것이다. 이 과정에서 `Repository.create()`의 `RepositoryProvisionedEvent` 등록은 제거 대상으로 검토하며, `Presentation` 계층은 기존처럼 단일 `RepositoryCreateUseCase` 호출만 유지한다. `DomainEventPublisher`와 다른 도메인 이벤트 사용처는 이번 범위에서 제거하지 않으며, 해당 인터페이스와 구현체의 잔존 여부는 다른 aggregate 사용처를 확인한 뒤 결정한다.

### 2.10. [server] RepositoryLifecycleService 분리 및 Load/Management 책임 재구성

**Status:** pending  
**Dependencies:** None  

`RepositoryLifecycleService`를 `RepositoryManagementService`와 `RepositoryLoadService`로 분리하고, 조회 전용 메서드를 별도 Load Service로 이관하며, `RepositoryLoadUseCase` 및 연관 호출부의 메서드 명명 규칙을 `load*` 계열로 정리할지 검토 후 반영한다.

**Details:**

[source: jgitkins-server, original subtask: custom]
참조 문서: .taskmaster/docs/refactor/task_2_10_plan.md. 검토 범위는 `server/src/main/java/io/jgitkins/server/application/service/RepositoryLifecycleService.java`, `server/src/main/java/io/jgitkins/server/application/port/in/RepositoryLoadUseCase.java`, `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryManagementController.java`, `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryContentController.java`, `server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java`, `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`, `server/src/test/java/io/jgitkins/server/application/service/RepositoryLifecycleServiceTest.java`, `server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java` 및 `RepositoryLoadUseCase` 호출 테스트 전반이다. 목표는 생성/삭제 오케스트레이션과 조회 책임을 분리하여 서비스 응집도를 높이고, Load 전용 구현체 도입 이후에도 기존 Controller의 UseCase 계약은 유지하는 것이다. 메서드 명명은 `RepositoryLoadUseCase`라는 포트 이름과 맞추어 `get*`에서 `load*`로 정리하는 방안을 우선 검토하되, 단순 치환이 아닌 조회 의도와 반환 의미가 명확한 이름으로 조정하는 것을 원칙으로 한다.

### 2.11. [server] BranchService 분리 및 Branch 조회/관리 책임 재구성

**Status:** pending  
**Dependencies:** None  

`BranchService`를 `BranchManagementService`와 `BranchLoadService`로 분리하고, 조회성 메서드를 별도 서비스로 이관하며, `BranchLoadUseCase`의 `get*` 메서드를 `load*`로 정리할지 검토 후 반영 기준을 수립한다.

**Details:**

[source: jgitkins-server, original subtask: custom]
참조 문서: .taskmaster/docs/refactor/task_2_11_plan.md. 검토 범위는 `server/src/main/java/io/jgitkins/server/application/service/BranchService.java`, `server/src/main/java/io/jgitkins/server/application/port/in/BranchLoadUseCase.java`, `server/src/main/java/io/jgitkins/server/application/port/in/BranchCreateUseCase.java`, `server/src/main/java/io/jgitkins/server/application/port/in/BranchDeleteUseCase.java`, `server/src/main/java/io/jgitkins/server/presentation/api/rest/BranchController.java`, `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`, `server/src/test/java/io/jgitkins/server/application/service/BranchServiceTest.java`, `server/src/test/java/io/jgitkins/server/presentation/api/rest/BranchControllerTest.java`, `server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java` 및 관련 조회 호출부 전반이다. 목표는 브랜치 조회와 생성/삭제 오케스트레이션 책임을 분리하여 서비스 응집도를 높이고, `BranchLoadUseCase`의 메서드 명명을 `get*`에서 `load*`로 정렬할지 검토한 뒤 일관된 기준을 문서화하는 것이다.

### 2.12. [server] PushEventHandleService 분해 및 정책/실행 경계 1차 분리

**Status:** pending  
**Dependencies:** None  

`PushEventHandleService`가 직접 수행하던 브랜치 상태 반영, 정책 해석, 실행 요청 생성을 분리해 오케스트레이션 전용 서비스로 얇게 만들고, 향후 PR/branch event 확장을 위한 첫 seam을 만든다.

**Details:**

[source: jgitkins-server, original subtask: custom]
대상 범위는 `server/src/main/java/io/jgitkins/server/application/service/PushEventHandleService.java`, `server/src/main/java/io/jgitkins/server/application/support/PushJobCreationPolicy.java`, `server/src/main/java/io/jgitkins/server/application/validate/JobCreationValidator.java`, `server/src/test/java/io/jgitkins/server/application/service/PushEventHandleServiceTest.java`, `server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`와 신규 support collaborator 패키지다. 1차 목표는 `PushEventHandleService`를 `BranchChangeRecorder`, `EventPolicyResolver`, `ExecutionRequestService` 협력자로 분해하여 브랜치 변경 사실 기록, `ci.yml` 기반 정책 해석, `JobCreateUseCase` 호출 경계를 명확히 나누는 것이다. 이 단계에서는 기존 push 기반 동작과 runner dispatch 흐름을 유지하고, `PushJobCreationPolicy`는 push 전용 구현을 감싼 상태로 두되 향후 일반화 가능한 이름과 테스트 seam을 확보한다. 관련 단위 테스트를 추가하여 오케스트레이션 경계, support package 규칙, execution command 매핑이 유지되는지 검증한다.

### 2.13. [docs] SCM/CI 도메인 모델링 bounded context 명세

**Status:** done
**Dependencies:** None

`docs/modeling/domain` 아래에 SCM 변경 흐름, CI 정책, 실행, PR readiness bounded context 문서를 작성하고 aggregate/VO/domain service 경계를 명시한다.

**Details:**

[source: docs/modeling/domain]
이번 작업은 구현 전 도메인 명세를 고정하기 위한 문서화 리팩토링이다. 범위는 `docs/modeling/domain/README.md`, `change-graph-context.md`, `ci-policy-context.md`, `pipeline-execution-context.md`, `pull-request-readiness-context.md` 작성이다. 각 bounded context 문서에는 목적, 핵심 질문, 유비쿼터스 언어, Subdomain Classification, 책임/비책임, 주요 입력/출력, Aggregate Root, Entities, Value Objects, 핵심 불변식, mermaid class diagram, domain service 후보, 현재 코드 시드, 다음 리팩터링 힌트를 포함한다. 특히 Change Graph Context는 병합 도메인의 기준 문서로 삼고 `PullRequest`, `BranchHeadSnapshot`, `MergeabilityAssessment`, `MergeTopologySummary`, `TargetDrift`의 의미를 먼저 고정한다.

### 2.14. [server] Change Graph MergeabilityAssessment 모델 도입

**Status:** done
**Dependencies:** None

`docs/modeling/domain/change-graph-context.md` 기준으로 기존 `MergeResult` 위에 제품 언어의 `MergeabilityAssessment`와 `MergeTopologySummary` 모델을 얹고, fast-forward/merge commit 필요 여부를 노출한다.

**Details:**

[source: docs/modeling/domain/change-graph-context.md]
이번 작업은 Change Graph Context의 첫 구현 단위다. 기존 `MergeResult` API 계약은 유지하되, domain model 패키지에 `MergeabilityAssessment`, `MergeabilityStatus`, `MergeTopologySummary`를 추가하고 application support assembler를 통해 기존 merge preview 결과를 제품 언어로 변환한다. `MergeService`에는 `MergeabilityEvaluationUseCase`를 추가해 향후 PR readiness 조합에서 raw `MergeResult` 대신 assessment를 사용할 수 있는 seam을 만든다. `MergeGitAdapter.previewMergeability`는 fast-forward 가능 여부와 merge commit 필요 여부를 `MergeResult`에 채워 downstream assembler가 topology를 만들 수 있게 한다. 관련 테스트는 assembler 매핑, MergeService 위임, application package convention을 검증한다.

[review closeout]
fast-forward 설명은 실제 merge 수행 전략을 약속하지 않도록 topology 설명으로 조정했다. `MergeGitAdapterTest`를 추가해 fast-forward 가능 경로와 diverged 경로의 topology flag를 JGit fixture로 검증했다. 문서에는 `PullRequest` 영속 Aggregate와 `MergeabilityAssessment` 조회 시점 계산값의 경계를 반영했다. 검증: `./gradlew :server:test` 통과.

### 2.15. [server] PullRequest 도메인/영속성/Application seam 도입

**Status:** done
**Dependencies:** 2.14

`docs/modeling/domain/change-graph-context.md` 기준으로 PR의 영속 상태를 대표하는 domain aggregate와 MBG 기반 영속성, 생성/상세 조회 application seam을 도입한다.

**Details:**

[source: docs/modeling/domain/change-graph-context.md]
이번 작업은 presentation API 없이 server 내부 seam까지 진행한다. `domain.pr` 하위에 `PullRequest` Aggregate Root와 `PullRequestId`, `PullRequestStatus`, `BranchHeadSnapshot`, `TargetDrift`, `PullRequestRepository`를 추가한다. source/target 동일 branch 금지, 열린 pull request만 close/markMerged 가능, 닫힌 pull request만 reopen 가능, 저장된 assessment snapshot은 선택적 기록값이라는 불변식을 테스트로 고정한다.

영속성은 JPA가 아니라 기존 MyBatis Generator 흐름을 따른다. `server/data/ddl.sql`과 `server/data/mbg/mbg-config.xml`에 `PULL_REQUEST`를 명세하고, `server/data/mbg/generate.sh`로 생성된 `PullRequestEntity`, `PullRequestEntityCondition`, `PullRequestEntityMbgMapper`, `PullRequestEntityMbgMapper.xml`을 사용한다. 구현체는 `infrastructure.adapter.persistence.pr.PullRequestPersistenceAdapter`로 두고 domain repository를 구현한다.

Application 계층에는 `CreatePullRequestUseCase`와 `GetPullRequestDetailUseCase`를 추가한다. PR 생성은 repository/source/target branch head snapshot만 저장하고, 병합 가능 여부는 저장하지 않는다. 상세 조회는 read-only로 저장된 `PullRequest`와 현재 Git head, `PullRequestMergeabilityResolver`가 매번 재계산한 `MergeabilityAssessment`를 조합해 `PullRequestDetailResult`를 반환한다. 조회 중 target drift가 감지되어도 영속화하지 않으며, status 변경성 command에서만 저장하는 방향을 유지한다.

[implementation closeout]
`PullRequest` Aggregate와 PR 전용 VO/status/repository를 추가했고, MBG 기반 `PULL_REQUEST` 영속성 객체와 adapter/domain mapper를 연결했다. `BranchGitPort.getHeadCommitHash`를 추가해 생성/상세 조회에서 현재 branch head를 읽을 수 있게 했다. `CreatePullRequestUseCase`는 initial snapshot만 저장하고 mergeability는 저장하지 않으며, `GetPullRequestDetailUseCase`는 read-only로 현재 Git 상태 기준 mergeability를 매번 재계산한다. `PullRequestServiceTest`, `PullRequestDomainMapperTest`, `PullRequestTest`로 생성, 상세 조회 read-only 정책, target drift 응답 계산, assessment 비영속화, domain invariant를 검증했다. 검증: `./gradlew :server:test` 통과.

### 2.16. [server] PullRequest REST API 연결

**Status:** done
**Dependencies:** 2.15

`CreatePullRequestUseCase`와 `GetPullRequestDetailUseCase`를 presentation REST API에 연결해 실제 클라이언트가 PR을 생성하고 상세 상태를 조회할 수 있게 한다.

**Details:**

[source: 2.15 implementation]
`PullRequestController`를 추가하고 `POST /repositories/{namespace}/{repoName}/pull-requests`, `GET /pull-requests/{pullRequestId}` 또는 기존 라우팅 규칙에 맞는 endpoint를 제공한다. request/response DTO와 presentation mapper를 추가해 application DTO와 API 계약을 분리한다. 생성 API는 source/target branch만 받아 PR을 저장하고, 상세 API는 저장된 PR 상태와 현재 Git 기준 mergeability/readiness seed를 반환한다. 상세 조회는 read-only이며 mergeability와 target drift 관측값을 저장하지 않는다.

테스트는 controller slice 또는 service mock 기반 REST 테스트로 happy path, source/target 동일 branch 오류, branch not found 오류, PR not found 오류를 검증한다. 다음 단계의 readiness 조합 확장을 위해 response에는 stored source/target, current source/target, status, targetDrift, mergeability를 명확히 노출한다.

### 2.32. [plan][P1][server, docs] Repository Overview 연관 객체 Repository Context 이관 계획 수립

**Status:** done
**Dependencies:** 2.29, 2.31  

`WebRepositoryController`가 동떨어져 보이는 원인인 `RepositoryOverviewUseCase`, `RepositoryOverviewService`, `RepositoryOverviewResult` 및 경로 해석 연관 객체의 목표 패키지와 점진 이관 순서를 수립한다.

**Details:**

참조 문서: `.taskmaster/docs/refactor/task_2_32_plan.md`. 검토 범위는 `server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java`, `server/src/main/java/io/jgitkins/server/repository/presentation/api/rest/RepositoryManagementController.java`, `server/src/main/java/io/jgitkins/server/application/port/in/RepositoryOverviewUseCase.java`, `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`, `server/src/main/java/io/jgitkins/server/application/dto/result/RepositoryOverviewResult.java`, `server/src/main/java/io/jgitkins/server/application/dto/RepositoryKey.java`, `server/src/main/java/io/jgitkins/server/application/dto/FileEntry.java`, `server/src/main/java/io/jgitkins/server/application/port/in/FileTreeLoadUseCase.java`, `server/src/main/java/io/jgitkins/server/application/port/in/GitRepositoryAccessUseCase.java` 및 관련 테스트다. 목표는 overview 조회 흐름을 Repository Context application 안으로 끌어오고, web internal/BFF adapter인 `WebRepositoryController`는 top-level `presentation.api.web`에 정렬하며, `RepositoryKey`, file tree, access resolution 객체는 blast radius와 cross-context 성격에 따라 즉시 이관, 유지, 후속 검토 대상으로 분류하는 것이다. 계획 문서는 3가지 이관 방안 비교, 선택 방안, 단계별 파일 이동 순서, before/after 코드 스니펫, 검증 전략, autoplan self-review 결과를 포함한다.

[implementation closeout]
`RepositoryOverviewUseCase`, `RepositoryOverviewService`, `RepositoryOverviewResult`를 Repository Context application 하위로 이관했다. `RepositoryOverviewService`는 더 이상 `RepositoryLoadUseCase`, `BranchLoadUseCase`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase` 같은 inbound use case를 주입하지 않고 `RepositoryQueryPort`, `BranchQueryPort`, `FileGitPort`, `CurrentUserPort`, `GitRepositoryAccessService`를 조합한다. `WebRepositoryController`는 web internal/BFF adapter 성격에 맞춰 `server.presentation.api.web`으로 이동했고, `getOverviewByPath(namespace, repoName, branch)`로 위임하여 path 기반 repository lookup 조합 책임을 제거했다. `RepositoryManagementController`와 테스트 import도 새 package로 정리했다. 검증: `./gradlew :server:compileJava`, `./gradlew :server:test` 통과.

### 2.33. [plan][P0][build, server, web, runner, docs] Core/Context/App 멀티모듈 분리 계획 수립

**Status:** pending
**Dependencies:** 2.32

`server`, `web`, `runner`에 흩어진 web/security/persistence/grpc 설정과 bounded context 코드를 `core-*`, `context-*`, `app-*` 모듈로 점진 분리하기 위한 실행 계획을 수립한다.

**Details:**

참조 문서: `.taskmaster/docs/refactor/task_2_33_plan.md`, `.taskmaster/docs/refactor/task_2_33_core_libraries_plan.md`. 목표 구조는 `core-common`, `core-web`, `core-security`, `core-persistence`, `core-grpc`, `server-api-client`, `context-shared`, `context-repository`, `context-execution`, `context-collaboration`, `context-identity`, `context-organization`, `app-server`, `app-web`, `app-runner`다. 핵심 결정은 1) Boot plugin은 실행 가능한 `app-*` 모듈에만 적용한다. 2) `core-*`는 business context를 의존하지 않는다. 3) `context-*`는 `app-*`를 의존하지 않는다. 4) `app-web`은 `context-repository` 같은 도메인 context를 직접 의존하지 않고 `server-api-client`와 자체 DTO/ViewModel mapper만 사용한다. 5) `core-persistence`에는 DataSource/MyBatis/transaction 공통 설정만 두고 context별 mapper/entity/adapter는 각 context가 소유한다. 계획 문서는 현재 Gradle 의존성 관찰, 목표 모듈별 책임, 금지 의존성, Gradle 코드 스니펫, component scan 전략, phase별 구현 순서, 검증 기준, 후속 subtask 후보를 포함한다.

[implementation closeout]
`core-common`, `core-web`, `core-security`, `core-persistence`, `core-grpc` library module을 추가했다. `core-common`에는 `ErrorCode`, `ProblemSpec`, `JgitkinsException`을 이동했고, server business DTO와 infrastructure exception에 의존하는 `CommitFileFactory`는 server에 유지했다. `core-web`에는 `ApiResponse`, `ApiError`, `LocationUriBuilder`를 `api` 하위 package로 이동했고, server REST/internal adapter import를 정리했다. `core-security`에는 server problem spec을 직접 의존하지 않는 `SecurityErrorResponseWriter`만 추가하고, server security handler가 이를 사용하도록 변경했다. `core-persistence`에는 `DataSourceConfig`, `MybatisConfig`를 이동하고 `JGitkinsServerApplication`에서 명시 import했다. `core-grpc`는 gRPC/protobuf dependency 기준 shell로 추가했으며 proto/stub 이동은 보류했다. Architecture test에는 core module의 app import 금지, web MVC의 core ApiResponse import 금지, core-persistence의 business persistence model 소유 금지를 추가했다. 검증: `:core-common:test`, `:core-web:test`, `:core-security:test`, `:core-persistence:test`, `:core-grpc:test`, `:server:test`, `:web:test`, `:runner:test` 통과.

### 2.34. [plan][P1][server, docs] Common infrastructure와 bounded context infrastructure 패키지 분리 계획 수립

**Status:** pending
**Dependencies:** 2.33

`server/common/infrastructure` 에 공통 설정만 남기고, 각 bounded context 의 persistence model, mapper, adapter, technical support 를 context 하위 `infrastructure` 로 이동하는 실행 계획을 수립한다.

**Details:**

참조 문서: `.taskmaster/docs/refactor/task_2_34_plan.md`. 검토 범위는 `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/*`, `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/*`, `app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/*`, `app-server/src/main/java/io/jgitkins/server/common/infrastructure/*` 신설 기준, 그리고 `collaboration`, `repository`, `identity.access`, `change.review`, `execution` 각 bounded context 의 `infrastructure` 패키지 설계다. 목표는 공통 기술 설정과 비즈니스 의미가 있는 persistence 자산을 분리해, package tree 가 소유권을 숨기지 않도록 만드는 것이다. 계획 문서는 공통 vs context 경계 원칙, 3가지 방법 비교, phase 별 이동 순서, package policy test 추가안, generated MBG 산출물 이동 기준, 병렬 작업 lane, NOT in scope 를 포함한다.
