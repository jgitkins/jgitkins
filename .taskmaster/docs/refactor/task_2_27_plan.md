# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.27 Repository aggregate load / read model query seam 재설계 계획서

### 배경
- `2.26`에서 `RepositoryPersistencePort`를 `RepositoryRepository`와 `RepositoryQueryPort`로 분리했지만, aggregate 복원 책임과 순수 조회 책임이 아직 최종 형태로 닫히지 않았다.
- 현재 판단은 `Repository` aggregate 자체를 복원하는 메서드는 `domain.repository.RepositoryRepository`가 소유해야 한다.
- 반대로 단순 조회 시나리오는 aggregate를 application에서 다시 DTO로 매핑하기보다, read model을 직접 반환하는 query seam으로 가는 편이 더 자연스럽다.

### 목표
- `RepositoryRepository`에 aggregate 복원 메서드를 재귀속한다.
- 순수 조회는 `RepositoryQueryPort`로 재정의해 application read model을 직접 반환하게 한다.
- aggregate seam과 read model seam의 호출 기준을 서비스별로 명확히 고정한다.
- `2.26` 커밋을 checkpoint로 삼고, 다음 구현은 이 방향으로 재정렬한다.

### 용어 정리
- `RepositoryRepository`
  - `Repository` aggregate 저장/삭제/복원 저장소
  - 도메인 규칙이 필요한 흐름에서 사용
- `RepositoryQueryPort`
  - read-only 조회 전용 outgoing port
  - `RepositoryResult` 또는 별도 read DTO를 직접 반환
- `Aggregate Load`
  - 후속 도메인 연산을 위해 `Repository` aggregate 자체를 복원하는 조회
- `Read Model Query`
  - 화면/API 응답 조립을 위해 필요한 projection/DTO 조회

### 선택 방안
- **채택안**
- `2.26`에서 도입한 aggregate 반환 `RepositoryQueryPort`는 먼저 `RepositoryAggregateQueryPort` 같은 임시 이름으로 치환한다.
- 그 다음 read model 반환 전용 새 `RepositoryQueryPort`를 추가한다.
- 호출부 이관이 끝나면 aggregate 조회는 `RepositoryRepository`로 정리하고, 임시 aggregate query port는 제거한다.
- `RepositoryRepository`
  - `save`, `update`, `deleteById`
  - `findById`
  - `findByOwnerAndName`
  - `findByOwnerAndPath`
  - `findByClonePath`
  - aggregate 복원이 필요한 최소 메서드
- `RepositoryQueryPort`
  - `RepositoryResult` 계열 조회
  - lightweight read predicate/count query 포함
  - owner별 목록, 공개/가시성 필터 조회, path 기반 화면용 read model 조회
  - 특히 기존 `loadRepositories()`처럼 aggregate 전체를 메모리에서 필터링하는 구조를 대체해야 한다.
- aggregate가 필요한 서비스와 read model만 필요한 서비스는 다른 포트를 보게 한다.

### 범위
- `server/src/main/java/io/jgitkins/server/domain/repository/RepositoryRepository.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryQueryPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryLoadService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/RepositoryLookupService.java`
- `server/src/main/java/io/jgitkins/server/application/validate/RepositoryValidator.java`
- `server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `server/src/main/java/io/jgitkins/server/application/validate/ActivationValidator.java`
- `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/RepositoryPersistenceAdapter.java`
- 관련 mapper, 테스트, package convention

### 핵심 판단
- `RepositoryManagementService`, `RepositoryLookupService`, `PullRequestService`, `RepositoryValidator`
  - 도메인 연산 또는 aggregate 식별이 필요하므로 `RepositoryRepository`를 사용한다.
- `RepositoryLoadService`, 이후 web/REST read 시나리오
  - `RepositoryResult` 같은 application read model을 직접 받도록 설계한다.
- `ActivationValidator.validateUserHasNoRepositories(...)`
  - aggregate 복원이 필요 없으므로 read/query seam으로 남기는 것이 맞다.

### 목표 패키지 방향
- `server/src/main/java/io/jgitkins/server/domain/repository/RepositoryRepository.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryQueryPort.java`
- 필요 시 aggregate 조회 전용 보조 port는 `RepositoryLookupPort`처럼 별도 분리 검토

### 메서드 분류 기준
- `RepositoryRepository`
  - `Repository save(Repository repository)`
  - `Repository update(Repository repository)`
  - `void deleteById(RepositoryId id)`
  - `Optional<Repository> findById(RepositoryId id)`
  - `Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name)`
  - `Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path)`
  - `Optional<Repository> findByClonePath(String clonePath)`

- `RepositoryQueryPort`
  - `Optional<RepositoryResult> loadRepository(Long repositoryId)`
  - `Optional<RepositoryResult> loadRepositoryByPath(String namespace, String repoName)`
  - `List<RepositoryResult> loadVisibleRepositories(Long requesterId)`
  - `List<RepositoryResult> loadUserRepositories(String username, Long requesterId)`
  - `long countByOwner(OwnerType ownerType, OwnerId ownerId)`
  - 필요 시 `boolean existsVisibleRepository(...)` 같은 lightweight predicate query도 같은 포트에 둔다.
  - `loadVisibleRepositories`는 visibility/owner/membership 조건을 query 단계에서 반영하는 방향으로 설계한다.

### 단계별 계획
1. `RepositoryRepository`에 aggregate 복원 메서드를 되돌린다.
2. `RepositoryQueryPort`는 aggregate 반환 port가 아니라, read model query port로 재정의한다.
3. `RepositoryLoadService`를 aggregate 조회 기반에서 read model query 기반으로 전환한다.
4. `RepositoryLookupService`, `PullRequestService`, `RepositoryValidator`는 `RepositoryRepository` 의존으로 되돌린다.
5. `ActivationValidator` 같은 순수 조회성 검증은 read model/query seam으로 이동한다.
6. adapter와 mapper를 새 기준에 맞춘다.

### 점진 이관 순서
1. 기존 aggregate 반환 `RepositoryQueryPort`를 `RepositoryAggregateQueryPort` 같은 임시 이름으로 먼저 분리한다.
2. `RepositoryRepository` 시그니처를 aggregate 복원 기준으로 재정의한다.
3. 새 read model 반환 `RepositoryQueryPort`를 추가한다.
4. persistence adapter에 aggregate/query 구현 분기를 반영한다.
5. `RepositoryLoadService`를 새 `RepositoryQueryPort` 기준으로 교체한다.
6. aggregate 의존 service/validator를 `RepositoryRepository` 기준으로 되돌린다.
7. 임시 aggregate query port를 제거하고 테스트/아키텍처 규칙을 검증한다.

### 검증 기준
- `RepositoryLookupService`는 aggregate 복원용 `RepositoryRepository`를 사용해야 한다.
- `PullRequestService`는 PR 대상 repository 복원 시 `RepositoryRepository`를 사용해야 한다.
- `RepositoryValidator`는 이름 중복 확인 시 `RepositoryRepository`를 사용해야 한다.
- `RepositoryLoadService`는 aggregate를 받아 mapper를 거치는 대신 `RepositoryQueryPort`를 사용해야 한다.
- `ActivationValidator`는 aggregate 복원 없이 동작해야 한다.

### 예시 코드

#### 1. RepositoryRepository 예시
```java
package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import java.util.Optional;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);

    Optional<Repository> findById(RepositoryId id);

    Optional<Repository> findByOwnerAndName(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name
    );

    Optional<Repository> findByOwnerAndPath(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryPath path
    );

    Optional<Repository> findByClonePath(String clonePath);
}
```

#### 2. RepositoryQueryPort 예시
```java
package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import java.util.List;
import java.util.Optional;

