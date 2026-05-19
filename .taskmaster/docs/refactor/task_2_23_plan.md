# Task 2.23 리팩토링 계획

## 제목

- **리팩토링 계획**: P5 Collaboration Context 기준 패키지 승격 및 경계 재정렬
- **후속 상세 계획 단위**: `task_2_23/` 하위에 Domain, Application, Persistence, Presentation/Test 단위로 분리 작성

## 세부 문서

1. [01_collaboration_domain_boundary.md](./task_2_23/01_collaboration_domain_boundary.md)
2. [02_collaboration_application_services.md](./task_2_23/02_collaboration_application_services.md)
3. [03_collaboration_persistence_and_shared_permissions.md](./task_2_23/03_collaboration_persistence_and_shared_permissions.md)
4. [04_collaboration_presentation_and_tests.md](./task_2_23/04_collaboration_presentation_and_tests.md)

## 배경

`Collaboration Context`는 조직 생성, 조직 멤버십, organization ownership, repository 접근 가능 여부의 기초 정보를 다룬다.

현재 관련 코드는 `app-server` 안에 흩어져 있고, 도메인 용어도 문서와 코드가 완전히 맞지 않는다.

- 코드 이름은 `Organize`, `OrganizeMember`, `OrganizeService`, `OrganizeController`다.
- 문서 언어는 `Organization`, `Organization Member`, `Collaboration Context`가 더 자연스럽다.
- `Identity & Access Context`는 이미 `io.jgitkins.server.identity.access`로 승격되어 있으므로, collaboration 쪽은 그 결과를 전제로 다시 묶어야 한다.

이 작업의 목적은 기능 추가가 아니라 bounded context 경계를 `app-server` 내부에서 먼저 선명하게 만드는 것이다.
즉, `Collaboration Context`를 `io.jgitkins.server.collaboration` 패키지 아래로 먼저 모으고, 그 다음 단계에서 Gradle module extraction을 검토한다.

분리 축은 둘이다.

- persisted state: `Organization`가 직접 소유해야 하는 상태
- computed result: membership, repository access, namespace resolution 시점에 다시 계산해야 하는 결과

## 현재 상태에서 이미 존재하는 것

이 계획은 새로 다 만드는 문서가 아니라, 이미 있는 구조를 덜 억지스럽게 정리하는 문서다.

- `Organize` aggregate는 이미 조직의 핵심 상태를 소유하고 있다.
- `OrganizeMember`는 이미 membership row 성격을 가진다.
- `OrganizeService`와 `OrganizeMemberService`는 생성, 조회, 추가, 제거 흐름을 이미 오케스트레이션하고 있다.
- `OrganizeValidator`와 `OrganizeMemberValidator`는 중복 이름, 중복 멤버, 접근 가능 여부를 이미 검증한다.
- `RepositoryNamespaceResolver`는 organization owner를 namespace 문자열로 변환하는 seam을 이미 가지고 있다.
- `RepositoryAccessibilityService`는 owner/member 기준 접근 가능 여부를 이미 계산한다.
- `Identity & Access Context`가 `UserId`를 제공하므로, collaboration 쪽은 user identity를 다시 정의할 필요가 없다.

## 목표

- `Collaboration Context`를 `app-server` 내부의 `io.jgitkins.server.collaboration` 패키지로 승격한다.
- `Organize` aggregate가 소유할 값과 `OrganizeMember`의 relation model 경계를 다시 고정한다.
- organization 생성, member 추가/제거, organization 접근 가능 여부 계산 책임을 application seam으로 분리한다.
- repository ownership, namespace resolution, shared permission calculation이 collaboration context를 명시적으로 통해가도록 정리한다.
- `Organize` 명명 rename은 별도 subtask로 분리하고, 이번 단계에서는 패키지 경계만 먼저 정리한다.
- 이 단계에서는 Gradle module extraction을 하지 않고, package-local bounded context를 먼저 완성한다.

## 핵심 결정

### 1. Context 이름과 Java package root는 분리해서 본다

- 문서와 도메인 모델의 이름: `Collaboration Context`
- 문서 용어: `Organization`, `Organization Member`
- Java package root: `io.jgitkins.server.collaboration`
- 현재 코드 클래스명: `Organize`, `OrganizeMember`는 유지

이 작업에서는 전면적인 `Organization` rename을 하지 않는다.
그 rename은 파일명, 테스트명, MapStruct, event, validator, persistence mapper까지 한 번에 흔들기 때문에, 이번 task의 핵심인 경계 재정렬보다 blast radius가 크다.

### 2. app-server는 composition root로 남긴다

