# Task 2.33 Plan: 멀티모듈 Core/Context/App 분리 설계

### 목적
- 현재 `server`, `web`, `runner`가 각자 보유한 web/security/persistence/grpc 설정과 도메인 context 코드를 점진적으로 분리한다.
- 실행 가능한 애플리케이션 모듈은 `app-*`에만 두고, 공통 기술 설정은 `core-*`, 비즈니스 경계는 `context-*` 모듈로 승격한다.
- `app-web`은 domain context를 직접 의존하지 않는다. `app-web`은 server가 제공하는 API 응답을 client SDK로 호출하고, web 자체 DTO/ViewModel로 변환해서 사용한다.
- `server-api-client`는 `app-web` 전용 client SDK로 취급하고 `app-server`에는 두지 않는다.

### 현재 관찰
- 현재 Gradle root는 다음 3개 모듈만 포함한다.
```gradle
include 'runner'
include 'server'
include 'web'
```
- `server`는 `spring-boot-starter-web`, validation, security, oauth2-client, gRPC server, MyBatis, JGit, OpenAPI, JWT, actuator를 함께 가진다.
- `web`은 `spring-boot-starter-web`, thymeleaf, security, oauth2-client, redis session, actuator를 가진다.
- `runner`는 Boot app이지만 HTTP server보다는 AMQP, JDBC, `spring-web`, gRPC client, Docker 실행기 성격이 강하다.
- `server` 내부는 이미 `repository`, `execution`, `shared` context 패키지가 생겼지만, top-level `application/domain/infrastructure/presentation`도 함께 남아 있다.

### 결정 원칙
- Boot plugin은 실행 가능한 app module에만 적용한다.
- library module은 `java-library`를 기본으로 사용한다.
- `context-*`는 `app-*`를 의존하지 않는다.
- `core-*`는 business context를 의존하지 않는다.
- `app-*`는 조립자다. profile, component scan, datasource wiring, security filter chain, bootJar만 담당한다.
- `app-web`은 `context-repository`, `context-execution`, `context-collaboration`을 직접 의존하지 않는다.
- `app-web`은 `server-api-client` 또는 `core-web-client` 성격의 SDK와 자체 DTO mapper만 의존한다.
- `ApiResponse`는 `app-web`의 Thymeleaf MVC 화면 응답 모델이 아니라 server API client의 JSON transport envelope로만 사용한다.

### 목표 구조
```text
jgitkins
├── core-common
├── core-web
├── core-security
├── core-persistence
├── core-grpc
├── server-api-client
├── context-shared
├── context-repository
├── context-execution
├── context-collaboration
├── context-identity
├── context-organization
├── app-server
├── app-web
└── app-runner
```

### 1차 권장 구조
- 모든 목표 모듈을 한 번에 만들지 않는다.
- 1차는 공통 기술 설정과 가장 정리된 Repository Context만 대상으로 한다.
```text
core-common
core-web
core-persistence
server-api-client
context-shared
context-repository
app-server
app-web
app-runner
```

### 모듈 책임

#### `core-common`
- 공통 exception base, error/problem contract, value object helper, domain event interface처럼 기술과 도메인 context에 독립적인 코드만 둔다.
- Spring Boot auto configuration을 두지 않는다.
- 후보:
```text
server/src/main/java/io/jgitkins/server/common/**
server/src/main/java/io/jgitkins/server/shared/common/**
```

#### `core-web`
- HTTP API envelope와 MVC 공통 설정을 분리해서 둔다.
- `ApiResponse`, `ApiError`, 공통 presentation error spec, 공통 validation helper, 공통 Jackson/WebMvc 설정 후보를 포함한다.
- 특정 context controller, request DTO, mapper는 넣지 않는다.
- `ApiResponse`는 REST/server API client boundary 전용이다. `web` 모듈의 `@Controller`가 `Model`에 넣는 화면 모델로 사용하지 않는다.
- 후보:
```text
server/src/main/java/io/jgitkins/server/presentation/common/**
server/src/main/java/io/jgitkins/server/presentation/common/error/**
server/src/main/java/io/jgitkins/server/presentation/exception/**
server/src/main/java/io/jgitkins/server/presentation/util/LocationUriBuilder.java
```
- 주의:
  - `GlobalExceptionHandler`는 `core-web`에 넣을 수 있지만, error code mapper가 app별로 달라질 수 있으면 auto-configuration으로 분리한다.
  - Spring Security handler는 `core-security` 후보로 보는 편이 낫다.

