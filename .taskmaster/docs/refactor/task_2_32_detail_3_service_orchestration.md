# Task 2.32 Detail 3: RepositoryOverviewService 이관과 Orchestration 재설계

### 목적
- 이 문서는 `RepositoryOverviewService`를 Repository Context service로 이동하는 절차를 정의한다.
- 핵심은 단순 package 이동이 아니라 inbound UseCase 간 의존성을 제거하는 것이다.

### 이동 대상
```text
FROM server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java
TO   server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryOverviewService.java
```

### AS-IS 의존성
```java
package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private static final String ROOT_PATH = "";

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final BranchLoadUseCase branchLoadUseCase;
    private final FileTreeLoadUseCase fileTreeLoadUseCase;
    private final CurrentUserPort currentUserPersistencePort;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;
}
```

### TO-BE 의존성
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private static final String ROOT_PATH = "";

    private final RepositoryQueryPort repositoryQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final FileGitPort fileGitPort;
    private final CurrentUserPort currentUserPort;
    private final GitRepositoryAccessService gitRepositoryAccessService;
}
```

### `getOverview` 구현
```java
@Override
public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {
    RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
            .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    return buildOverview(repository, branch);
}
```

### `getOverviewByPath` 개선 후보
```java
@Override
public RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch) {
    RepositoryResult repository = repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
            .orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
    return buildOverview(repository, branch);
}
```

### 공통 overview 조립
```java
private RepositoryOverviewResult buildOverview(RepositoryResult repository, String branch) {
    RepositoryKey key = resolveRepositoryKey(repository);
    List<BranchSearchResult> branches = branchQueryPort.findAllByRepositoryId(repository.id());
    String selectedBranch = resolveBranch(branch, branches);
    List<FileEntry> tree = fileGitPort.listTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);
    Long userId = currentUserPort.resolveCurrentUserId().orElse(null);
    RepositoryPermission permission = gitRepositoryAccessService.resolvePermission(
            null,
            key.namespace(),
            key.repoName(),
            userId);

    return new RepositoryOverviewResult(
            repository,
            branches,
            tree,
            selectedBranch,
            permission.role(),
            permission.writable());
}
```

### branch 선택 정책
```java
private String resolveBranch(String branch, List<BranchSearchResult> branches) {
    if (StringUtils.hasText(branch)) {
        return branch;
    }

    return branches.stream()
            .filter(BranchSearchResult::defaultBranch)
            .findFirst()
            .map(BranchSearchResult::name)
            .orElseGet(() -> branches.isEmpty() ? null : branches.get(0).name());
}
```

### repository path 해석 정책
```java
private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
    RepositoryKey key = RepositoryKey.fromPath(repository.clonePath());
    return key != null ? key : RepositoryKey.fromPath(repository.path());
}
```

### null path 위험
- 기존 코드도 `RepositoryKey`가 null이면 NPE가 발생할 수 있다.
- package 이관과 orchestration 재설계를 한 번에 수행하되, null path 정책 변경은 별도 판단이 필요하다.
- 명시 예외를 도입하려면 exception class, problem spec, handler mapping까지 확인해야 한다.

```java
private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
    RepositoryKey key = RepositoryKey.fromPath(repository.clonePath());
    if (key != null) {
        return key;
    }

    key = RepositoryKey.fromPath(repository.path());
    if (key != null) {
        return key;
    }

    throw new RepositoryPathUnresolvableException(repository.id());
}
```

### 완료 조건
- `RepositoryOverviewService`는 `repository.application.service`에 있다.
- `RepositoryOverviewService`는 다음 inbound UseCase를 주입하지 않는다.
  - `RepositoryLoadUseCase`
  - `BranchLoadUseCase`
  - `FileTreeLoadUseCase`
  - `GitRepositoryAccessUseCase`
- `RepositoryOverviewService`는 다음 collaborator를 사용한다.
  - `RepositoryQueryPort`
  - `BranchQueryPort`
  - `FileGitPort`
  - `CurrentUserPort`
  - `GitRepositoryAccessService`

