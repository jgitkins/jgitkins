# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.9 Repository 생성 프로비저닝 이벤트 제거 및 UseCase 오케스트레이션 정리 계획서

### 배경 (왜?)
- 현재 `RepositoryLifecycleService.create()`는 저장소 저장과 git repository initialize까지만 직접 수행하고, 이후 기본 브랜치 생성과 초기 커밋 처리는 `RepositoryProvisionedEventListener`가 `AFTER_COMMIT` 시점에 담당한다.
- 현재 구조는 저장소 생성 유스케이스의 핵심 후속 절차가 이벤트 리스너에 분산되어 있어, 생성 흐름의 완료 조건과 실패 지점을 한 곳에서 파악하기 어렵다.
- `Repository.create()`는 영속화 이전 시점에 `RepositoryProvisionedEvent`를 등록하므로 이벤트의 `repositoryId`가 비어 있을 가능성이 있으며, 실제 리스너도 이를 직접 사용하지 못하고 owner/name으로 다시 조회한다.
- 현재 후속 절차는 외부 bounded context 통합보다는 같은 애플리케이션 내부의 프로비저닝 단계 성격이 강하므로, 도메인 이벤트보다 애플리케이션 오케스트레이션으로 다루는 편이 더 자연스럽다.
- Presentation 계층으로 책임을 올리는 방식은 헥사고날 아키텍처의 경계와 맞지 않으므로, Presentation은 단일 UseCase 호출을 유지하고 Application 계층 내부에서 구조를 재정리해야 한다.

### 목표 (Goals)
- `RepositoryProvisionedEventListener` 기반 생성 후처리 흐름을 제거한다.
- `application.support` 패키지에 `RepositoryProvisioner`를 추가하여 저장소 프로비저닝 상세 절차를 캡슐화한다.
- `RepositoryLifecycleService.create()`가 저장소 생성 유스케이스의 전체 흐름을 직접 오케스트레이션하도록 정리한다.
- `Repository.create()`에서 애플리케이션 후속 절차를 위한 이벤트 등록을 제거하여 도메인 책임을 단순화한다.
- Presentation 계층의 호출 계약은 유지하여 컨트롤러 변경 범위를 최소화한다.

