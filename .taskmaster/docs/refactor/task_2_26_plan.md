# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.26 Repository Persistence/Query/Git 포트 분리 계획서

### 배경
- `RepositoryPersistencePort`가 aggregate 저장/삭제와 조회 seam을 함께 갖고 있다.
- 현재 `RepositoryLoadService`, `RepositoryLookupService`, `RepositoryValidator`, `RepositoryManagementService`가 같은 포트에 서로 다른 의도로 의존한다.
- 반면 git 외부 상태 변경은 이미 `RepositoryGitPort`, `BranchGitPort`, `CommitGitPort`로 분리되어 있어, persistence/query 분리와 같은 수준의 경계 정리가 필요하다.

### 목표
- aggregate 생명주기 저장소는 `RepositoryRepository`로 분리한다.
- 조회 seam은 `RepositoryQueryPort`로 분리한다.
- repository-level git seam은 기존 `RepositoryGitPort`를 유지하되 책임을 명확히 고정한다.
- 호출부가 `쓰기 규칙 확인`, `조회`, `git external state`를 서로 다른 포트로 의존하도록 정리한다.

### 용어 정리
- `RepositoryRepository`
  - aggregate 저장/복원과 쓰기 규칙 확인에 필요한 최소 저장소
- `RepositoryQueryPort`
  - 조회 전용 outgoing port
  - lookup/read model 구성/카운트/식별자 해석용 메서드를 담당
- `RepositoryGitPort`
  - repository-level git seam
  - bare repository 초기화, 삭제, HEAD 참조 갱신만 담당

### 선택 방안
- `RepositoryPersistencePort`는 유지하지 않는다.
- `RepositoryRepository` 하나로 모든 메서드를 옮기지 않는다.
- **채택안**: aggregate 저장소 / query port / repository-level git port 분리
- `save`, `update`, `deleteById`는 `RepositoryRepository`
- `findById`, `findAll`, `findAllByOwner`, `findByClonePath`, `findByPath`, `findByOwnerAndPath`, `findByOwnerAndName`, `findIdByOwnerAndName`, `countByOwner`는 `RepositoryQueryPort`
- `RepositoryGitPort`는 이미 존재하므로 새로 확장하지 않고 현재 역할을 유지한다.
- `BranchGitPort`, `CommitGitPort`는 그대로 분리 유지한다.

