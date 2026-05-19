# Task 2.22 리팩토링 계획

## 제목

- **리팩토링 계획**: P4 Identity & Access Context 기준 패키지 승격 및 경계 재정렬
- **후속 상세 계획 단위**: `task_2_22/` 하위에 Domain, Application, Persistence, Presentation/Test 단위로 분리 작성

## 배경

`Identity & Access Context`는 사용자 식별, 외부 로그인, username 활성화, 개인 credential 발급을 다룬다.

현재 관련 코드는 `app-server` 안에 흩어져 있다.

- `app-server/src/main/java/io/jgitkins/server/domain/model/User.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserIdentity.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserCredential.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/OAuthLoginService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/UserProfileService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/UserCredentialService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/AdminUserService.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserIdentityPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserCredentialPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/AdminUserController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserCredentialController.java`

이 작업의 목적은 기능 추가가 아니라 bounded context 경계를 app-server 내부에서 먼저 선명하게 만드는 것이다.
즉, `Identity & Access Context`를 `io.jgitkins.server.identity.access` 패키지 아래로 먼저 모으고, 그 다음 단계에서 Gradle module extraction을 검토한다.

분리 축은 둘이다.

- persisted state: `User`가 직접 소유해야 하는 상태
- computed result: 로그인/토큰/조회 시점에 다시 계산해야 하는 결과

## 목표

- `Identity & Access Context`를 `app-server` 내부의 `io.jgitkins.server.identity.access` 패키지로 승격한다.
- `User` aggregate가 소유할 값과 `UserIdentity`, `UserCredential`의 종속 모델 경계를 다시 고정한다.
- OAuth 로그인, username 활성화, PAT 발급/조회/폐기 책임을 application seam으로 분리한다.
- namespace 충돌과 collaboration 연계 포인트를 사용자 활성화 경계에서 다시 정리한다.
- MBG 영속성 계층은 persisted state만 다루도록 맞춘다.
- 이 단계에서는 Gradle module extraction을 하지 않고, package-local bounded context를 먼저 완성한다.

## 핵심 결정

### 1. Context 이름과 Java package root는 분리해서 본다

- 문서와 도메인 모델의 이름: `Identity & Access Context`
- Java package root: `io.jgitkins.server.identity.access`
- 파일 경로 관례: `identity/access`

이 작업에서는 `identity_access` 같은 비표준 표기를 쓰지 않는다.

### 2. app-server는 composition root로 남긴다

`app-server`는 현재 단계에서 조립자 역할을 유지한다.

- repository, execution, change-review, shared, collaboration은 각자 별도 context로 남는다.
- identity/access만 먼저 `app-server` 내부에서 패키지 승격을 진행한다.
- module extraction은 후속 단계다.

### 3. User 중심 모델과 credential 모델을 분리해서 본다

- `User`는 aggregate root candidate다.
- `UserIdentity`는 외부 identity를 내부 사용자와 연결하는 종속 모델이다.
- `UserCredential`은 PAT provider를 표현하는 종속 모델이다.
- PAT 원문과 OAuth 앱 토큰은 저장 모델이 아니라 computed result다.

### 4. Username 활성화는 cross-context seam을 가진다

`User` 활성화는 `Identity & Access Context` 안에서 일어나지만, namespace 충돌 검증은 `Repository`와 `Collaboration` context의 정책을 참조할 수 있다.

- user name 자체의 중복 검증
- organization / namespace 충돌 검증
- 활성화 후 collaboration owner / repository owner와의 연결 가능성

이 경계는 application service에서 처리하고, domain aggregate는 상태 전이만 책임진다.

## TO-BE 패키지 방향

```text
app-server/src/main/java/io/jgitkins/server/identity/access/
  domain/
    aggregate/
    entity/
    repository/
    vo/
    exception/
  application/
    service/
    port/in/
    port/out/
    dto/command/
    dto/result/
    support/
    mapper/
    exception/
  infrastructure/
    adapter/persistence/
    adapter/security/
    mapper/
    persistence/model/
    persistence/mapper/
  presentation/
    api/rest/
    dto/
    mapper/
```

이 구조의 핵심은 다음이다.

- `domain`은 `User`와 그 종속 모델을 가진다.
- `application`은 OAuth login, activation, PAT issue/revoke seam을 가진다.
- `infrastructure`는 MBG entity, persistence adapter, security adapter를 가진다.
- `presentation`은 user/admin/credential API를 가진다.

## 구현 단위

### 1. Domain model audit

`User` aggregate와 종속 모델의 경계를 다시 검토한다.

정리 방향:

- `User.create(...)`와 `User.rehydrate(...)`를 aggregate root 경계로 유지한다.
- `UserIdentity`는 외부 provider identity 연결 모델로 유지한다.
- `UserCredential`은 PAT 발급/조회/폐기용 종속 모델로 유지한다.
- `UserStatus`의 상태 전이는 `PENDING` -> `ACTIVE` 중심으로 유지한다.
- `UserAuthority`, `Username`, `UserId`, `SystemUser`의 위치를 identity.access domain으로 정렬한다.

### 2. Application seam 분리

인증, 활성화, credential, admin 조회를 하나의 top-level application 아래에 두지 않고 역할별 service로 분리한다.

권장 구조:

```text
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/
  OAuthLoginService
  UserProfileService
  UserCredentialService
  AdminUserService
  PublicUserQueryService
```

분리 기준:

- OAuth login service는 외부 identity 확인 + user upsert + app token 발급만 수행한다.
- Profile service는 username activation과 사용자 프로필 변경만 수행한다.
- Credential service는 PAT issue/query/revoke만 수행한다.
- Admin service는 user / identity 조회와 status update만 수행한다.

### 3. Persistence and security adapter alignment

`UserDomainMapper`, `UserIdentityDomainMapper`, `UserCredentialDomainMapper`는 persisted state만 안정적으로 다루도록 맞춘다.

정리 포인트:

- entity에 저장되는 필드와 aggregate root의 진짜 상태를 일치시킨다.
- `UserIdentity`, `UserCredential`은 별도 table/entity로 유지한다.
- `CurrentUserSecurityAdapter`와 `PatTokenAuthenticationService`는 security adapter로 분리한다.
- `TokenIssuerPort`는 app token 발급 경계로만 남긴다.
- `PasswordEncoder`는 PAT hash 생성 도구로만 사용한다.

### 4. Presentation/API 정리

API는 login, activation, admin user, PAT credential 흐름을 기준으로 유지하되 ownership만 identity.access presentation으로 옮긴다.

정리 우선순위:

- OAuth login API는 로그인 결과와 app token을 반환한다.
- username activation API는 현재 사용자 경계를 기준으로 동작한다.
- PAT 발급/조회/폐기 API는 credential presentation으로 묶는다.
- admin user API는 사용자, identity, status 변경을 담당한다.

### 5. 상세 계획 문서 분리 기준

다음 4개 문서로 상세 구현안을 분리한다.

1. `task_2_22/01_identity_access_domain_boundary.md`
2. `task_2_22/02_identity_access_application_services.md`
3. `task_2_22/03_identity_access_persistence_and_security.md`
4. `task_2_22/04_identity_access_presentation_and_tests.md`

## 상세 파일 범위

### 우선 검토 대상

- `app-server/src/main/java/io/jgitkins/server/domain/model/User.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserIdentity.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserCredential.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserAuthority.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/UserStatus.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/UserId.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/Username.java`
- `app-server/src/main/java/io/jgitkins/server/domain/model/vo/SystemUser.java`
- `app-server/src/main/java/io/jgitkins/server/domain/exception/UserAlreadyActivatedException.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/OAuthLoginService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/UserProfileService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/UserCredentialService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/AdminUserService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/PublicUserQueryService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/UserService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/UserProfileUpdater.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/UsernameAllocator.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/UserPersistencePort.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/UserIdentityPersistencePort.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/UserCredentialPersistencePort.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/TokenIssuerPort.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/out/CurrentUserPort.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserIdentityPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/persistence/UserCredentialPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/adapter/security/CurrentUserSecurityAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/infrastructure/config/security/auth/PatTokenAuthenticationService.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/AdminUserController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserCredentialController.java`

### 테스트 보강 대상

- `app-server/src/test/java/io/jgitkins/server/application/service/OAuthLoginServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/service/UserProfileServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/service/UserCredentialServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/service/AdminUserServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/support/UserServiceTest.java`
- `app-server/src/test/java/io/jgitkins/server/infrastructure/mapper/UserDomainMapperTest.java`
- `app-server/src/test/java/io/jgitkins/server/infrastructure/mapper/UserIdentityDomainMapperTest.java`
- `app-server/src/test/java/io/jgitkins/server/infrastructure/mapper/UserCredentialDomainMapperTest.java`
- `app-server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`

## 구현 순서

1. `User` aggregate와 종속 모델의 persisted/computed 경계를 재확인한다.
2. `OAuthLoginService`, `UserProfileService`, `UserCredentialService`, `AdminUserService`로 application seam을 나눈다.
3. `UserDomainMapper`와 persistence adapters를 identity.access package 기준으로 정리한다.
4. presentation과 security adapter를 정리하고, architecture guardrail을 추가한다.
5. package-local bounded context가 안정화되면 module extraction 후보를 별도 계획으로 분리한다.

## 테스트 전략

- 로그인 테스트: OAuth login에서 `User`와 `UserIdentity`가 일관되게 저장되는지 검증한다.
- 활성화 테스트: username activation에서 `UserStatus` 전이가 정상이고 namespace 충돌이 차단되는지 검증한다.
- PAT 테스트: issue/list/revoke에서 원문 token과 hash 저장 경계가 분리되는지 검증한다.
- mapper 테스트: entity와 aggregate 간 변환에서 persisted state만 왕복하는지 검증한다.
- 구조 테스트: `identity.access` 관련 package가 repository/shared seam을 무단으로 끌어오지 않는지 확인한다.

## 완료 기준

- `Identity & Access Context`가 `io.jgitkins.server.identity.access` 패키지로 승격된다.
- OAuth login / username activation / PAT credential 경계가 문서와 코드 후보 기준으로 정리된다.
- MBG persistence와 application seam의 우선순위가 명시된다.
- `UserService`와 `UserCredentialService`가 각각 어떤 책임만 남길지 결정된다.
- 추후 구현이 문서만 보고도 가능할 정도로 파일 범위와 테스트 전략이 구체적이다.

## NOT in scope

- 이 단계에서 DB schema 변경은 하지 않는다.
- 이 단계에서 Gradle module extraction은 하지 않는다.
- 이 단계에서 repository context package와 shared context를 다시 이동하지 않는다.
- 이 단계에서 change-review context를 identity.access 안으로 끌어오지 않는다.
- 이 단계에서 collaboration context의 ownership model을 재설계하지 않는다.