`app-server`는 현재 단계에서 조립자 역할을 유지한다.

- repository, execution, change-review, shared, identity/access는 각자 별도 context로 남는다.
- collaboration만 먼저 `app-server` 내부에서 패키지 승격을 진행한다.
- module extraction은 후속 단계다.

### 3. Organization member는 aggregate 내부 entity가 아니라 relation model로 본다

- `Organize`는 aggregate root다.
- `OrganizeMember`는 organization 내부 entity보다 relation model에 가깝다.
- 멤버십 조회는 organization aggregate의 source of truth가 아니라 membership query seam으로 다룬다.
- 멤버십의 기본 역할은 `MEMBER`다.

### 4. repository 접근 가능 여부는 organization owner/member seam으로 계산한다

현재 repository 접근 가능 여부는 다음 두 축으로 계산된다.

- owner가 organization인지 user인지
- organization owner면 requester가 membership에 있는지

이 경계는 application service에서 처리하고, domain aggregate는 organization 상태와 membership state만 책임진다.

### 5. shared/repository 쪽은 collaboration persistence port를 직접 알지 않도록 줄인다

지금은 shared/repository 쪽이 organization persistence/member persistence port를 직접 가져다 쓴다.
이번 계획에서는 이 의존성을 완전히 한 번에 없애기보다, 다음 순서로 줄인다.

1. collaboration package 내부 port를 먼저 정리한다.
2. repository/shared가 읽는 데이터만 남기는 query seam을 만든다.
3. repository/shared가 persistence detail 대신 collaboration query seam만 보게 바꾼다.

이렇게 해야 현재 동작을 유지하면서 경계만 앞으로 밀 수 있다.

## TO-BE 패키지 방향

``` text
app-server/src/main/java/io/jgitkins/server/collaboration/
  domain/
    aggregate/
    entity/
    event/
    vo/
    exception/
    repository/
  application/
    service/
    port/in/
    port/out/
    dto/command/
    dto/result/
    support/
    mapper/
    validate/
  infrastructure/
    adapter/persistence/
    mapper/
    persistence/model/
    persistence/mapper/
  presentation/
    api/rest/
    dto/
    mapper/
```

이 구조의 핵심은 다음이다.

- `domain`은 `Organize`와 `OrganizeMember`의 상태 경계를 가진다.
- `application`은 organization 생성, membership 관리, access 판단 seam을 가진다.
- `infrastructure`는 MBG entity, persistence adapter, mapper를 가진다.
- `presentation`은 organization/member API를 가진다.
- `shared`와 `repository`는 collaboration의 query seam만 읽고, persistence detail은 모른다.

## 구현 단위

### 1. Domain model audit

`Organize` aggregate와 `OrganizeMember` relation model의 경계를 다시 검토한다.

정리 방향:

- `Organize.create(...)`와 `Organize.reconstruct(...)`를 aggregate root 경계로 유지한다.
- `OrganizeCreatedEvent`를 collaboration domain event로 함께 이동한다.
- `OrganizeMember`는 organization 내부 collection보다 relation model로 유지한다.
- `OrganizeId`, `OrganizeName`, `OrganizeMemberRole`의 위치를 collaboration domain vo로 정렬한다.
- `OrganizeMember`의 기본 role은 `MEMBER`로 유지한다.

이 단계에서 실제로 옮길 대상은 아래와 같다.

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/domain/aggregate/Organize.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/aggregate/Organize.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/OrganizeMember.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/entity/OrganizeMember.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/event/OrganizeCreatedEvent.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/event/OrganizeCreatedEvent.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeId.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeId.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeName.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeName.java` |
| `app-server/src/main/java/io/jgitkins/server/domain/model/vo/OrganizeMemberRole.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/domain/vo/OrganizeMemberRole.java` |

Before/after 핵심 스니펫:

```java
// before
package io.jgitkins.server.domain.aggregate;
```

```java
// after
package io.jgitkins.server.collaboration.domain.aggregate;
```

이 단계에서 `OrganizeMember`의 소유 관계는 이렇게 정리한다.

```java
// before
// Organize aggregate가 member collection을 사실상 소유하는 것처럼 보이는 호출부가 존재할 수 있다.
```

```java
// after
// member 조회/추가는 application service가 담당하고,
// domain aggregate는 relation model의 불변식만 책임진다.
```

### 2. Application seam 분리

조직 생성, member 추가/제거, member 조회, 접근 가능 organization 조회를 하나의 top-level application 아래에 섞지 않고 역할별 service로 나눈다.

권장 구조:

```text
app-server/src/main/java/io/jgitkins/server/collaboration/application/service/
  OrganizeService
  OrganizeMemberService
  OrganizeAccessService