### 범위
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryPersistencePort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryGitPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/RepositoryLookupService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryLoadService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/provisioning/RepositoryProvisioner.java`
- `server/src/main/java/io/jgitkins/server/application/validate/RepositoryValidator.java`
- `server/src/main/java/io/jgitkins/server/application/support/PushEventCommandResolver.java`
- `server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/RepositoryPersistenceAdapter.java`
- 관련 서비스/validator/support 테스트

### 핵심 판단
- `RepositoryRepository`
  - aggregate 저장소다.
  - 저장/업데이트/삭제만 담당한다.
- `RepositoryQueryPort`
  - 조회/탐색/lookup seam이다.
  - 단건 조회, 목록 조회, clone path/path 기반 해석, owner 기준 카운트/식별자 조회를 담당한다.
- `RepositoryGitPort`
  - repository-level git seam만 담당한다.
  - `BranchGitPort`, `CommitGitPort`를 흡수하지 않는다.

### 목표 패키지 방향
- `server/src/main/java/io/jgitkins/server/domain/repository/RepositoryRepository.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryQueryPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryGitPort.java`

### 메서드 분류 기준
- `RepositoryRepository`
  - `Repository save(Repository repository)`
  - `Repository update(Repository repository)`
  - `void deleteById(RepositoryId id)`

- `RepositoryQueryPort`
  - `Optional<Repository> findById(RepositoryId id)`
  - `List<Repository> findAll()`
  - `Optional<Repository> findByClonePath(String clonePath)`
  - `Optional<Repository> findByPath(String path)`
  - `Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path)`
  - `Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name)`
  - `Optional<Long> findIdByOwnerAndName(OwnerType ownerType, OwnerId ownerId, String repoName)`
  - `List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId)`
  - `long countByOwner(OwnerType ownerType, OwnerId ownerId)`

### 단계별 계획
1. `RepositoryRepository`, `RepositoryQueryPort`를 추가한다.
2. `RepositoryPersistenceAdapter`가 두 인터페이스를 함께 구현하도록 변경한다.
3. 쓰기 흐름 호출부는 `RepositoryRepository`로, 조회 흐름 호출부는 `RepositoryQueryPort`로 의존을 교체한다.
4. `RepositoryPersistencePort`를 제거한다.
5. 테스트와 package convention을 새 기준으로 정리한다.

### 점진 이관 순서
1. 포트/저장소 인터페이스 추가
2. adapter 다중 구현 전환
3. `RepositoryManagementService`, `RepositoryProvisioner` 교체
4. `RepositoryLoadService`, `RepositoryLookupService`, `PushEventCommandResolver`, `PullRequestService`, `RepositoryValidator` 교체
5. 구 포트 제거
6. 테스트/아키텍처 규칙 검증

### 검증 기준
- `RepositoryManagementService`는 쓰기에는 `RepositoryRepository`, 삭제 전 조회에는 `RepositoryQueryPort`를 사용해야 한다.
- `RepositoryLookupService`와 `RepositoryLoadService`는 `RepositoryQueryPort`에 의존해야 한다.
- `RepositoryValidator`는 중복 이름 확인 때문에 `RepositoryQueryPort`에 의존해야 한다.
- `RepositoryProvisioner`는 persistence update에는 `RepositoryRepository`, git 초기화/삭제는 `RepositoryGitPort`를 사용해야 한다.
- `RepositoryGitPort`는 repository-level git seam만 유지해야 하며 branch/commit 메서드가 추가되면 안 된다.

### 예시 코드

#### 1. RepositoryRepository 예시
```java
package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);
}
```

#### 2. RepositoryQueryPort 예시
```java
package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import java.util.List;
import java.util.Optional;

public interface RepositoryQueryPort {

    Optional<Repository> findById(RepositoryId id);

    List<Repository> findAll();

    Optional<Repository> findByClonePath(String clonePath);

    Optional<Repository> findByPath(String path);

    Optional<Repository> findByOwnerAndPath(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryPath path
    );

    Optional<Repository> findByOwnerAndName(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name
    );

    Optional<Long> findIdByOwnerAndName(
            OwnerType ownerType,
            OwnerId ownerId,
            String repoName
    );

    List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId);

    long countByOwner(OwnerType ownerType, OwnerId ownerId);
}
```

`RepositoryQueryPort`는 1차 단계에서 `application DTO`가 아니라 `Repository Aggregate`를 반환한다. 이유는 현재 호출부가 단순 조회 렌더링만 하는 것이 아니라 `RepositoryLookupService`, `RepositoryAccessibilityService`, `RepositoryValidator`, `PullRequestService`처럼 도메인 규칙과 식별 해석에 aggregate 정보를 그대로 사용하기 때문이다. DTO 반환으로 바꾸면 query port가 read model seam을 넘어서 application mapping 정책까지 소유하게 된다. 따라서 이번 단계에서는 `Aggregate 반환 + application service에서 DTO 변환`을 유지하고, 장기적으로 read model 전용 요구가 커질 때 별도 query result seam을 추가한다.

#### 3. RepositoryGitPort 유지 기준 예시
```java
package io.jgitkins.server.repository.application.port.out;

public interface RepositoryGitPort {

    void initialize(String namespace, String repoName);

    void deleteRepository(String namespace, String repoName);