public interface RepositoryQueryPort {

    Optional<RepositoryResult> loadRepository(Long repositoryId);

    Optional<RepositoryResult> loadRepositoryByPath(String namespace, String repoName);

    List<RepositoryResult> loadVisibleRepositories(Long requesterId);

    List<RepositoryResult> loadUserRepositories(String username, Long requesterId);

    long countByOwner(OwnerType ownerType, OwnerId ownerId);
}
```

`RepositoryQueryPort`의 접두사는 `get`이 아니라 `load`를 유지한다. 이유는 현재 코드베이스가 `RepositoryLoadUseCase`, `BranchLoadUseCase`, `RepositoryLoadService`처럼 조회 흐름을 `load`로 통일하고 있고, `get`은 단순 접근자나 존재 보장 뉘앙스를 주기 쉽기 때문이다. 이번 단계의 목적도 query seam을 read 흐름으로 정리하는 것이므로 `load*` 계열이 더 일관적이다.

#### 3. RepositoryLoadService 예시
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryQueryPort repositoryQueryPort;
    private final CurrentUserPort currentUserPort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        return repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepositoryByPath(String namespace, String repoName) {
        return repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
                .orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadRepositories() {
        Long requesterId = currentUserPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadVisibleRepositories(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(String username) {
        Long requesterId = currentUserPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadUserRepositories(username, requesterId);
    }
}
```

#### 4. RepositoryLookupService 예시
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
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryLookupService {

    private final RepositoryRepository repositoryRepository;
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
        return repositoryRepository.findByClonePath(clonePath);
    }

    private Optional<Repository> findUserOwned(String namespace, String repoName) {
        Optional<User> user = userPort.findByUsername(namespace);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        return repositoryRepository.findByOwnerAndName(
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

        return repositoryRepository.findByOwnerAndPath(
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

#### 5. RepositoryQueryAdapter 예시
```java
package io.jgitkins.server.repository.infrastructure.adapter.persistence;

import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryQueryAdapter implements RepositoryQueryPort {

    @Override
    public Optional<RepositoryResult> loadRepository(Long repositoryId) {
        return Optional.empty();
    }

    @Override
    public Optional<RepositoryResult> loadRepositoryByPath(String namespace, String repoName) {
        return Optional.empty();
    }

    @Override
    public List<RepositoryResult> loadVisibleRepositories(Long requesterId) {
        return List.of();
    }

    @Override
    public List<RepositoryResult> loadUserRepositories(String username, Long requesterId) {
        return List.of();
    }

    @Override
    public long countByOwner(OwnerType ownerType, OwnerId ownerId) {
        return 0L;
    }
}
```

### 완료 기준
- `RepositoryRepository`가 aggregate 복원 메서드를 다시 소유해야 한다.
- `RepositoryLoadService`는 aggregate를 받아 mapper를 거치는 대신 read model query seam을 사용해야 한다.
- `RepositoryLookupService`, `PullRequestService`, `RepositoryValidator`는 `RepositoryRepository`로 정리되어야 한다.
- `ActivationValidator` 등 순수 조회성 로직은 aggregate 복원 없이 동작해야 한다.