```

현재 코드에서의 이동 기준:

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeService.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/service/OrganizeService.java` |
| `app-server/src/main/java/io/jgitkins/server/application/service/OrganizeMemberService.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/service/OrganizeMemberService.java` |
| `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeValidator.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/validate/OrganizeValidator.java` |
| `app-server/src/main/java/io/jgitkins/server/application/validate/OrganizeMemberValidator.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/application/validate/OrganizeMemberValidator.java` |

핵심 스니펫:

```java
// before
@Service
@RequiredArgsConstructor
public class OrganizeService implements OrganizeCreationUseCase,
                                        OrganizeLoadUseCase,
                                        OrganizeDeletionUseCase {
```

```java
// after
@Service
@RequiredArgsConstructor
public class OrganizeService implements OrganizeCreationUseCase,
                                        OrganizeLoadUseCase,
                                        OrganizeDeletionUseCase {
    // package root만 collaboration/application/service로 이동
}
```

```java
// before
private final OrganizePersistencePort organizePort;
private final CurrentUserPort currentUserPersistencePort;
```

```java
// after
private final OrganizePersistencePort organizePort;
private final CurrentUserPort currentUserPersistencePort;
// import package가 collaboration/application/port로 바뀐다.
```

분리 기준:

- `OrganizeService`는 조직 생성/조회/삭제만 수행한다.
- `OrganizeMemberService`는 member add/remove/query만 수행한다.
- `OrganizeAccessService`는 owner/member 기준 접근 가능 여부와 namespace lookup 시 필요한 읽기 seam만 제공한다.
- `OrganizeValidator`는 조직 이름 중복, 존재 여부, 접근 가능 여부를 검증한다.
- `OrganizeMemberValidator`는 중복 멤버와 role 기본값만 책임진다.

### 3. Persistence and permission seam alignment

`OrganizePersistenceAdapter`, `OrganizeMemberPersistenceAdapter`, `OrganizeDomainMapper`는 persisted state만 안정적으로 다루도록 맞춘다.

정리 포인트:

- entity에 저장되는 필드와 aggregate root의 진짜 상태를 일치시킨다.
- `OrganizeMember`는 별도 table/entity로 유지한다.
- `OrganizeCreatedEvent`와 `Organize`의 저장 정책을 함께 정리한다.
- `RepositoryNamespaceResolver`와 `RepositoryAccessibilityService`는 collaboration query seam을 통해 organization name/member existence를 읽는다.
- membership 조회는 request마다 N번 찌르는 방식이 아니라, 가능한 한 bulk query seam을 제공한다.

이동 대상:

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizePersistenceAdapter.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/adapter/persistence/OrganizePersistenceAdapter.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/OrganizeMemberPersistenceAdapter.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/adapter/persistence/OrganizeMemberPersistenceAdapter.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeDomainMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/mapper/OrganizeDomainMapper.java` |
| `app-server/src/main/java/io/jgitkins/server/infrastructure/mapper/OrganizeMemberDomainMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure/mapper/OrganizeMemberDomainMapper.java` |

`RepositoryNamespaceResolver` / `RepositoryAccessibilityService`의 변경 예:

```java
// before
private final OrganizePersistencePort organizePort;
private final OrganizeMemberPersistencePort organizeMemberPort;
```

```java
// after
private final OrganizeQueryPort organizeQueryPort;
private final OrganizeMembershipQueryPort organizeMembershipQueryPort;
```

여기서 핵심은 `shared`가 persistence port를 직접 보는 게 아니라, collaboration의 read/query seam만 보게 바꾸는 것이다.

### 4. Presentation/API 정리

API는 organization 생성, member 관리, organization 조회, accessible organization 조회 흐름을 기준으로 ownership만 collaboration presentation으로 옮긴다.

정리 우선순위:

- organization 생성 API는 현재 사용자 owner를 기준으로 동작한다.
- organization member 추가/제거 API는 membership relation model을 직접 건드린다.
- accessible organization 조회 API는 현재 사용자 기준 membership/owner 계산을 수행한다.
- repository namespace / access calculation은 collaboration context의 query seam을 호출하는 방향으로 정리한다.

이동 대상:

| 현재 위치 | 목표 위치 |
|---|---|
| `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/rest/OrganizeController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/OrganizeMemberController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/rest/OrganizeMemberController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/api/web/WebOrganizeController.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/api/web/WebOrganizeController.java` |
| `app-server/src/main/java/io/jgitkins/server/presentation/mapper/OrganizeRequestMapper.java` | `app-server/src/main/java/io/jgitkins/server/collaboration/presentation/mapper/OrganizeRequestMapper.java` |

대표 흐름 예시:

```java
// before
@GetMapping("/organizes/accessible")
public List<OrganizeCreationResult> getAccessibleOrganizes() {
    return organizeService.getAccessibleOrganizes();
}
```

```java
// after
@GetMapping("/collaboration/organizes/accessible")
public List<OrganizeCreationResult> getAccessibleOrganizes() {
    return organizeService.getAccessibleOrganizes();
}
```

### 5. 상세 계획 문서 분리 기준

다음 4개 문서로 상세 구현안을 분리한다.

1. `task_2_23/01_collaboration_domain_boundary.md`
2. `task_2_23/02_collaboration_application_services.md`
3. `task_2_23/03_collaboration_persistence_and_shared_permissions.md`
4. `task_2_23/04_collaboration_presentation_and_tests.md`

## 흐름 다이어그램

### 1. Organization 생성

```text
Client
  -> OrganizationController
  -> OrganizeService
  -> OrganizeValidator.validateCreation()
  -> Organize.create()
  -> OrganizePersistencePort.save()
  -> OrganizeCreatedEvent
  -> result DTO
