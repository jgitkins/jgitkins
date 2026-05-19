# Task 2.23 Detail 1: Collaboration Domain Boundary

## 목표

- `Organize` aggregate root와 `OrganizeMember` relation model의 경계를 먼저 고정한다.
- `OrganizeCreatedEvent`, `OrganizeId`, `OrganizeName`, `OrganizeMemberRole`의 패키지 위치를 collaboration domain 기준으로 정리한다.
- rename을 하지 않고도 domain boundary를 `io.jgitkins.server.collaboration` 아래로 먼저 모을 수 있게 한다.

## 범위

- `app-server/src/main/java/io/jgitkins/server/domain/aggregate/Organize.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/OrganizeMember.java`
- `app-server/src/main/java/io/jgitkins/server/domain/event/OrganizeCreatedEvent.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeId.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeName.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeMemberRole.java`
- 연관 단위 테스트

## 이동 맵

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/domain/aggregate/Organize.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/aggregate/Organize.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/OrganizeMember.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/entity/OrganizeMember.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/event/OrganizeCreatedEvent.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/event/OrganizeCreatedEvent.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeId.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeId.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeName.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeName.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeMemberRole.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeMemberRole.java` |

## 코드 스니펫

```java
// before
package io.jgitkins.server.domain.aggregate;

public class Organize {
    public static Organize create(OrganizeName name, UserId ownerId, String description) {
        // ...
    }
}
```

```java
// after
package io.jgitkins.server.collaboration.domain.aggregate;

public class Organize {
    public static Organize create(OrganizeName name, UserId ownerId, String description) {
        // same invariant, new package root
    }
}
```

```java
// before
package io.jgitkins.server.domain.model;

public class OrganizeMember {
}
```

```java
// after
package io.jgitkins.server.collaboration.domain.entity;

public class OrganizeMember {
}
```

## 핵심 판단

### 1. Aggregate root는 `Organize`만 유지한다

- 조직의 핵심 상태는 `Organize`가 소유한다.
- 이름, owner, 생성 시각, 활성 상태 같은 persisted state만 aggregate에 둔다.
- member 집합 자체를 aggregate root의 source of truth로 과도하게 끌어오지 않는다.

### 2. `OrganizeMember`는 relation model로 본다

- `OrganizeMember`는 `Organize` 내부 collection보다 `User`와 `Organize` 사이의 관계 모델에 가깝다.
- membership 조회는 aggregate 로딩이 아니라 query seam으로 처리할 수 있어야 한다.
- role 기본값은 `MEMBER`로 유지한다.

### 3. domain event는 organization 생성 시점에만 사용한다

- `OrganizeCreatedEvent`는 aggregate 생성 후 저장/후속 orchestration에만 쓰인다.
- member 추가/제거는 별도 relation change로 다루고, aggregate event로 섞지 않는다.

### 4. VO는 collaboration domain에 묶는다

- `OrganizeId`, `OrganizeName`, `OrganizeMemberRole`는 collaboration domain VO로 정렬한다.
- identity/access의 `UserId`는 다시 정의하지 않고 그대로 참조한다.

## 구현 원칙

- persisted state와 computed result를 섞지 않는다.
- aggregate는 validation과 invariant를 책임지고, membership lookup은 application seam이 책임진다.
- file 이동과 package 이동은 동일 PR에서 하되, rename은 별도 subtask로 미룬다.

## 테스트 기준

- `Organize.create(...)`는 name, owner, timestamps, event emission을 검증한다.
- `Organize.reconstruct(...)`는 저장된 상태만 복원한다.
- `OrganizeMember.create(...)`는 필수 식별자와 role 불변식을 검증한다.
- `OrganizeMemberRole`의 default role 정책이 깨지지 않아야 한다.
- package convention test에서 collaboration domain이 old package에 남지 않아야 한다.

## 완료 기준

- domain package root가 collaboration 기준으로 이동한다.
- domain event, VO, aggregate boundary가 일관된다.
- 다음 단계에서 application seam을 옮길 수 있을 정도로 domain 계약이 고정된다.
- `Organize` / `OrganizeMember` / VO / event import가 old package에 남지 않는다.
