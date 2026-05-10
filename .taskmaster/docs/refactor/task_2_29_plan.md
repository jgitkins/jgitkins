# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.29 Repository Context domain aggregate/entity/VO 이관 계획서

### 배경
- 현재 `Branch`는 이미 `repository.domain` 하위로 이동했지만, `Repository` aggregate와 직접 연관된 도메인 타입은 아직 `server.domain.*`에 남아 있다.
- 이 상태는 `Repository Context`의 핵심 모델이 문맥상 분리된 상태라서 package ownership이 불명확하다.
- 다만 `OwnerId`, `OwnerType`, `OrganizeId`, `UserId`처럼 다른 context에서도 넓게 쓰는 식별자까지 한 번에 옮기면 범위가 과도하게 커진다.
- `BranchName`도 현재 `Repository`, `Job`, `PR snapshot`에서 함께 사용되므로 이번 단계의 이동 대상에서 제외한다.

### 한 줄 결론
- `Repository Context가 직접 소유하는 aggregate, relation model, repository, repository-prefixed VO만 repository.domain 하위로 이동하고, cross-context 식별자는 이번 단계에서 유지한다.`

### 목표
- `Repository` aggregate와 직접 연관된 repository-context 도메인 타입의 소속을 일치시킨다.
- `Repository Context` 전용 VO와 cross-context 식별자를 구분한다.
- `application`, `shared`, `infrastructure`, `test`의 import 경로를 새 domain package 기준으로 정리한다.
- `Branch` 이동 이후 남아 있던 repository domain의 package split을 닫는다.

### 범위
- `server/src/main/java/io/jgitkins/server/repository/domain/aggregate/Repository.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/repository/RepositoryRepository.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/model/RepositoryMember.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/event/RepositorySynchronizedEvent.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/vo/RepositoryId.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/vo/RepositoryName.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/vo/RepositoryPath.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/vo/RepositoryVisibility.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/vo/RepositoryMemberRole.java`
- 관련 mapper, adapter, service, validator, shared support, 테스트, package convention, 문서

### 이번 단계에서 유지
- `server/src/main/java/io/jgitkins/server/domain/model/vo/OwnerId.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/OwnerType.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeId.java`
- `server/src/main/java/io/jgitkins/server/domain/model/vo/UserId.java`

### 핵심 판단
- `Repository`, `RepositoryRepository`, `RepositoryMember`, `RepositorySynchronizedEvent`, `RepositoryId/Name/Path/Visibility`, `RepositoryMemberRole`는 `Repository Context`가 소유한다.
- `OwnerId`, `OwnerType`, `OrganizeId`, `UserId`는 다른 context에서도 직접 쓰므로 이번 단계에서 유지한다.
- `BranchName`은 repository 쪽으로 귀속시킬 여지는 있지만 `Job`, `PR snapshot` 등 영향 범위가 넓어서 이번 단계에서는 이동하지 않는다.
- `RepositorySynchronizedEvent`만 `repository.domain.event`로 이동하고, `DomainEvent` 인터페이스는 기존 `server.domain.event` 위치를 유지한다.
- 이번 작업은 aggregate 경계를 바꾸는 작업이 아니라 package ownership을 정리하는 작업이다.

### 목표 패키지 구조
```text
server/src/main/java/io/jgitkins/server/repository/
  domain/
    aggregate/
      Repository.java
    model/
      RepositoryMember.java
    repository/
      RepositoryRepository.java
    event/
      RepositorySynchronizedEvent.java
    vo/
      RepositoryId.java
      RepositoryName.java
      RepositoryPath.java
      RepositoryVisibility.java
      RepositoryMemberRole.java
```

### 이동하지 않는 이유
- `OwnerId`, `OwnerType`
  - repository가 사용하더라도 전용 개념이 아니라 owner 식별 공통 의미가 더 크다.
- `OrganizeId`
  - organize context의 canonical identifier다.
- `UserId`
  - repository member relation에서 쓰이지만 identity/access 전반에서 공통으로 사용한다.

### 단계별 계획
1. `Repository Context` 소유 타입과 유지 타입을 문서상 확정한다.
2. `Repository`, `RepositoryRepository`, `RepositoryMember`, `RepositorySynchronizedEvent`를 `repository.domain` 하위로 이동한다.
3. `RepositoryId`, `RepositoryName`, `RepositoryPath`, `RepositoryVisibility`, `RepositoryMemberRole`를 `repository.domain.vo`로 이동한다.
4. `application`, `shared`, `infrastructure`, `test` import를 새 package 기준으로 정리한다.
5. `BranchName`은 이번 단계에서 유지하고, 별도 task에서 재검토한다.
6. package convention, 문서, 테스트를 새 구조 기준으로 정리한다.

