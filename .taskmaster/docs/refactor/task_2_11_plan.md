# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.11 BranchService 분리 및 Branch 조회/관리 책임 재구성 계획서

### 배경 (왜?)
- 현재 `BranchService`는 조회, 생성, 삭제를 함께 담당한다.
- 조회는 단순 조회와 매핑 중심이고, 생성/삭제는 검증과 Git 연동을 포함한다.
- 책임 성격이 다른 메서드가 한 클래스에 섞여 있어 응집도가 낮다.
- `BranchLoadUseCase`와 `get*` 메서드명도 일관성이 부족하다.

### 목표 (Goals)
- `BranchService`를 `BranchManagementService`와 `BranchLoadService`로 분리한다.
- 조회 메서드를 `BranchLoadService`로 이동한다.
- `BranchLoadUseCase` 메서드명을 `load*`로 정리한다.
- `BranchManagementService`의 `createBranch`, `deleteBranch`를 더 읽기 쉬운 구조로 정리한다.
- 기본 브랜치 삭제 금지 규칙을 `Branch` 도메인 객체로 이동한다.

### 범위 (Scope)
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/BranchService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/BranchManagementService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/BranchLoadService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/port/in/BranchLoadUseCase.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/domain/Branch.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/presentation/api/rest/BranchController.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`
- **수정 대상**: 관련 테스트 클래스
- **수정 제외 대상**: `BranchGitPort`, `BranchPersistencePort`, API URL 및 DTO 규격

### 계획 (Plan)
- **단계 1**: `BranchService`의 조회 메서드와 관리 메서드를 분리한다.
- **단계 2**: `BranchLoadService`와 `BranchManagementService`를 생성한다.
- **단계 3**: `BranchLoadUseCase`와 호출부 메서드명을 `load*`로 정리한다.
- **단계 4**: `BranchManagementService`의 공통 로딩 흐름과 예외 생성을 private 메서드로 정리한다.
- **단계 5**: 기본 브랜치 삭제 금지 규칙을 `Branch.delete()`로 이동하고 테스트를 수정한다.

### 기대효과 (Expected Benefits)
- 조회와 관리 책임이 분리된다.
- `LoadUseCase`와 `load*` 메서드명이 정렬된다.
- `createBranch`, `deleteBranch`의 시나리오 가독성이 좋아진다.
- 삭제 규칙이 validator가 아니라 도메인 객체에 위치하게 된다.
- 이후 브랜치 조회와 관리 변경을 더 독립적으로 수행할 수 있다.

### 예시 (방안 2 기준 코드 스니펫)

#### AS-IS (현재 구조)
```java
@Service
@RequiredArgsConstructor
public class BranchService implements BranchLoadUseCase, BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchApplicationMapper branchApplicationMapper;
    private final BranchCreationValidator branchCreationValidator;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final BranchGitPort branchGitPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryPersistencePort repositoryPort;

    @Override
    public List<BranchSearchResult> getBranches(Long repositoryId) {
        return branchPort.findAllByRepositoryId(repositoryId)
                .stream()
                .map(branchApplicationMapper::toSearchResult)
                .toList();
    }

    @Override
    public BranchSearchResult getBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .map(branchApplicationMapper::toSearchResult)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.BRANCH_NOT_FOUND,
                        "Branch not found: " + branchName));
    }

    @Override
    public void createBranch(BranchCreateCommand command) {
        Repository repository = loadRepositoryWithWriteAccess(command.repositoryId());
        String namespace = repositoryNamespaceResolver.resolve(repository);
        String resolvedSourceBranch = branchCreationValidator.validateAndResolveSource(command, repository);
        BranchCreationContext context = BranchCreationContext.of(command, namespace, repository, resolvedSourceBranch);

        branchGitPort.createBranch(context);
        branchPort.save(Branch.create(command.repositoryId(), command.branchName()));
    }

    @Override
    public void deleteBranch(Long repositoryId, String branchName) {
        Repository repository = loadRepositoryWithWriteAccess(repositoryId);
        String namespace = repositoryNamespaceResolver.resolve(repository);
        Branch branch = loadBranch(repositoryId, branchName);
        branchCreationValidator.validateNotDefaultBranch(repository, branch);

        branchGitPort.deleteBranch(namespace, repository.getName().getValue(), branchName);
        branchPort.deleteByRepositoryIdAndName(repositoryId, branchName);
    }

    private Branch loadBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.BRANCH_NOT_FOUND,
                        "Branch not found: " + branchName));
    }

    private Repository loadRepositoryWithWriteAccess(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        "Repository not found: " + repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());

        return repository;
    }
}
```

#### TO-BE (개선 제안 구조)
```java
@Service
@RequiredArgsConstructor
public class BranchLoadService implements BranchLoadUseCase {