    void updateHeadReference(String namespace, String repoName, String branch);
}
```

`RepositoryGitPort`는 repository-level git seam만 가진다. `createBranch`, `deleteBranch`, `loadCommit`, `commit` 같은 branch/commit 책임은 넣지 않는다. 해당 책임은 이미 `BranchGitPort`, `CommitGitPort`가 맡고 있고, 이를 다시 합치면 repository git port가 외부 git 조작 전부를 먹는 거대한 포트가 된다.

#### 4. RepositoryPersistenceAdapter 예시
```java
package io.jgitkins.server.repository.infrastructure.adapter.persistence;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryPersistenceAdapter
        implements RepositoryRepository, RepositoryQueryPort {

    @Override
    public Repository save(Repository repository) {
        // insert
        return null;
    }

    @Override
    public Repository update(Repository repository) {
        // update
        return null;
    }

    @Override
    public void deleteById(RepositoryId id) {
        // delete
    }

    @Override
    public Optional<Repository> findById(RepositoryId id) {
        // select by id
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByOwnerAndName(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name
    ) {
        // select by owner and repository name
        return Optional.empty();
    }

    @Override
    public List<Repository> findAll() {
        return List.of();
    }

    @Override
    public Optional<Repository> findByClonePath(String clonePath) {
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByPath(String path) {
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByOwnerAndPath(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryPath path
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> findIdByOwnerAndName(
            OwnerType ownerType,
            OwnerId ownerId,
            String repoName
    ) {
        return Optional.empty();
    }

    @Override
    public List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId) {
        return List.of();
    }

    @Override
    public long countByOwner(OwnerType ownerType, OwnerId ownerId) {
        return 0L;
    }
}
```

1차 단계에서는 adapter를 하나로 유지해도 된다. 포트 분리는 의존 방향과 책임 경계를 위한 것이지, 구현 클래스 개수를 반드시 늘려야 한다는 뜻은 아니다. 현재 `RepositoryPersistenceAdapter`는 같은 DB/MBG mapper와 같은 `RepositoryEntity`를 사용하므로, `RepositoryRepository`와 `RepositoryQueryPort`를 함께 구현하는 편이 구현 비용과 변경 리스크가 낮다. 다만 이후 query 최적화, projection, read replica, 별도 read mapper가 필요해지면 그 시점에 query adapter를 분리한다.

#### 5. RepositoryManagementService 예시
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryManagementUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryRepository repositoryRepository;
    private final RepositoryQueryPort repositoryQueryPort;
    private final RepositoryOwnershipPolicy repositoryOwnershipPolicy;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        RepositoryCreationPlan creationPlan = repositoryOwnershipPolicy.prepareCreation(command);
        Repository saved = repositoryRepository.save(creationPlan.repository());
        Repository provisioned = repositoryProvisioner.provision(saved, creationPlan.initialCommitOptions());
        return repositoryApplicationMapper.toDto(provisioned);
    }

    @Override
    @Transactional
    public void deleteRepository(Long repositoryId) {
        RepositoryId id = RepositoryId.of(repositoryId);
        Repository repository = repositoryQueryPort.findById(id)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        repositoryOwnershipPolicy.validateDeletion(repository);
        repositoryProvisioner.delete(repository);
        repositoryRepository.deleteById(id);
    }
}
```

#### 6. RepositoryLoadService / RepositoryLookupService 예시
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.shared.application.support.RepositoryAccessibilityService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryAccessibilityService repositoryAccessibilityService;
    private final RepositoryQueryPort repositoryQueryPort;
    private final CurrentUserPort currentUserPersistencePort;
    private final UserPersistencePort userPort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        Repository repository = repositoryQueryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        return repositoryApplicationMapper.toDto(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadRepositories() {
        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        Map<OrganizeId, Boolean> membershipCache = new HashMap<>();

        return repositoryQueryPort.findAll().stream()
                .filter(repo -> repositoryAccessibilityService.isVisibleToRequester(repo, requesterId, membershipCache))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(String username) {
        String normalizedUsername = username != null ? username.trim() : "";

        Long ownerId = userPort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + normalizedUsername));

        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        return repositoryQueryPort.findAllByOwner(OwnerType.USER, OwnerId.of(ownerId)).stream()
                .filter(repo -> repositoryAccessibilityService.isVisibleToUserOwner(repo, requesterId, ownerId))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }
}
```

```java
package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryLookupService {

    private final RepositoryQueryPort repositoryQueryPort;
    private final UserPersistencePort userPort;
    private final OrganizePersistencePort organizePort;

    public Optional<Repository> resolveByPath(String namespace, String repoName) {
        String normalizedNamespace = trimSlashes(namespace);
        String normalizedRepoName = trimSlashes(repoName);

        return findByClonePath(normalizedNamespace, normalizedRepoName)
                .or(() -> findUserOwned(normalizedNamespace, normalizedRepoName))
                .or(() -> findOrganizationOwned(normalizedNamespace, normalizedRepoName));
    }

    private Optional<Repository> findByClonePath(String namespace, String repoName) {
        String clonePath = RepositoryPathHelper.buildClonePath(namespace, repoName);
        return repositoryQueryPort.findByClonePath(clonePath);
    }

    private Optional<Repository> findUserOwned(String namespace, String repoName) {
        Optional<User> user = userPort.findByUsername(namespace);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        return repositoryQueryPort.findByOwnerAndName(
                OwnerType.USER,
                OwnerId.of(user.get().getId()),
                RepositoryName.from(repoName)
        );
    }

    private Optional<Repository> findOrganizationOwned(String namespace, String repoName) {
        Optional<Organize> organize = findOrganizationByNamespace(namespace);
        if (organize.isEmpty()) {
            return Optional.empty();
        }

        return repositoryQueryPort.findByOwnerAndPath(
                OwnerType.ORGANIZATION,
                OwnerId.of(organize.get().getId().getValue()),
                RepositoryPath.from(repoName)
        );
    }

    private Optional<Organize> findOrganizationByNamespace(String namespace) {
        try {
            return organizePort.findByName(OrganizeName.from(namespace));
        } catch (IllegalArgumentException ex) {
            log.debug("invalid organization namespace. namespace={}", namespace, ex);
            return Optional.empty();
        }
    }

    private String trimSlashes(String value) {
        if (value == null) {
            throw new IllegalArgumentException("path segment must not be null");
        }
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
```

#### 7. RepositoryValidator / RepositoryProvisioner 예시
```java
package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.exception.RepositoryAlreadyExistsException;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryValidator {

    private final RepositoryQueryPort repositoryQueryPort;
    private final OrganizeMemberPersistencePort organizeMemberPort;
    private final CurrentUserPort currentUserPersistencePort;

    public void validateRepositoryNameUnique(OwnerType ownerType, OwnerId ownerId, RepositoryName name) {
        repositoryQueryPort.findByOwnerAndName(ownerType, ownerId, name)
                .ifPresent(existing -> {
                    throw new RepositoryAlreadyExistsException(
                            "Repository name already exists for owner: " + name.getValue());
                });
    }
}
```

```java
package io.jgitkins.server.repository.application.support.provisioning;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.common.factory.CommitFileFactory;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.repository.BranchRepository;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryGitPort;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryRepository repositoryRepository;
    private final BranchRepository branchRepository;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    public Repository provision(Repository repository, InitialCommitOptions initialCommitOptions) {
        initializeGitRepository(repository);
        createDefaultBranch(repository);
        return initializeContentIfNeeded(repository, initialCommitOptions);
    }

    private Repository initializeContentIfNeeded(Repository repository, InitialCommitOptions initialCommitOptions) {
        if (initialCommitOptions == null || !initialCommitOptions.requiresInitialContent()) {
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
                initialCommitOptions.commitMessage(),
                initialCommitOptions.authorName(),
                initialCommitOptions.authorEmail(),
                files
        );
        repositoryGitPort.updateHeadReference(namespace, repoName, branchName);

        return repositoryRepository.update(repository.markInit(LocalDateTime.now()));
    }
}
```

### 완료 기준
- `RepositoryPersistencePort`가 제거되어야 한다.
- persistence adapter는 `RepositoryRepository`, `RepositoryQueryPort`를 함께 구현해야 한다.
- repository-level git seam은 `RepositoryGitPort`에만 남고, branch/commit 책임은 합쳐지지 않아야 한다.
- 관련 서비스/validator/support 테스트가 새 포트 기준으로 통과해야 한다.