### 검증 기준
- `Repository Context` 핵심 도메인 타입이 `repository.domain` 하위에 정렬돼 있어야 한다.
- `OwnerId`, `OwnerType`, `OrganizeId`, `UserId`는 기존 위치를 유지해야 한다.
- `RepositoryRepository`와 `RepositoryPersistenceAdapter`가 새 domain package를 기준으로 연결돼야 한다.
- `RepositoryMemberPersistenceAdapter`, `RepositoryMemberDomainMapper`가 새 model/vo 경로를 사용해야 한다.
- `shared`와 다른 context는 repository 전용 VO만 참조하고, cross-context 식별자는 기존 위치를 유지해야 한다.
- 직접 영향 테스트는 최소한 `RepositoryTest`, `RepositoryValueObjectTest`, `RepositoryProvisionerTest`, `PushEventCommandResolverTest`, `BranchCreationValidatorTest`, `PullRequestServiceTest`, `ArchitecturePackageConventionTest`를 포함해야 한다.

### 예시 코드

#### 1. 현재 상태 예시
```java
package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.RepositorySynchronizedEvent;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;

public class Repository extends AbstractAggregateRoot<RepositoryId> {
    // ...
}
```

```java
package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;

public interface RepositoryRepository {
    Repository save(Repository repository);
    Optional<Repository> findById(RepositoryId id);
    Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name);
    Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path);
}
```

```java
package io.jgitkins.server.domain.model;

import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;

public class RepositoryMember {
    private final RepositoryId repositoryId;
    private final UserId userId;
    private final RepositoryMemberRole role;
}
```

#### 2. 목표 상태 예시
```java
package io.jgitkins.server.repository.domain.aggregate;

import io.jgitkins.server.domain.aggregate.AbstractAggregateRoot;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.event.RepositorySynchronizedEvent;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Repository extends AbstractAggregateRoot<RepositoryId> {

    private final RepositoryId id;
    private final OwnerType ownerType;
    private final OwnerId ownerId;
    private final RepositoryName name;
    private final RepositoryPath path;
    private final BranchName defaultBranch;
    private final RepositoryVisibility visibility;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String credentialId;
    private final String clonePath;
    private final LocalDateTime lastSyncedAt;
    private final boolean requiresInitialContent;
    private final boolean initialized;

    private Repository(
            RepositoryId id,
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name,
            RepositoryPath path,
            BranchName defaultBranch,
            RepositoryVisibility visibility,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String credentialId,
            String clonePath,
            LocalDateTime lastSyncedAt,
            boolean requiresInitialContent,
            boolean initialized
    ) {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.name = name;
        this.path = path;
        this.defaultBranch = defaultBranch;
        this.visibility = visibility;
        this.description = description != null ? description.trim() : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.credentialId = credentialId;
        this.clonePath = clonePath;
        this.lastSyncedAt = lastSyncedAt;
        this.requiresInitialContent = requiresInitialContent;
        this.initialized = initialized;
    }

    public static Repository create(
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name,
            RepositoryPath path,
            BranchName defaultBranch,
            RepositoryVisibility visibility,
            String description,
            String clonePath,
            String credentialId,
            boolean requiresInitialContent
    ) {
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
                requiresInitialContent,
                false
        );
    }

    public Repository withIdentity(RepositoryId repositoryId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Repository identified = new Repository(
                repositoryId,
                ownerType,
                ownerId,
                name,
                path,
                defaultBranch,
                visibility,
                description,
                createdAt,
                updatedAt,
                credentialId,
                clonePath,
                lastSyncedAt,
                requiresInitialContent,
                initialized
        );
        identified.copyDomainEventsFrom(this);
        return identified;
    }

    public Repository markInit(LocalDateTime syncedAt) {
        LocalDateTime effectiveSyncedAt = syncedAt != null ? syncedAt : LocalDateTime.now();
        Repository marked = new Repository(
                id,
                ownerType,
                ownerId,
                name,
                path,
                defaultBranch,
                visibility,
                description,
                createdAt,
                effectiveSyncedAt,
                credentialId,
                clonePath,
                effectiveSyncedAt,
                false,
                true
        );
        marked.copyDomainEventsFrom(this);
        marked.registerEvent(RepositorySynchronizedEvent.from(marked));
        return marked;
    }

    public static Repository rehydrate(
            RepositoryId repositoryId,
            OwnerType ownerType,
            OwnerId ownerId,
            RepositoryName name,
            RepositoryPath path,
            BranchName defaultBranch,
            RepositoryVisibility visibility,
            String description,
            String clonePath,
            String credentialId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastSyncedAt
    ) {
        return new Repository(
                repositoryId,
                ownerType,
                ownerId,
                name,
                path,
                defaultBranch,
                visibility,
                description,
                createdAt,
                updatedAt,
                credentialId,
                clonePath,
                lastSyncedAt,
                lastSyncedAt == null,
                lastSyncedAt != null
        );
    }

    public OrganizeId getOrganizeId() {
        if (ownerType == OwnerType.ORGANIZATION && ownerId != null) {
            return OrganizeId.of(ownerId.getValue());
        }
        return null;
    }
}
```