### 범위 (Scope)
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/event/RepositoryProvisionedEventListener.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/domain/event/RepositoryProvisionedEvent.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/domain/aggregate/Repository.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryLifecycleService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/support/RepositoryProvisioner.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryManagementController.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/application/port/in/RepositoryCreateUseCase.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/application/common/event/DomainEventPublisher.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/infrastructure/event/SpringDomainEventPublisher.java`
- **수정 제외 대상**: `Organize`, `Runner`, `Job` aggregate의 다른 domain event 발행 구조는 이번 작업에서 변경하지 않는다.
- **수정 제외 대상**: Outbox, 메시지 브로커, 비동기 재시도 체계 도입은 이번 작업 범위에서 제외한다.
- **수정 제외 대상**: Controller의 API 계약과 DTO 구조 변경은 이번 작업 범위에서 제외한다.

### 방법 조사 및 선택
- **방안 1**: 현재 이벤트 리스너 구조를 유지하되, 리스너 내부 로직만 별도 서비스로 추출한다.
  장점은 변경 범위가 가장 작다.
  단점은 유스케이스 흐름이 여전히 이벤트에 분산되어 구조 문제가 본질적으로 남는다.
- **방안 2**: `RepositoryProvisionedEventListener`와 `RepositoryProvisionedEvent`를 제거하고, `RepositoryProvisioner`를 `application.support`에 추가한 뒤 `RepositoryLifecycleService.create()`가 이를 직접 호출한다.
  장점은 생성 유스케이스의 흐름이 한 곳으로 모이고 Presentation은 단일 UseCase 호출만 유지할 수 있다.
  단점은 기존 이벤트 기반 분리보다 동기 절차가 명시적으로 드러나므로 서비스 메서드 책임 분해가 필요하다.
- **방안 3**: 이벤트는 제거하되, `RepositoryProvisioningUseCase`를 별도로 도입하여 컨트롤러 또는 상위 파사드가 두 개의 UseCase를 순차 호출한다.
  장점은 프로비저닝 단위를 별도 유스케이스로 명확히 분리할 수 있다.
  단점은 Presentation이 두 단계 흐름을 알아야 하거나 상위 조합 계층이 추가되어 현재 요구사항과 맞지 않는다.
- **선택 방안**: 방안 2를 선택한다.
- **선택 이유**: 이번 요구사항의 핵심은 이벤트 리스닝 제거와 `support` 패키지의 `RepositoryProvisioner` 도입, 그리고 단일 `RepositoryCreateUseCase` 호출 유지에 있다.

### 설계 원칙
- Presentation은 요청/응답 변환과 단일 UseCase 호출만 담당한다.
- Application Service는 유스케이스 흐름을 오케스트레이션한다.
- `RepositoryProvisioner`는 재사용 가능한 후속 절차 조합을 캡슐화하되, 도메인 규칙을 대체하지 않는다.
- Domain Aggregate는 저장소 상태와 규칙만 표현하고, 애플리케이션 후속 절차를 위한 이벤트 등록 책임은 가지지 않는다.
- 다른 aggregate에서 사용 중인 `DomainEventPublisher` 제거 여부는 이번 작업 범위와 분리한다.

### 변경 대상 BEFORE / AFTER 목록
- `RepositoryProvisionedEventListener`는 `@TransactionalEventListener` 기반 후처리 진입점에서 제거한다.
- `RepositoryProvisionedEvent`는 저장소 생성 후속 절차 전달 객체에서 제거한다.
- `Repository.create()`는 `RepositoryProvisionedEvent` 등록을 수행하는 구조에서 순수 aggregate 생성 구조로 변경한다.
- `RepositoryLifecycleService.create()`는 저장 후 이벤트 발행으로 흐름을 넘기는 구조에서 `RepositoryProvisioner`를 직접 호출하는 구조로 변경한다.
- `RepositoryLifecycleService`의 `DomainEventPublisher` 의존성은 저장소 생성 경로에서 제거한다.
- `application.support` 패키지는 `RepositoryProvisioner`가 없는 구조에서 생성 후 상세 프로비저닝 절차를 담는 구조로 변경한다.
- 기본 브랜치 생성, 초기 파일 준비, 초기 커밋, HEAD 갱신, `markInit()` 반영 로직은 이벤트 리스너 내부 구현에서 `RepositoryProvisioner` 내부 구현으로 이동한다.
- `RepositoryManagementController`는 여전히 `RepositoryCreateUseCase.create()`만 호출하는 구조를 유지한다.
- `DomainEventPublisher`와 `SpringDomainEventPublisher`는 다른 aggregate 사용 여부를 확인한 뒤 유지 또는 후속 과제로 분리한다.

### 계획 (Plan)
- **단계 1**: 현재 저장소 생성 흐름과 이벤트 리스너 내부 책임을 분석하여 `RepositoryProvisioner`의 입력과 책임 범위를 확정한다.
- **단계 2**: `RepositoryProvisioner`가 담당할 세부 절차를 `기본 브랜치 생성`, `초기 컨텐츠 커밋`, `HEAD 갱신`, `초기화 상태 반영` 순으로 정리한다.
- **단계 3**: `RepositoryLifecycleService.create()`가 저장 및 git initialize 직후 `RepositoryProvisioner`를 호출하도록 흐름을 재설계한다.
- **단계 4**: `RepositoryProvisionedEventListener`, `RepositoryProvisionedEvent`, `Repository.create()`의 이벤트 등록 코드를 제거하는 변경 순서를 확정한다.
- **단계 5**: 변경 후 회귀 검증 범위와 남는 기술 부채를 정리한다.

### 검증 기준
- 저장소 생성 API는 기존과 동일하게 단일 `RepositoryCreateUseCase` 호출로 동작해야 한다.
- 저장소 생성 후 기본 브랜치 엔트리가 정상 생성되어야 한다.
- `readme` 옵션이 있는 경우 초기 커밋과 HEAD 갱신이 기존과 동일하게 수행되어야 한다.
- `readme` 옵션이 없는 경우 불필요한 초기 커밋이 발생하지 않아야 한다.
- `RepositoryProvisionedEventListener`와 `RepositoryProvisionedEvent` 제거 이후에도 저장소 생성 회귀가 없어야 한다.
- 다른 aggregate의 domain event 흐름은 이번 작업으로 인해 깨지지 않아야 한다.

### 개선 사항 점검
- **개선안 1**: `RepositoryProvisioner`의 public method는 하나로 유지하고 세부 절차는 private method로 분리한다.
- **개선안 2**: 저장소 프로비저닝 실패 시점과 실패 메시지를 로깅 또는 예외 메시지 관점에서 명확히 정리한다.
- **개선안 3**: 저장소 생성과 프로비저닝을 장기적으로 outbox 또는 상태 전이 모델로 분리할 필요가 있는지 후속 검토 과제로 남긴다.
- **선택 개선안**: 개선안 1, 2를 이번 계획에 반영하고 개선안 3은 후속 과제로 유지한다.

### 기대효과 (Expected Benefits)
- 저장소 생성 유스케이스의 전체 흐름이 `Application Service + Support` 조합으로 한 곳에 정리된다.
- 이벤트 payload와 재조회에 의존하던 구조가 제거되어 코드 이해 비용이 줄어든다.
- Presentation 계층은 단일 UseCase 호출을 유지하면서도 세부 구현은 Application 계층에서 명확히 분리할 수 있다.
- 향후 저장소 생성 실패 처리, 재시도 정책, 상태 전이 확장 시 구조적 출발점을 더 명확히 확보할 수 있다.

### 예시 (방안 2 기준 코드 스니펫)

#### AS-IS (현재 구조)
```java
@Transactional
public RepositoryResult create(RepositoryCreateCommand command) {
    Repository repository = createRepository(command);
    validateRepositoryCreation(repository, command.organizeId());

    Repository saved = repositoryPort.save(repository);
    repositoryGitPort.initialize(
            repositoryNamespaceResolver.resolve(repository.getOwnerType(), repository.getOwnerId()),
            repository.getName().getValue());

    publishDomainEvents(saved);
    return repositoryApplicationMapper.toDto(saved);
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = REQUIRES_NEW)
public void onRepositoryProvisioned(RepositoryProvisionedEvent event) {
    Branch defaultBranch = Branch.create(...);
    branchPort.save(defaultBranch);
    commitGitPort.commit(...);
    repositoryGitPort.updateHeadReference(...);
    repositoryPort.update(repository.markInit(LocalDateTime.now()));
}
```

#### TO-BE (개선 제안 구조)
```java
@Transactional
public RepositoryResult create(RepositoryCreateCommand command) {
    Repository repository = createRepository(command);
    validateRepositoryCreation(repository, command.organizeId());

    Repository saved = repositoryPort.save(repository);
    initializeGitRepository(saved);

    Repository provisioned = repositoryProvisioner.provision(saved, command);
    return repositoryApplicationMapper.toDto(provisioned);
}