    private final BranchApplicationMapper branchApplicationMapper;
    private final BranchPersistencePort branchPort;

    @Override
    public List<BranchSearchResult> loadBranches(Long repositoryId) {
        return branchPort.findAllByRepositoryId(repositoryId)
                .stream()
                .map(branchApplicationMapper::toSearchResult)
                .toList();
    }

    @Override
    public BranchSearchResult loadBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .map(branchApplicationMapper::toSearchResult)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.BRANCH_NOT_FOUND,
                        "Branch not found: " + branchName));
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchCreationValidator branchCreationValidator;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final BranchGitPort branchGitPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryPersistencePort repositoryPort;

    @Override
    public void createBranch(BranchCreateCommand command) {
        BranchWriteContext context = loadWriteContext(command.repositoryId());
        String sourceBranch = branchCreationValidator.validateAndResolveSource(command, context.repository());
        BranchCreationContext creationContext = BranchCreationContext.of(
                command,
                context.namespace(),
                context.repository(),
                sourceBranch
        );

        branchGitPort.createBranch(creationContext);
        branchPort.save(Branch.create(command.repositoryId(), command.branchName()));
    }

    @Override
    public void deleteBranch(Long repositoryId, String branchName) {
        BranchWriteContext context = loadWriteContext(repositoryId);
        Branch branch = loadExistingBranch(repositoryId, branchName);

        branch.delete();
        branchGitPort.deleteBranch(
                context.namespace(),
                context.repository().getName().getValue(),
                branchName
        );
        branchPort.deleteByRepositoryIdAndName(repositoryId, branchName);
    }

    private BranchWriteContext loadWriteContext(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        "Repository not found: " + repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchWriteContext(repository, namespace);
    }

    private Branch loadExistingBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.BRANCH_NOT_FOUND,
                        "Branch not found: " + branchName));
    }

    private record BranchWriteContext(Repository repository, String namespace) {
    }
}
```

```java
public interface BranchLoadUseCase {
    List<BranchSearchResult> loadBranches(Long repositoryId);
    BranchSearchResult loadBranch(Long repositoryId, String branchName);
}
```

```java
public class Branch {

    public void delete() {
        if (defaultBranch) {
            throw new DefaultBranchDeletionNotAllowedException(name);
        }
    }
}
```

### 주의사항
- **포맷팅 금지**: 리팩토링 과정에서 코드 포맷팅 절대하지말것. 주로 코드의 기능과 구조를 개선하는 데 집중한다.
- **기존 기능 보장**: 리팩토링 후에도 기존의 기능이 정상적으로 동작하는지 확인하는 테스트가 필요하다.
- **계획우선**: 계획문서 작성중에 절대로 구현을 진행하지말것.
- **예시전체나열**: 변경하려는 목록의 BEFORE AFTER를 모두 나열한다.
- **문서체규약**: 모든 문장은 간결한 공식 문서체로 작성하고, 예시 코드는 상세히 작성한다.

### 결론
- Task 2.11은 `BranchService`의 조회/관리 책임과 `BranchManagementService` 내부 오케스트레이션을 정리하고, 기본 브랜치 삭제 규칙을 `Branch` 도메인으로 이동하는 작업으로 정의한다.
