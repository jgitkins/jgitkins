# 03. Identity & Access Persistence and Security 상세 계획

## 목적

Identity & Access Context의 persistence adapter, mapper, security adapter를 새 package root 아래로 이동한다.

## AS-IS

```text
app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/UserDomainMapper.java
app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/UserIdentityDomainMapper.java
app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/UserCredentialDomainMapper.java

app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserPersistenceAdapter.java
app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserIdentityPersistenceAdapter.java
app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserCredentialPersistenceAdapter.java

app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserEntity.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserEntityCondition.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserIdentitiesEntity.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserIdentitiesEntityCondition.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserCredentialsEntity.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/UserCredentialsEntityCondition.java

app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/UserEntityMbgMapper.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/UserIdentitiesEntityMbgMapper.java
app-server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/UserCredentialsEntityMbgMapper.java

app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/security/CurrentUserSecurityAdapter.java
app-server/src/main/java/io/jgitkins/server/infrastructure/config/security/auth/PatTokenAuthenticationService.java
```

## TO-BE

```text
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/mapper/UserDomainMapper.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/mapper/UserIdentityDomainMapper.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/mapper/UserCredentialDomainMapper.java

app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/adapter/persistence/UserPersistenceAdapter.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/adapter/persistence/UserIdentityPersistenceAdapter.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/adapter/persistence/UserCredentialPersistenceAdapter.java

app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserEntity.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserEntityCondition.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserIdentitiesEntity.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserIdentitiesEntityCondition.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserCredentialsEntity.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/model/UserCredentialsEntityCondition.java

app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/mapper/UserEntityMbgMapper.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/mapper/UserIdentitiesEntityMbgMapper.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/persistence/mapper/UserCredentialsEntityMbgMapper.java

app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/adapter/security/CurrentUserSecurityAdapter.java
app-server/src/main/java/io/jgitkins/server/identity/access/infrastructure/adapter/security/PatTokenAuthenticationService.java
```

## 결정 사항

- `UserPersistenceAdapter`는 `User` aggregate root만 저장/조회한다.
- `UserIdentityPersistenceAdapter`는 provider identity lookup/update만 담당한다.
- `UserCredentialPersistenceAdapter`는 PAT credential 저장/조회/폐기만 담당한다.
- `CurrentUserSecurityAdapter`는 현재 사용자 식별만 담당한다.
- `PatTokenAuthenticationService`는 PAT authentication만 담당한다.
- MBG entity는 persisted state만 표현한다.
- mapper는 computed result를 저장하지 않는다.
- `PasswordEncoder`는 PAT hash 생성 도구로만 남긴다.

## 코드 스니펫

### UserDomainMapper

```java
package io.jgitkins.server.identity.access.infrastructure.mapper;

import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.infrastructure.persistence.model.UserEntity;

public class UserDomainMapper {
    public UserEntity toEntity(User user) { ... }
    public User toDomain(UserEntity entity) { ... }
}
```

### Persistence adapter

```java
package io.jgitkins.server.identity.access.infrastructure.adapter.persistence;

import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import io.jgitkins.server.identity.access.domain.aggregate.User;

public class UserPersistenceAdapter implements UserPersistencePort {
    ...
}
```

## 테스트 기준

- user mapper는 user aggregate state만 왕복해야 한다.
- identity mapper는 external provider linkage를 보존해야 한다.
- credential mapper는 password hash와 provider metadata를 보존해야 한다.
- security adapter는 current user boundary를 repository lookup과 혼동하지 않아야 한다.

## 완료 기준

- persistence / mapper / security adapter가 identity.access package 기준으로 정리된다.
- persisted state와 computed result 경계가 adapter 레벨에서도 일관된다.
