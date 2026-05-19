# 01. Identity & Access Domain Boundary 상세 계획

## 목적

`Identity & Access Context`의 핵심 도메인 모델을 top-level `domain` 패키지에서 `identity.access.domain` 하위로 이동한다.

대상 모델은 `User`, `UserIdentity`, `UserCredential`다. `UserIdentity`와 `UserCredential`은 독립 aggregate로 승격하지 않고 `User`의 종속 모델로 유지한다.

## AS-IS

```text
app-server/src/main/java/io/jgitkins/server/domain/model/User.java
app-server/src/main/java/io/jgitkins/server/domain/model/UserIdentity.java
app-server/src/main/java/io/jgitkins/server/domain/model/UserCredential.java
app-server/src/main/java/io/jgitkins/server/domain/model/UserAuthority.java
app-server/src/main/java/io/jgitkins/server/domain/model/UserStatus.java

app-server/src/main/java/io/jgitkins/server/domain/model/vo/UserId.java
app-server/src/main/java/io/jgitkins/server/domain/model/vo/Username.java
app-server/src/main/java/io/jgitkins/server/domain/model/vo/SystemUser.java

app-server/src/main/java/io/jgitkins/server/domain/exception/UserAlreadyActivatedException.java
```

## TO-BE

```text
app-server/src/main/java/io/jgitkins/server/identity/access/domain/aggregate/User.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/entity/UserIdentity.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/entity/UserCredential.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/vo/UserId.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/vo/Username.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/vo/SystemUser.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/vo/UserAuthority.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/vo/UserStatus.java
app-server/src/main/java/io/jgitkins/server/identity/access/domain/exception/UserAlreadyActivatedException.java
```

## 결정 사항

- `User`는 Identity & Access Context Aggregate Root다.
- `UserIdentity`는 외부 provider identity를 내부 사용자와 연결하는 entity다.
- `UserCredential`은 PAT 발급/조회/폐기용 entity다.
- `UserStatus`는 `PENDING`, `ACTIVE` 중심으로 유지한다.
- `UserAuthority`는 사용자 권한 표현용 value/enum이다.
- `SystemUser`는 내부 시스템 사용자 식별 표현으로 유지한다.
- `UserCredential`을 별도 aggregate로 승격하지 않는다. PAT 계정 수명은 `User`에 종속된다.
- `UserIdentity`도 별도 aggregate로 승격하지 않는다. OAuth 로그인 결과를 내부 사용자와 연결하는 역할이 강하다.

## 코드 스니펫

### User

```java
package io.jgitkins.server.identity.access.domain.aggregate;

import io.jgitkins.server.domain.aggregate.AbstractAggregateRoot;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.identity.access.domain.vo.Username;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends AbstractAggregateRoot<UserId> {

    private final UserId id;
    private final Username username;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final UserAuthority authority;
    private final UserStatus status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<UserIdentity> identities;
    private final List<UserCredential> credentials;

    public static User createPending(...) {
        ...
    }

    public User activateWithUsername(Username username) {
        ...
    }
}
```

### UserIdentity

```java
package io.jgitkins.server.identity.access.domain.entity;

import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserIdentity {
    private final Long id;
    private final UserId userId;
    private final String providerName;
    private final String providerSub;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
```

### UserCredential

```java
package io.jgitkins.server.identity.access.domain.entity;

import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCredential {
    private final Long id;
    private final UserId userId;
    private final String provider;
    private final String name;
    private final String description;
    private final String passwordHash;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
```

## 테스트 기준

- `User` 생성 시 username, status, timestamps가 일관되게 채워져야 한다.
- `User.activateWithUsername(...)`는 `PENDING` 상태에서만 성공해야 한다.
- `UserIdentity`는 외부 provider 연결 정보를 보존해야 한다.
- `UserCredential`은 PAT provider와 password hash를 보존해야 한다.
- `User` 재구성 시 identity/credential 종속 모델은 aggregate 내부 컬렉션으로만 복원된다.

## 완료 기준

- `User`를 중심으로 identity-access domain 경계가 선명해진다.
- `UserIdentity`와 `UserCredential`의 aggregate 승격 여부가 문서 기준으로 닫힌다.
- 상태 전이와 entity 저장 경계가 도메인 코드 기준으로 정리된다.
