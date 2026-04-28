# Repository Access Resolution

## 핵심
- repository 식별과 permission 계산을 분리한다.
- namespace 충돌 정책을 lookup과 동일하게 맞춘다.

## 정리 방향
- repository 식별은 lookup/resolver에 위임한다.
- `GitRepositoryAccessService`는 permission 계산 중심으로 축소한다.
- `RepositoryPermission`은 `repository.application.result`로 분리한다.
- 현재 문서 기준으로 `GitRepositoryAccessService`의 `UseCase` 의존은 제거 방향으로 확정한다.
- 이 클래스는 외부 진입점이 아니라 내부 권한 계산 협력자로 본다.
- 외부 진입 계약이 필요하면 별도 facade 또는 dedicated use case를 두고, 내부 계산기는 분리한다.

## 책임 분리 계획
- `GitRepositoryAccessService`는 최종적으로 `Repository -> Permission` 계산기 역할만 가진다.
- `OwnerType + ownerName + repositoryName -> Repository` 해석은 lookup 계층에서 수행한다.
- public visibility, owner, repository member, organization member의 우선순위를 하나의 permission matrix로 고정한다.
- `resolveVisibility(...)`가 계속 필요하다면 permission 계산과는 별도 façade로 둘지 검토한다.
- `GitRepositoryAccessUseCase` 구현은 제거하고, 내부 application collaborator로 재정의한다.

## 단계별 실행 계획
1. `GitRepositoryAccessUseCase` 구현 제거 여부를 실제 호출부 기준으로 확정하고, 내부 협력자 방향으로 정리한다.
2. `resolveRepository(...)`의 내부 분기에서 직접 repository를 찾는 로직을 제거한다.
3. `ownerType == null` 분기와 namespace 충돌 해석을 `RepositoryLookupService` 위임으로 단일화한다.
4. `canRead`, `canWrite`, `resolveVisibility`, `resolvePermission` 간 중복 조건을 permission 계산 기준으로 정리한다.
5. `RepositoryPermission`을 facade 내부 record에서 분리해 dedicated result로 고정한다.
6. access 관련 테스트를 `anonymous`, `public`, `owner`, `repository member`, `organization member`, `namespace 충돌` 기준으로 재고정한다.

## 예시 코드
```java
package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.OrganizeMember;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.repository.application.result.RepositoryPermission;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitRepositoryAccessService {

    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final OrganizeMemberPersistencePort organizeMemberPort;

    public boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return resolvePermission(ownerType, ownerName, repositoryName, userId).member();
    }

    public boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return resolvePermission(ownerType, ownerName, repositoryName, userId).writable();
    }

    public RepositoryPermission resolvePermission(
            OwnerType ownerType,
            String ownerName,
            String repositoryName,
            Long userId) {
        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) {
            return RepositoryPermission.none();
        }
        return resolvePermission(repository.get(), userId);
    }

    public Optional<Boolean> resolveVisibility(
            OwnerType ownerType,
            String ownerName,
            String repositoryName) {
        return resolveRepository(ownerType, ownerName, repositoryName)
                .map(repository -> repository.getVisibility() == RepositoryVisibility.PUBLIC);
    }

    public RepositoryPermission resolvePermission(Repository repository, Long userId) {
        if (repository == null) {
            return RepositoryPermission.none();
        }

        if (repository.getVisibility() == RepositoryVisibility.PUBLIC && userId == null) {
            return new RepositoryPermission("PUBLIC_READ_ONLY", false, true);
        }
        if (userId == null) {
            return RepositoryPermission.anonymous();
        }

        UserId requesterId = UserId.of(userId);

        if (isOwner(repository, requesterId)) {
            return new RepositoryPermission("OWNER", true, true);
        }

        Optional<RepositoryPermission> repositoryMemberPermission =
                resolveRepositoryMemberPermission(repository, requesterId);
        if (repositoryMemberPermission.isPresent()) {
            return repositoryMemberPermission.get();
        }

        Optional<RepositoryPermission> organizationMemberPermission =
                resolveOrganizationMemberPermission(repository, requesterId);
        return organizationMemberPermission.orElseGet(RepositoryPermission::none);
    }

    private Optional<Repository> resolveRepository(
            OwnerType ownerType,
            String ownerName,
            String repositoryName) {
        if (ownerName == null || ownerName.isBlank()) {
            return Optional.empty();
        }
        if (repositoryName == null || repositoryName.isBlank()) {
            return Optional.empty();
        }

        if (ownerType == null) {
            return repositoryLookupService.resolveByPath(ownerName, repositoryName);
        }
        return repositoryLookupService.resolveByOwner(ownerType, ownerName, repositoryName);
    }

    private boolean isOwner(Repository repository, UserId requesterId) {
        return repository.getOwnerType() == OwnerType.USER
                && repository.getOwnerId() != null
                && requesterId.getValue().equals(repository.getOwnerId().getValue());
    }

    private Optional<RepositoryPermission> resolveRepositoryMemberPermission(
            Repository repository,
            UserId requesterId) {
        Optional<RepositoryMember> repositoryMember =
                repositoryMemberPort.findByRepositoryIdAndUserId(repository.getId(), requesterId);
        if (repositoryMember.isEmpty()) {
            return Optional.empty();
        }

        RepositoryMemberRole role = repositoryMember.get().getRole();
        boolean writable = role == RepositoryMemberRole.WRITER
                || role == RepositoryMemberRole.MAINTAINER;
        return Optional.of(new RepositoryPermission(
                "REPOSITORY_" + role.name(),
                writable,
                true));
    }

    private Optional<RepositoryPermission> resolveOrganizationMemberPermission(
            Repository repository,
            UserId requesterId) {
        if (repository.getOwnerType() != OwnerType.ORGANIZATION || repository.getOwnerId() == null) {
            return Optional.empty();
        }

        Optional<OrganizeMember> organizeMember = organizeMemberPort.findByOrganizeIdAndUserId(
                OrganizeId.of(repository.getOwnerId().getValue()),
                requesterId);
        if (organizeMember.isEmpty()) {
            return Optional.empty();
        }

        var role = organizeMember.get().getRole();
        boolean writable = switch (role) {
            case OWNER, MAINTAINER -> true;
            default -> false;
        };
        return Optional.of(new RepositoryPermission(
                "ORGANIZATION_" + role.name(),
                writable,
                true));
    }
}
```

## 아키텍처 메모
- access service는 `Repository`를 찾아오는 방법을 소유하지 않는다.
- access service는 `Repository`가 주어졌을 때 permission matrix를 계산하는 역할에 집중해야 한다.
- 현재 구조 문제는 `resolveRepositoryByNamespace(...)`가 lookup 규칙을 별도로 들고 있다는 점이다.
- 이 중복이 남아 있으면 namespace 충돌 정책이 다시 갈라진다.
- `GitRepositoryAccessService`는 1차 shared 이관 대상이 아니다.
- 먼저 repository 식별과 permission 계산 책임을 분리한 뒤, `RepositoryPermission`을 `repository.application.result`로 분리해 facade 계약과 내부 계산기를 같은 타입으로 맞춘다.
- 분리 완료 전까지는 access service가 lookup 정책을 재정의하지 않도록 한다.
- 현재 판단으로는 이 클래스가 외부 use case보다 내부 access evaluator에 더 가깝다.
- 목표 위치는 `io.jgitkins.server.repository.application.support`다.

## 검증 기준
- access 서비스는 계산 책임 중심이어야 한다.
- namespace 충돌 동작이 lookup과 access에서 동일해야 한다.
- public, owner, repository member, organization member 우선순위가 테스트로 고정되어야 한다.