#### `core-security`
- 인증/인가 filter, security handler, JWT/OAuth 공통 설정 후보를 둔다.
- 단, app-server와 app-web의 security chain은 다를 가능성이 크다.
- 1차에서는 공통 handler와 token utility만 후보로 분류하고, full migration은 보류한다.

#### `core-persistence`
- DataSource, MyBatis, transaction 공통 설정만 둔다.
- context별 MBG mapper, persistence adapter, table entity는 context가 소유한다.
- 잘못된 예:
```text
core-persistence
└── RepositoryEntity
└── JobEntity
└── UserEntity
```
- 올바른 예:
```text
core-persistence
└── DataSourceConfig
└── MybatisConfig
└── TransactionConfig
```

#### `core-grpc`
- protobuf generation convention, gRPC client/server 공통 설정, status mapper를 둔다.
- `context-execution` 또는 `app-runner` 전용 generated stub가 섞이지 않도록 주의한다.

#### `server-api-client`
- `app-web`이 `app-server`를 호출하기 위한 SDK다.
- `app-web`은 이 모듈만 통해 server API를 호출한다.
- server domain/application DTO를 직접 노출하지 않고, client contract DTO를 둔다.
```java
public interface RepositoryApiClient {

    List<RepositoryClientDto> getUserRepositories(String username);

    RepositoryOverviewClientDto getRepositoryOverview(String namespace, String repoName, String branch);
}
```
```java
@Component
public class RestRepositoryApiClient implements RepositoryApiClient {

    private final RestClient restClient;

    public RepositoryOverviewClientDto getRepositoryOverview(String namespace, String repoName, String branch) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/internal/repositories/{namespace}/{repoName}/overview")
                        .queryParam("branch", branch)
                        .build(namespace, repoName))
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<RepositoryOverviewClientDto>>() {})
                .getData();
    }
}
```
- `ApiResponse` contract는 `core-web` 또는 `server-api-client`의 client-facing copy 중 하나로 선택한다.
- 추천은 `core-web`의 envelope contract를 재사용하되, server 내부 exception/advice 설정까지 끌고 오지 않도록 모듈을 세분화하는 것이다.

#### `context-shared`
- 여러 context가 공유하지만 특정 기술에 묶이지 않는 application/domain 협력자를 둔다.
- 후보:
```text
server/src/main/java/io/jgitkins/server/shared/**
```
- 주의:
  - shared는 작게 유지한다.
  - shared에 repository/execution 비즈니스 규칙을 밀어 넣지 않는다.

#### `context-repository`
- repository, branch, member, repository overview, git access application/domain/infrastructure/presentation REST adapter를 소유한다.
- 후보:
```text
server/src/main/java/io/jgitkins/server/repository/**
```
- 단, `presentation.api.web`에 있는 web internal/BFF adapter는 `app-server` 또는 top-level server presentation에 남기는 편이 낫다.
- context presentation은 public REST resource adapter 중심으로 둔다.

#### `context-execution`
- job, runner, dispatch, execution policy, gRPC dispatch service를 소유한다.
- 후보:
```text
server/src/main/java/io/jgitkins/server/execution/**
runner/src/main/java/io/jgitkins/runner/**
```
- runner 전체를 context-execution에 넣는 것이 아니라, runner가 사용하는 contract/client와 execution domain/application을 먼저 분리한다.

#### `app-server`
- 기존 `server`의 실행 애플리케이션이다.
- 의존:
```text
app-server
├── core-common
├── core-web
├── core-security
├── core-persistence
├── core-grpc
├── context-shared
├── context-repository
├── context-execution
├── context-collaboration
├── context-identity
└── context-organization
```
- 책임:
  - `JGitkinsServerApplication`
  - profile config
  - app-level security chain
  - component scan/import composition
  - bootJar

#### `app-web`
- 기존 `web`의 실행 애플리케이션이다.
- 의존:
```text
app-web
├── core-web
├── core-security
└── server-api-client
```
- 비의존:
```text
app-web -X-> context-repository
app-web -X-> context-execution
app-web -X-> context-collaboration
```
- web 내부 DTO 변환 예:
```java
public record RepositoryOverviewViewModel(
        String namespace,
        String name,
        String defaultBranch,
        List<BranchViewModel> branches
) {

    public static RepositoryOverviewViewModel from(RepositoryOverviewClientDto dto) {
        return new RepositoryOverviewViewModel(
                dto.repository().namespace(),
                dto.repository().name(),
                dto.repository().defaultBranch(),
                dto.branches().stream()
                        .map(BranchViewModel::from)
                        .toList());
    }
}
```
- Thymeleaf MVC controller는 `ApiResponse`를 반환하거나 model attribute로 넣지 않는다.
```java
@Controller
public class RepositoryPageController {

    @GetMapping("/{namespace}/{repoName}")
    public String overview(@PathVariable String namespace,
                           @PathVariable String repoName,
                           Model model) {
        RepositoryOverviewClientDto dto = repositoryApiClient.getOverview(namespace, repoName, null);
        model.addAttribute("repository", RepositoryOverviewViewModel.from(dto));
        return "repository/overview";
    }
}
```

