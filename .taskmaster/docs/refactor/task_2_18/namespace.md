# Namespace

## 핵심
- `RepositoryNamespaceResolver`의 책임을 `owner -> namespace` 변환으로 고정한다.
- 현재 구현 기준으로 문서와 코드의 불일치를 제거한다.

## 정리 방향
- `namespace -> owner` 해석 관련 서술은 제거한다.
- `resolve(Repository)`와 `resolve(OwnerType, OwnerId)`는 모두 `owner -> namespace` 변환으로 본다.
- `resolve(Repository)` 유지 여부는 후속 작업으로 분리한다.

## 예시 코드
```java
package io.jgitkins.server.shared.application.support;

import io.jgitkins.server.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryNamespaceResolver {

    private final OrganizePersistencePort organizePort;
    private final UserPersistencePort userPort;

    public String resolve(Repository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null");
        }
        return resolve(repository.getOwnerType(), repository.getOwnerId());
    }

    public String resolve(OwnerType ownerType, OwnerId ownerId) {
        if (ownerType == null) {
            throw new IllegalArgumentException("ownerType must not be null");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId must not be null");
        }

        return switch (ownerType) {
            case ORGANIZATION -> resolveOrganizationNamespace(ownerId);
            case USER -> resolveUserNamespace(ownerId);
        };
    }

    private String resolveOrganizationNamespace(OwnerId ownerId) {
        return organizePort.findById(OrganizeId.of(ownerId.getValue()))
                .map(organize -> organize.getName().getValue())
                .orElseThrow(OrganizeNotFoundException::new);
    }

    private String resolveUserNamespace(OwnerId ownerId) {
        User user = userPort.findById(ownerId.getValue())
                .orElseThrow(UserNotFoundException::new);
        return user.getUsername();
    }
}
```

## 포트 네이밍 메모
- 현재 `OrganizePersistencePort`, `UserPersistencePort`는 CRUD 전체를 포함하는 포트이므로 즉시 `QueryPort`로 rename 하지는 않는다.
- 중장기 방향은 조회성 outgoing port를 `*QueryPort`로 분리하고, CUD 성격은 aggregate repository 또는 별도 command 성격 포트로 분리하는 것이다.
- 따라서 이 문서의 예시 코드는 현행 포트 이름을 유지하되, 후속 리팩토링 시 query-oriented port 분리를 검토 대상으로 남긴다.

## 아키텍처 메모
- `namespace -> owner` 역방향 해석은 같은 클래스에 다시 넣지 않는다.
- 역방향 해석이 필요해지면 `NamespaceOwnerResolver` 같은 별도 개념으로 분리한다.
- 이 클래스는 변환기다. 조회 전략, 충돌 해소 정책, 접근 제어 책임을 흡수하면 다시 경계가 무너진다.
- `Task 2.18` 1차 패키지 이관 대상으로 적합하다.
- 목표 위치는 `io.jgitkins.server.shared.application.support`다.

## 검증 기준
- `RepositoryNamespaceResolver`는 `owner -> namespace` 변환만 제공해야 한다.
- 문서에 현재 구현과 어긋나는 설명이 남지 않아야 한다.
- 동작 변화는 없어야 한다.