```

실패 가능성:

- 동일 organization name 중복
- owner user가 비어 있거나 invalid
- DB 저장 실패

### 2. Organization Member 추가 / 제거

```text
Client
  -> OrganizationMemberController
  -> OrganizeMemberService
  -> resolveRole(default = MEMBER)
  -> validateMemberNotExists()
  -> OrganizeMember.create()
  -> OrganizeMemberPersistencePort.save()/delete()
  -> result DTO
```

실패 가능성:

- 동일 `organizeId + userId` 중복
- role 누락
- membership row 삭제 시 대상 없음

### 3. Repository namespace / access 계산

```text
Repository load
  -> RepositoryNamespaceResolver
     -> organization owner ? collaboration query seam : user username

Repository visibility
  -> RepositoryAccessibilityService
     -> owner=user ? compare userId
     -> owner=organization ? membership query seam
     -> accessible / denied
```

실패 가능성:

- organization name lookup 실패
- membership row가 많아지면 N+1
- 현재 사용자 정보가 없을 때 접근 계산이 빈 값으로 떨어짐

## 테스트 기준

- `Organize` 생성 시 name, owner, timestamps가 일관되게 채워져야 한다.
- `Organize.reconstruct(...)`는 persisted state만 복원해야 한다.
- `OrganizeCreatedEvent`는 aggregate 생성 시 함께 발생해야 한다.
- `OrganizeMember.create(...)`는 `organizeId`, `userId`, `role` 필수 불변식을 지켜야 한다.
- organization 생성은 name 중복, invalid owner, DB save 실패를 다뤄야 한다.
- member 추가는 duplicate membership, default role, delete by key를 다뤄야 한다.
- repository namespace resolution은 owner type에 따라 organization name/user username을 나눠야 한다.
- repository accessibility는 public/private, owner/member, null requester를 모두 검증해야 한다.
- shared/repository가 collaboration persistence detail을 직접 import하지 않도록 architecture guardrail을 둔다.

## 구현 순서

1. `Organize` aggregate, `OrganizeMember` relation model, `OrganizeCreatedEvent`의 domain boundary를 collaboration package로 옮긴다.
2. `OrganizeService`, `OrganizeMemberService`, `OrganizeAccessService`로 application seam을 나눈다.
3. `OrganizeDomainMapper`와 persistence adapters를 collaboration package 기준으로 정리한다.
4. `RepositoryNamespaceResolver`와 `RepositoryAccessibilityService`를 collaboration query seam으로 전환한다.
5. presentation과 architecture guardrail을 정리하고, package-local bounded context가 안정화되면 module extraction 후보를 별도 계획으로 분리한다.

## 테스트 전략

- 도메인 테스트: organization 생성, member relation default role, aggregate event emission, invalid state를 검증한다.
- 애플리케이션 테스트: create/member/access 흐름의 happy path와 error path를 검증한다.
- mapper 테스트: entity와 aggregate 간 변환에서 persisted state만 왕복하는지 검증한다.
- shared/repository 테스트: namespace resolution과 access calculation이 collaboration query seam을 통해 동작하는지 검증한다.
- 구조 테스트: `collaboration` 관련 package가 identity/access persistence detail을 무단으로 끌어오지 않는지 확인한다.
- 성능 테스트: accessible organization 조회가 membership lookup N+1으로 커지지 않는지 확인한다.

### 테스트 커버리지 우선순위

```text
Organize.create()
  ├─ name 중복? -> validation test
  ├─ ownerId null? -> error path test
  └─ event emitted? -> domain test