#### `app-runner`
- 기존 `runner`의 실행 애플리케이션이다.
- 의존:
```text
app-runner
├── core-common
├── core-grpc
└── execution-client 또는 context-execution-contract
```
- runner가 server DB를 직접 읽는 구조는 장기적으로 줄인다.
- dispatch/report는 gRPC 또는 server API client를 통해 명확한 contract로 통신한다.

### Gradle 설계

#### Root settings
```gradle
rootProject.name = 'jgitkins'

include 'core-common'
include 'core-web'
include 'core-security'
include 'core-persistence'
include 'core-grpc'
include 'server-api-client'

include 'context-shared'
include 'context-repository'
include 'context-execution'
include 'context-collaboration'
include 'context-identity'
include 'context-organization'

include 'app-server'
include 'app-web'
include 'app-runner'
```

#### Library module
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api project(':core-common')
    implementation 'org.springframework:spring-context'
}
```

#### Boot app module
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation project(':core-web')
    implementation project(':core-persistence')
    implementation project(':context-repository')
    implementation project(':context-execution')
}

bootJar {
    archiveFileName = 'app.jar'
}
```

### Component Scan 전략
- app module이 전체 root package를 무작정 scan하면 context module 분리 효과가 약해진다.
- 1차는 안정성을 위해 기존 scan을 유지할 수 있으나, 목표는 explicit import다.
```java
@SpringBootApplication
@Import({
        CoreWebConfiguration.class,
        CorePersistenceConfiguration.class,
        RepositoryContextConfiguration.class,
        ExecutionContextConfiguration.class
})
public class JGitkinsServerApplication {
}
```
- context module은 자체 configuration class를 제공한다.
```java
@Configuration
@ComponentScan(basePackageClasses = RepositoryContextConfiguration.class)
@MapperScan("io.jgitkins.repository.infrastructure.persistence.mapper")
public class RepositoryContextConfiguration {
}
```

### 패키지명 목표
- 모듈 분리와 동시에 package prefix도 정리하는 것이 이상적이지만, 1차에서 모두 바꾸면 변경량이 과하다.
- 1차는 기존 `io.jgitkins.server.*` package를 유지하고 Gradle module만 먼저 분리한다.
- 2차에서 다음처럼 package rename을 검토한다.
```text
io.jgitkins.core.web
io.jgitkins.core.persistence
io.jgitkins.context.repository
io.jgitkins.context.execution
io.jgitkins.app.server
io.jgitkins.app.web
io.jgitkins.app.runner
```

### 단계별 실행 계획

#### Phase 0: 기준선 고정
- `settings.gradle`, 각 `build.gradle`, dependency tree를 문서화한다.
- `./gradlew :server:test`, `:web:test`, `:runner:test` 기준선을 확보한다.
- circular dependency가 생기지 않도록 module dependency rule을 먼저 문서화한다.

검증:
```bash
./gradlew :server:test
./gradlew :web:test
./gradlew :runner:test
```

#### Phase 1: `core-common`, `core-web` 추출
- `ApiResponse`, `ApiError`, 공통 problem/error contract를 `core-web` 또는 `core-common`으로 이동한다.
- `server`, `web`, `runner` 중 필요한 app만 해당 core를 의존한다.
- `ApiResponse.success/failure`처럼 내부 전용 factory visibility 정책을 유지한다.
- `web` 모듈은 server API client transport envelope 용도로만 `ApiResponse`를 사용한다.
- `ArchitecturePackageConventionTest`를 module boundary test로 확장한다.

검증 포인트:
- server REST controller와 server internal REST adapter는 계속 `ResponseEntity<ApiResponse<T>>`를 반환한다.
- web MVC 화면 controller는 `String viewName`, `Model`, `ModelAndView`를 사용하고 `ApiResponse`를 노출하지 않는다.
- controller가 직접 `ResponseEntity.ok/created/status`를 호출하지 않는다.
- `ApiResponse` public factory만 사용한다.

