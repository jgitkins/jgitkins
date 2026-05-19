# Task 2.23 Detail 2: Collaboration Application Services

## 목표

- organization 생성, member 관리, access 계산을 서로 다른 application seam으로 분리한다.
- `OrganizeService`와 `OrganizeMemberService`를 단일 덩어리로 유지하지 않고 책임별 service로 나눈다.
- repository ownership / namespace / shared permission 계산의 호출 경계를 application layer에서 고정한다.

## 범위

- `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeMemberService.java`
- `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeValidator.java`
- `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeMemberValidator.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/in/*Organize*`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/*Organize*`
- `app-server/src/main/java/io/jgitkins/server/application/dto/*Organize*`

## 이동 맵

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeService.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/service/OrganizeService.java` |
| `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeMemberService.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/service/OrganizeMemberService.java` |
| `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeValidator.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/validate/OrganizeValidator.java` |
| `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeMemberValidator.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/validate/OrganizeMemberValidator.java` |
| `app-server/src/main/java/io/jgitkins/server/application/port/in/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/port/in/*Organize*` |
| `app-server/src/main/java/io/jgitkins/server/application/port/out/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/port/out/*Organize*` |
| `app-server/src/main/java/io/jgitkins/server/application/dto/*Organize*` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/dto/*Organize*` |

## 현재 코드 기준 이동 순서

1. `OrganizeService` import를 collaboration domain/application으로 전환
2. `OrganizeMemberService` import를 collaboration domain/application으로 전환
3. validator 패키지를 collaboration 하위로 이동
4. inbound/outbound port package move
5. application dto/result/mapper import 정리

## before/after 스니펫

```java
// before
@Service
@RequiredArgsConstructor
public class OrganizeService implements OrganizeCreationUseCase,
                                        OrganizeLoadUseCase,
                                        OrganizeDeletionUseCase {
    private final OrganizePersistencePort organizePort;
    private final CurrentUserPort currentUserPersistencePort;
}
```

```java
// after
@Service
@RequiredArgsConstructor
public class OrganizeService implements OrganizeCreationUseCase,
                                        OrganizeLoadUseCase,
                                        OrganizeDeletionUseCase {
    private final OrganizePersistencePort organizePort;
    private final CurrentUserPort currentUserPersistencePort;
    // package import이 collaboration/application/*로 바뀐다.
}
```

```java
// before
public class OrganizeMemberService implements OrganizeMemberAddUseCase,
                                               OrganizeMemberRemoveUseCase,
                                               OrganizeMemberQueryUseCase {
    private final OrganizeMemberPersistencePort organizeMemberPort;
}
```

```java
// after
public class OrganizeMemberService implements OrganizeMemberAddUseCase,
                                               OrganizeMemberRemoveUseCase,
                                               OrganizeMemberQueryUseCase {
    private final OrganizeMemberPersistencePort organizeMemberPort;
    // collaboration/application/service로 이동
}
```

## 권장 구조

```text
app-server/src/main/java/io/jgitkins/server/collaboration/application/service/
  OrganizeService
  OrganizeMemberService
  OrganizeAccessService
```

## 책임 분리

### 1. OrganizeService

- 조직 생성, 조회, 삭제 같은 aggregate-level command를 처리한다.
- 존재 여부와 이름 중복을 먼저 검증한다.
- domain event 발행과 persistence save를 오케스트레이션한다.

### 2. OrganizeMemberService

- member add/remove/query를 담당한다.
- default role 결정과 duplicate membership 검증을 처리한다.
- relation model 저장과 삭제를 직접 오케스트레이션한다.

### 3. OrganizeAccessService

- owner/member 기준 접근 가능 여부를 계산한다.
- namespace resolution에서 필요한 read seam만 제공한다.
- repository 쪽이 직접 persistence adapter를 알지 않도록 중간 seam 역할을 한다.

## 검증 포인트

- `OrganizeValidator`는 name 중복, 존재 여부, owner validity만 다룬다.
- `OrganizeMemberValidator`는 duplicate membership과 role 기본값만 다룬다.
- access 계산은 application service에서만 수행되고, domain aggregate는 계산하지 않는다.

## 설계 기준

- service 이름은 현재 코드명을 그대로 억지로 유지하지 말고, 역할이 보이는 이름으로 정리한다.
- query와 command가 섞인 메서드는 분리한다.
- bulk lookup이 필요한 곳은 request마다 반복 조회하지 않는 방향을 우선한다.

## 테스트 기준

- 조직 생성 happy path / duplicate name / invalid owner
- member 추가 happy path / duplicate membership / delete missing member
- access lookup public / private / owner / member / anonymous
- service 간 책임 침범 여부를 architecture test로 확인

## 완료 기준

- collaboration application seam이 역할별로 분리된다.
- repository access 계산의 진입점이 명확해진다.
- 다음 단계에서 persistence/shared permission 정리를 붙일 수 있다.
- `OrganizeService`, `OrganizeMemberService`, validator, dto import가 old package에 남지 않는다.