OrganizeMember.create()
  ├─ organizeId null? -> error path test
  ├─ userId null? -> error path test
  ├─ role null? -> default role? 아니오, invalid path
  └─ duplicate membership? -> service test

RepositoryAccessibilityService
  ├─ public repository -> true
  ├─ user owner -> true/false
  ├─ organization owner + membership hit -> true
  └─ membership miss -> false
```

### 성능 메모

`OrganizeService.getAccessibleOrganizes()`는 현재 `findAll()` 뒤에 organization마다 membership 확인을 하기 쉽다.
조직 수가 많아지면 read path가 `O(N)` membership lookup으로 커질 수 있다.

이번 계획에서는 아래 둘 중 하나를 택한다.

1. bulk query port를 추가해 한 번에 membership 집합을 가져온다.
2. organization 수가 작고 확장 계획이 없으면 현재 방식 유지 후 후속 TODO로 남긴다.

권장안은 1번이다.
AI가 다루는 범위에서 bulk query를 미루는 건, 결국 다음 사람이 밤에 같은 코드 줄을 다시 만지는 일이다.

## Worktree parallelization strategy

3개 작업 흐름이 가능하다.

| Step | Modules touched | Depends on |
|------|----------------|------------|
| Domain boundary move | `domain/`, `domain/event/`, `domain/vo/` | — |
| Application seam split | `application/service/`, `application/validate/`, `application/port/` | Domain boundary move |
| Persistence + shared permission seam | `infrastructure/`, `shared/application/`, `repository/application/` | Domain boundary move, Application seam split |
| Presentation + tests | `presentation/`, `test/` | Application seam split, Persistence + shared permission seam |

Parallel lanes:

- Lane A: Domain boundary move
- Lane B: Application seam split
- Lane C: Persistence + shared permission seam
- Lane D: Presentation + tests

실행 순서:

1. A를 먼저 끝낸다.
2. B와 C는 분리 가능하면 병렬로 간다.
3. D는 B/C의 계약이 정해진 뒤 붙인다.

Conflict flags:

- B와 C는 `application/port/`와 `shared/` 의존성이 겹칠 수 있다.
- D는 controller DTO와 mapper가 겹치므로, B/C 계약이 아직 흔들리면 merge conflict가 쉽게 난다.

## 완료 기준

- `Collaboration Context`가 `io.jgitkins.server.collaboration` 패키지로 승격된다.
- `Organize` / `OrganizeMember` / `OrganizeCreatedEvent` / 관련 VO와 port가 collaboration boundary 안으로 정렬된다.
- repository namespace / access calculation이 collaboration query seam을 통해 동작한다.
- 문서와 코드의 용어가 `Organization` 중심으로 정리되지만, 전면 rename은 하지 않는다.
- 추후 구현이 문서만 보고도 가능할 정도로 파일 범위와 테스트 전략이 구체적이다.

## NOT in scope

- 이 단계에서 DB schema 변경은 하지 않는다.
- 이 단계에서 Gradle module extraction은 하지 않는다.
- 이 단계에서 `Organize` 클래스명을 `Organization`으로 전면 rename하지 않는다.
- 이 단계에서 repository domain ownership model을 다시 설계하지 않는다.
- 이 단계에서 identity/access의 user 모델을 다시 옮기지 않는다.
- 이 단계에서 repository visibility 정책에 팀/그룹/권한 계층을 새로 추가하지 않는다.

## 후속 검토 포인트

- `OrganizeService.getAccessibleOrganizes()`의 membership lookup을 bulk query로 바꿀지 결정해야 한다.
- `RepositoryAccessibilityService`와 `RepositoryNamespaceResolver`가 collaboration query seam만 보도록 할지, shared seam으로 한 번 더 감쌀지 결정해야 한다.

## 후속 subtask

- **명명 rename 분리**: `Organize` / `OrganizeMember` / related mapper, event, test 이름을 `Organization` 계열로 바꾸는 작업은 별도 subtask로 분리한다.
- **이유**: 패키지 경계 정리와 동시에 이름까지 바꾸면 diff가 커지고, 경계 버그와 rename 버그를 한 번에 디버깅해야 한다.
- **범위**: 파일명, 클래스명, 테스트명, mapper명, event명, controller/service/persistence naming 일괄 변경.
- **시점**: collaboration package 승격과 permission seam 정리가 안정화된 뒤.