#### Phase 2: `server-api-client` 도입 및 `app-web` 의존 정리
- `server-api-client`를 `app-web` 전용 SDK로 고정하고, web module의 server 호출부를 이 모듈로 감싼다.
- `app-web` 내부는 client DTO를 web ViewModel로 변환한다.
- `app-web`이 `context-*` 또는 server application DTO를 직접 import하지 못하도록 테스트를 추가한다.

금지 규칙:
```java
assertNoImports(appWebRoot, "import io.jgitkins.server.repository.");
assertNoImports(appWebRoot, "import io.jgitkins.server.execution.");
assertNoImports(appWebRoot, "import io.jgitkins.server.application.");
```

#### Phase 3: `core-persistence` 추출
- DataSource/MyBatis/transaction 공통 설정만 이동한다.
- MBG mapper/entity는 각 context 또는 legacy server에 남긴다.
- `@MapperScan` 범위를 app-server composition에서 명시한다.

#### Phase 4: `context-repository` Gradle module 승격
- 이미 패키지 정리가 진행된 `server/repository/**`를 별도 module로 이동한다.
- top-level legacy application에 남은 repository 관련 포트/DTO를 먼저 정리한다.
- `RepositoryFileService`, `FileEntry`, `RepositoryKey`처럼 아직 top-level에 남은 파일 관련 객체의 소유권을 별도 subtask로 확정한다.

#### Phase 5: `app-server` 조립
- 기존 `server` module을 `app-server`로 rename하거나 신규 `app-server`를 만들고 기존 server source를 이동한다.
- context/core module을 의존하도록 `build.gradle`을 정리한다.
- bootJar는 `app-server`만 활성화한다.

#### Phase 6: `context-execution`, `app-runner` 정리
- runner와 server execution package의 contract를 분리한다.
- gRPC proto/stub 생성 위치를 `core-grpc` 또는 `context-execution-contract`로 고정한다.
- runner가 server DB나 server internal model에 직접 결합된 부분을 줄인다.

### 위험과 대응
- 위험: 모듈 수가 늘어나면서 빌드 시간이 증가한다.
  - 대응: 처음부터 모든 context를 분리하지 않고 core/context-repository부터 진행한다.
- 위험: Spring component scan이 깨진다.
  - 대응: context별 `*ContextConfiguration`과 app-level `@Import`를 사용한다.
- 위험: `app-web`이 편의상 context DTO를 직접 import한다.
  - 대응: architecture test로 `app-web -> context-*` import 금지.
- 위험: `server-api-client`가 app-server 쪽에 섞이면 server 런타임이 transport SDK에 오염된다.
  - 대응: `server-api-client`는 app-web에만 붙이고 app-server는 포함하지 않는다.
- 위험: `core-web`이 보안, 세션, OAuth까지 끌어안는다.
  - 대응: `core-web`은 HTTP envelope/MVC 공통까지만 두고 security는 별도 module로 분리한다.
- 위험: persistence 공통화가 entity 공통화로 변질된다.
  - 대응: `core-persistence`에는 설정만 둔다. mapper/entity/adapter는 context가 소유한다.

### 완료 기준
- 멀티모듈 목표 구조와 1차 범위가 문서화되어 있다.
- `app-web`은 context module을 직접 의존하지 않는다는 원칙이 명시되어 있다.
- Boot plugin은 app module에만 적용한다는 Gradle 원칙이 명시되어 있다.
- `core-web`, `core-persistence`, `server-api-client`, `context-repository`의 책임과 금지 사항이 코드 스니펫과 함께 정의되어 있다.
- 후속 구현 subtask를 `Phase 1 -> Phase 2 -> Phase 3 -> Phase 4` 순서로 쪼갤 수 있다.

### 후속 subtask 후보
- `2.34 [build] core-common/core-web 1차 모듈 추출`
- `2.35 [web] server-api-client 도입 및 app-web context 직접 의존 금지`
- `2.36 [build] core-persistence 설정 모듈 추출`
- `2.37 [repository] context-repository Gradle module 승격`
- `2.38 [execution] execution contract/core-grpc 분리 및 app-runner 의존 정리`

### 세부 실행 문서
- `.taskmaster/docs/refactor/task_2_33_core_libraries_plan.md`: `core-*` library module만 먼저 분리하는 간결 실행 계획과 상세 코드 스니펫을 다룬다.
- `.taskmaster/docs/refactor/task_2_33_injection_core_libraries_from_apps_plan.md`: `app-server`, `app-web`, `app-runner` rename 이후 core library 주입과 bootstrap 정리를 다룬다.
