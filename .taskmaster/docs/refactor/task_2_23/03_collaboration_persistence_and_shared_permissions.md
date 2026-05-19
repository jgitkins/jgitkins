# Task 2.23 Detail 3: Collaboration Persistence and Shared Permissions

## 목표

- collaboration persistence adapter와 mapper를 domain boundary에 맞춰 정리한다.
- `RepositoryNamespaceResolver`, `RepositoryAccessibilityService`가 collaboration query seam만 보도록 만든다.
- repository/shared 쪽에서 organization persistence detail을 직접 아는 의존성을 줄인다.

## 범위

- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizePersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizeMemberPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeDomainMapper.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeMemberDomainMapper.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/*Organize*`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/*Organize*`
- `app-server/src/main/java/io/jgitkins/server/shared/application/support/RepositoryNamespaceResolver.java`
- `app-server/src/main/java/io/jgitkins/server/shared/application/support/RepositoryAccessibilityService.java`

## 이동 맵

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizePersistenceAdapter.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/adapter/persistence/OrganizePersistenceAdapter.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizeMemberPersistenceAdapter.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/adapter/persistence/OrganizeMemberPersistenceAdapter.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeDomainMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/mapper/OrganizeDomainMapper.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeMemberDomainMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/mapper/OrganizeMemberDomainMapper.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/persistence/model/*Organize*` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/persistence/mapper/*Organize*` |
| `app-server/src/main/java/io/jgitkins/server/shared/application/support/RepositoryNamespaceResolver.java` | 유지하되 collaboration query seam 의존으로 전환 |
| `app-server/src/main/java/io/jgitkins/server/shared/application/support/RepositoryAccessibilityService.java` | 유지하되 collaboration query seam 의존으로 전환 |

## query seam 설계 스니펫

```java
// before
private final OrganizePersistencePort organizePort;
private final OrganizeMemberPersistencePort organizeMemberPort;
```

```java
// after
private final OrganizeQueryPort organizeQueryPort;
private final OrganizeMemberQueryPort organizeMemberQueryPort;
```

```java
// before
return organizePort.findById(OrganizeId.of(ownerId.getValue()))
        .map(org -> org.getName().getValue())
        .orElseThrow(OrganizeNotFoundException::new);
```

```java
// after
return organizeQueryPort.findNameById(OrganizeId.of(ownerId.getValue()))
        .orElseThrow(OrganizeNotFoundException::new);
```

```java
// before
id -> organizeMemberPort.existsByOrganizeIdAndUserId(id, UserId.of(userId))
```

```java
// after
id -> organizeMemberQueryPort.existsByOrganizeIdAndUserId(id, UserId.of(userId))
```

## 핵심 판단

### 1. persistence는 persisted state만 다룬다

- entity는 aggregate의 영속 상태만 표현한다.
- computed result, access policy, namespace 해석은 persistence layer에 두지 않는다.

### 2. member lookup은 query seam으로 분리한다

- `RepositoryAccessibilityService`가 조직별 membership을 조회할 때 매번 N번 찌르지 않도록 한다.
- 가능하면 bulk query port를 제공한다.
- bulk query가 당장 어렵다면 후속 TODO로 분리하되, 지금 구조가 더 악화되지 않게 막는다.

### 3. shared/repository는 collaboration persistence detail을 숨긴다

- repository와 shared는 collaboration application/query seam만 의존한다.
- old package의 `OrganizePersistencePort`, `OrganizeMemberPersistencePort`를 직접 끌어오는 구조는 줄인다.

## 설계 포인트

- `OrganizeDomainMapper`는 aggregate와 entity 간의 순수 변환만 맡긴다.
- persistence adapter는 transaction boundary와 save/load orchestration만 맡긴다.
- namespace resolution은 owner type에 따라 user username 또는 organization name을 해석한다.

## 성능 메모

- accessible organization 조회는 조직 수가 늘어날수록 membership lookup 비용이 커진다.
- bulk query port가 가장 안전하다.
- 현재 규모가 작아도 architecture는 bulk-ready로 맞춰 두는 편이 낫다.

## 테스트 기준

- aggregate/entity round-trip이 persisted state만 보존하는지 검증
- namespace resolution이 owner type에 따라 분기되는지 검증
- accessibility 계산이 public/private, owner/member, anonymous를 모두 다루는지 검증
- shared/repository가 collaboration persistence adapter를 직접 의존하지 않는지 검증

## 완료 기준

- collaboration persistence adapter와 shared permission seam이 분리된다.
- repository/shared는 read seam만 보게 된다.
- 성능 병목이 되는 lookup 구조가 눈에 보이게 된다.
- collaboration query seam 인터페이스가 실제 코드의 target package를 명시한다.
