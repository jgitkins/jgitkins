# Task 2.23 Detail 4: Collaboration Presentation and Tests

## 목표

- organization / member API를 presentation layer에서 collaboration context로 정렬한다.
- controller, request dto, mapper, test를 패키지 경계에 맞춰 재배치한다.
- rename을 제외하고도 presentation test coverage가 안정적으로 유지되게 한다.

## 범위

- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeMemberController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/web/WebOrganizeController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/dto/*Organize*`
- `app-server/src/main/java/io/jgitkins/server/presentation/mapper/OrganizeRequestMapper.java`
- `app-server/src/test/java/io/jgitkins/server/presentation/**/Organize*`
- `app-server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`

## 이동 맵

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/rest/OrganizeController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeMemberController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/rest/OrganizeMemberController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/api/web/WebOrganizeController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/web/WebOrganizeController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/dto/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/dto/*Organize*` |
| `app-server/src/main/java/io/jgitkins/server/presentation/mapper/OrganizeRequestMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/mapper/OrganizeRequestMapper.java` |

## 코드 스니펫

```java
// before
@RestController
@RequestMapping("/api/organizes")
public class OrganizeController {
}
```

```java
// after
@RestController
@RequestMapping("/api/collaboration/organizes")
public class OrganizeController {
}
```

```java
// before
public record OrganizeCreationRequest(String name, String description) {}
```

```java
// after
public record OrganizeCreationRequest(String name, String description) {}
// package root만 collaboration/presentation/dto로 이동
```

```java
// before
// test imports still point to io.jgitkins.server.presentation.api.rest.OrganizeController
```

```java
// after
// test imports point to io.jgitkins.server.collaboration.presentation.api.rest.OrganizeController
```

## 정리 방향

### 1. Controller

- organization 생성, member 관리, accessible organization 조회 endpoint를 collaboration 기준으로 본다.
- controller는 orchestration이 아니라 request translation만 맡긴다.
- query와 command endpoint를 섞지 않는다.

### 2. DTO / Mapper

- request dto와 result dto는 collaboration presentation 기준으로 재정렬한다.
- `OrganizeRequestMapper`는 transport object와 application dto 사이 변환만 맡긴다.
- DTO는 단순 carrier 성격이면 record 전환 후보가 될 수 있지만, 이번 task의 핵심은 rename이 아니다.

### 3. Tests

- controller test는 request translation, status code, response shape를 검증한다.
- service test는 behavior를 검증한다.
- architecture test는 package boundary를 검증한다.

## 테스트 우선순위

1. controller happy path
2. controller validation / error mapping
3. service unit test
4. architecture package convention
5. compile / test regression

## 실패 모드

- controller import가 old package를 그대로 들고 오는 경우
- presentation mapper가 domain detail을 직접 알게 되는 경우
- package move 이후 test fixture가 이전 경로를 참조하는 경우

## 완료 기준

- collaboration presentation이 old package 의존을 정리한다.
- 주요 controller와 test가 새 경계에 맞게 정렬된다.
- 이후 rename subtask를 독립적으로 진행해도 충돌이 적다.
- controller, DTO, mapper, test import가 새 package root로 정렬된다.