private void initializeGitRepository(Repository repository) {
    repositoryGitPort.initialize(
            repositoryNamespaceResolver.resolve(repository.getOwnerType(), repository.getOwnerId()),
            repository.getName().getValue());
}
```

```java
@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    public Repository provision(Repository repository, RepositoryCreateCommand command) {
        createDefaultBranch(repository);
        return initializeContentIfNeeded(repository, command.initialCommitOptions());
    }

    private void createDefaultBranch(Repository repository) {
        Branch defaultBranch = Branch.create(
                repository.getId().getValue(),
                repository.getDefaultBranch().getValue(),
                false,
                true,
                true
        );
        branchPort.save(defaultBranch);
    }

    private Repository initializeContentIfNeeded(Repository repository, InitialCommitOptions options) {
        if (options == null || !options.requiresInitialContent()) {
            return repository;
        }

        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getName().getValue();
        String branchName = repository.getDefaultBranch().getValue();

        List<CommitFile> files = commitFileFactory.prepareInitialFile(repoName);
        commitGitPort.commit(
                namespace,
                repoName,
                branchName,
                options.commitMessage(),
                options.authorName(),
                options.authorEmail(),
                files
        );
        repositoryGitPort.updateHeadReference(namespace, repoName, branchName);

        Repository initialized = repository.markInit(LocalDateTime.now());
        return repositoryPort.update(initialized);
    }
}
```

```java
public static Repository create(OwnerType ownerType,
                                OwnerId ownerId,
                                RepositoryName name,
                                RepositoryPath path,
                                BranchName defaultBranch,
                                RepositoryVisibility visibility,
                                String description,
                                String clonePath,
                                String credentialId,
                                InitialCommitOptions initialCommitOptions) {
    if (initialCommitOptions == null) {
        throw new IllegalArgumentException("InitialCommitOptions must not be null");
    }
    LocalDateTime now = LocalDateTime.now();
    return new Repository(
            null,
            ownerType,
            ownerId,
            name,
            path,
            defaultBranch,
            visibility,
            description,
            now,
            now,
            credentialId,
            clonePath,
            null,
            initialCommitOptions.requiresInitialContent(),
            false
    );
}
```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repositories")
public class RepositoryManagementController {

    private final RepositoryCreateUseCase repositoryCreateUseCase;
    private final RepositoryRequestMapper repositoryRequestMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResult>> create(@Valid @RequestBody RepositoryCreateRequest request) {
        RepositoryCreateCommand command = repositoryRequestMapper.toCommand(request);
        RepositoryResult result = repositoryCreateUseCase.create(command);
        return ApiResponse.created(result.id(), result);
    }
}
```

### 예시 해설
- `RepositoryManagementController`는 기존처럼 단일 `RepositoryCreateUseCase`만 호출한다.
- `RepositoryLifecycleService`는 유스케이스 진입점으로서 저장, git initialize, provision 호출 순서를 직접 관리한다.
- `RepositoryProvisioner`는 이벤트 리스너에 있던 후속 절차를 묶되, 외부에 노출하는 public 메서드는 `provision(...)` 하나로 제한한다.
- `Repository.create()`는 이벤트 발행을 제거하고 aggregate 생성 책임만 유지한다.

### 주의사항
- **포맷팅 금지**: 계획 단계에서 코드 포맷팅이나 구조 외 변경은 수행하지 않는다.
- **기존 기능 보장**: 저장소 생성 API의 외부 계약과 생성 결과는 유지해야 한다.
- **계획우선**: 계획 문서 작성 단계에서는 구현을 진행하지 않는다.
- **예시전체나열**: 변경하려는 핵심 흐름의 BEFORE / AFTER를 누락 없이 정리한다.
- **문서체규약**: 모든 문장은 공식 문서체로 유지한다.

### 결론
- Task 2.9는 저장소 생성 후처리의 이벤트 리스닝 구조를 제거하고, `RepositoryProvisioner`를 통해 유스케이스 내부 오케스트레이션으로 재정리하기 위한 계획 작업으로 정의한다.
- 구현은 `RepositoryLifecycleService`가 단일 UseCase 진입점을 유지한 채 `RepositoryProvisioner`를 호출하는 방향으로 진행한다.
