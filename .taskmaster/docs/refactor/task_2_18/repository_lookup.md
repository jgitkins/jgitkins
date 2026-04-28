# Repository Lookup

## 핵심
- `RepositoryLookupService`를 repository 조회 전용으로 단순화한다.
- visibility 판단은 별도 서비스로 분리한다.

## 정리 방향
- `RepositoryLookupService` public API는 `resolveByPath(...)` 중심으로 정리한다.
- visibility 계산은 `RepositoryAccessibilityService`로 이동한다.
- 조회 흐름은 `trimSlashes -> clonePath -> user owner -> organization owner` 순서로 고정한다.
- namespace 충돌 시 user-owned repository 우선 규칙은 유지한다.

## 책임 분리 계획
- `RepositoryLookupService`는 최종적으로 `repository identifier resolver` 역할만 가진다.
- 조회 API와 접근 제어 API를 동시에 제공하지 않는다.
- 내부 구현에서 clone path 조회, user owner 조회, organization owner 조회를 분리하되 외부에는 하나의 조회 규칙만 노출한다.
- namespace 충돌 정책은 이 서비스가 단일 소유한다.

## 단계별 실행 계획
1. 현재 `resolveByPath(...)`의 목표 흐름을 문서 기준으로 확정한다.
2. `findByClonePath`, `findUserOwned`, `findOrganizationOwned` private 단계로 분리한다.
3. visibility 관련 메서드는 `RepositoryAccessibilityService`로 완전히 이관한다.
4. `resolveByOwner(...)`를 명시적 API로 두어 access service가 namespace 해석을 재구현하지 않게 만든다.
5. lookup 관련 테스트를 `조회 성공`, `조회 우선순위`, `namespace 충돌`, `입력 normalize` 기준으로 고정한다.

## 예시 코드
```java
package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryLookupService {

    private final RepositoryPersistencePort repositoryPort;
    private final UserPersistencePort userPort;
    private final OrganizePersistencePort organizePort;

    public Optional<Repository> resolveByPath(String namespace, String repoName) {
        String normalizedNamespace = trimSlashes(namespace);
        String normalizedRepoName = trimSlashes(repoName);

        return findByClonePath(normalizedNamespace, normalizedRepoName)
                .or(() -> findUserOwned(normalizedNamespace, normalizedRepoName))
                .or(() -> findOrganizationOwned(normalizedNamespace, normalizedRepoName));
    }

    public Optional<Repository> resolveByOwner(
            OwnerType ownerType,
            String ownerName,
            String repositoryName) {
        if (ownerType == null) {
            throw new IllegalArgumentException("ownerType must not be null");
        }

        String normalizedOwnerName = trimSlashes(ownerName);
        String normalizedRepositoryName = trimSlashes(repositoryName);

        return switch (ownerType) {
            case USER -> findUserOwned(normalizedOwnerName, normalizedRepositoryName);
            case ORGANIZATION -> findOrganizationOwned(normalizedOwnerName, normalizedRepositoryName);
        };
    }

    private Optional<Repository> findByClonePath(String namespace, String repoName) {
        String clonePath = RepositoryPathHelper.buildClonePath(namespace, repoName);
        return repositoryPort.findByClonePath(clonePath);
    }

    private Optional<Repository> findUserOwned(String namespace, String repoName) {
        Optional<User> user = userPort.findByUsername(namespace);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        return repositoryPort.findByOwnerAndName(
                OwnerType.USER,
                OwnerId.of(user.get().getId()),
                RepositoryName.from(repoName));
    }

    private Optional<Repository> findOrganizationOwned(String namespace, String repoName) {
        Optional<Organize> organize = findOrganizationByNamespace(namespace);
        if (organize.isEmpty()) {
            return Optional.empty();
        }

        return repositoryPort.findByOwnerAndPath(
                OwnerType.ORGANIZATION,
                OwnerId.of(organize.get().getId().getValue()),
                RepositoryPath.from(repoName));
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

## 아키텍처 메모
- lookup은 "어떤 repository인가"를 찾는 단계다.
- accessibility는 "보여줄 수 있는가"를 판단하는 단계다.
- 둘을 다시 합치면 상위 orchestration 서비스가 더 강하게 결합된다.
- `RepositoryAccessibilityService`는 1차 shared 이관 후보다.
- `RepositoryLookupService`는 repository 식별 전용 collaborator로 고정하고 `io.jgitkins.server.repository.application.support`로 이관한다.
- 분리 완료 전까지는 lookup 서비스에 permission 계산이나 membership 판단 로직을 다시 넣지 않는다.
- 현재 복잡성의 원인은 일반 API와 Git Smart HTTP API가 같은 lookup 서비스를 공유하기 때문이다.
- 후속 단계에서는 `공통 resolution core + 채널별 facade` 구조로 한 번 더 나눌 수 있다.
- public API는 persistence query 느낌의 `find`보다 identifier resolution 의미가 드러나는 `resolve` 접두사를 우선 검토한다.
- private 단계 메서드는 실제 조회 실패 가능성을 표현하므로 `findByClonePath`, `findUserOwned`, `findOrganizationOwned`를 유지한다.
- controller 입력에서 들어오는 값 정리는 복잡한 normalize가 아니라 `trimSlashes` 수준의 좁은 이름으로 두는 편이 읽기 쉽다.

## 검증 기준
- lookup과 accessibility 책임이 분리되어야 한다.
- clone path 우선 조회가 유지되어야 한다.
- 충돌 우선순위가 테스트로 고정되어야 한다.