```java
package io.jgitkins.server.repository.domain.repository;

import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;

import java.util.Optional;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);

    Optional<Repository> findById(RepositoryId id);

    Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name);

    Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path);

    Optional<Repository> findByClonePath(String clonePath);

    Optional<Repository> findByPath(String path);
}
```

```java
package io.jgitkins.server.repository.domain.model;

import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RepositoryMember {

    private final RepositoryId repositoryId;
    private final UserId userId;
    private final RepositoryMemberRole role;
    private final LocalDateTime addedAt;

    public static RepositoryMember create(
            RepositoryId repositoryId,
            UserId userId,
            RepositoryMemberRole role,
            LocalDateTime addedAt
    ) {
        if (repositoryId == null || userId == null || role == null) {
            throw new IllegalArgumentException("RepositoryMember requires repositoryId, userId and role");
        }
        return new RepositoryMember(
                repositoryId,
                userId,
                role,
                addedAt != null ? addedAt : LocalDateTime.now()
        );
    }
}
```

```java
package io.jgitkins.server.repository.domain.event;

import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepositorySynchronizedEvent implements io.jgitkins.server.domain.event.DomainEvent {

    private final RepositoryId repositoryId;
    private final RepositoryName name;
    private final RepositoryPath path;
    private final LocalDateTime syncedAt;
    private final Instant occurredAt;

    public static RepositorySynchronizedEvent from(Repository repository) {
        return new RepositorySynchronizedEvent(
                repository.getId(),
                repository.getName(),
                repository.getPath(),
                repository.getLastSyncedAt(),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
```

#### 3. 유지하는 식별자 예시
```java
package io.jgitkins.server.domain.model.vo;

public enum OwnerType {
    USER,
    ORGANIZATION
}
```

```java
package io.jgitkins.server.shared.application.support;

import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.aggregate.Repository;

public class RepositoryNamespaceResolver {

    public String resolve(Repository repository) {
        return resolve(repository.getOwnerType(), repository.getOwnerId());
    }

    public String resolve(OwnerType ownerType, OwnerId ownerId) {
        // owner identity는 cross-context 식별자이므로 기존 위치 유지
        return "...";
    }
}
```

#### 4. Persistence adapter 예시
```java
package io.jgitkins.server.repository.infrastructure.adapter.persistence;

import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.infrastructure.mapper.RepositoryDomainMapper;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RepositoryPersistenceAdapter implements RepositoryRepository, RepositoryQueryPort {

    @Override
    public Repository save(Repository repository) {
        // aggregate persistence
        return repository;
    }

    @Override
    public Optional<Repository> findById(RepositoryId id) {
        // aggregate restore
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name) {
        return Optional.empty();
    }

    @Override
    public Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path) {
        return Optional.empty();
    }

    @Override
    public java.util.List<RepositoryResult> loadVisibleRepositories(Long requesterId) {
        // read model query
        return java.util.List.of();
    }
}
```

### 완료 기준
- `Repository Context` 핵심 도메인 타입이 `repository.domain`으로 정렬된다.
- `shared`와 다른 context는 새 repository domain 타입을 정상 참조한다.
- cross-context 식별자는 기존 위치를 유지한다.
- `compileJava`, `compileTestJava`, repository 관련 단위 테스트, package convention 테스트가 통과한다.
