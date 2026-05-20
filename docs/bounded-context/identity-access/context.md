## Identity & Access Context

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념과 유비쿼터스 언어](#핵심-개념과-유비쿼터스-언어)
- [User](#user)
- [User Identity](#user-identity)
- [User Credential](#user-credential)
- [Activation](#activation)
- [Aggregate / Entity / Value Object 경계](#aggregate--entity--value-object-경계)
- [Aggregate Root Candidate: User](#aggregate-root-candidate-user)
- [Entity Candidate: User Identity](#entity-candidate-user-identity)
- [Entity Candidate: User Credential](#entity-candidate-user-credential)
- [주요 Value Objects](#주요-value-objects)
- [상태](#상태)
- [불변식](#불변식)
- [주요 시나리오](#주요-시나리오)
- [1. OAuth 로그인](#1-oauth-로그인)
- [2. Username 활성화](#2-username-활성화)
- [3. PAT 발급과 조회](#3-pat-발급과-조회)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [미확정 쟁점](#미확정-쟁점)

### 문제 정의

`Identity & Access Context`는 사용자 식별, 외부 로그인, username 활성화, 개인 credential 발급을 다룬다.

이 문서의 목적은 다음 질문에 답하는 것이다.

- `User`가 직접 소유하는 상태는 무엇인가
- 외부 OAuth identity는 `User`와 어떤 관계인가
- PAT credential은 `User` 내부 모델인가 별도 aggregate인가
- username 활성화와 토큰 발급은 어느 경계에서 처리되는가

### 책임 범위

이 context의 책임은 다음과 같다.

- 사용자 생성과 조회
- OAuth 로그인과 앱 토큰 발급
- 외부 identity 연결
- username 활성화
- PAT credential 발급, 조회, 폐기
- 사용자 상태 변경

직접 소유하지 않는 책임은 다음과 같다.

- 저장소 생성과 권한 계산
- 조직 생성과 조직 멤버십 관리
- repository/organization 기반 접근 제어의 최종 판단
- Git repository read/write 실행

### 핵심 개념과 유비쿼터스 언어

#### User

서비스의 사용자 계정이다.

#### User Identity

외부 인증 제공자에서 온 사용자 신원이다.

#### User Credential

사용자가 발급받는 개인 credential이다. 현재 provider는 `PAT`다.

#### Activation

PENDING 상태 사용자가 username을 확정하고 ACTIVE로 전환하는 과정이다.

### Aggregate / Entity / Value Object 경계

#### Aggregate Root Candidate: User

`User`는 현재 이 context의 중심 모델이다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/User.java`
- 유스케이스 근거: `OAuthLoginUseCase`, `SignupUseCase`, `AdminUserUpdateUseCase`
- 서비스 근거: `OAuthLoginService`, `UserProfileService`, `AdminUserService`

`User`가 직접 소유하거나 결정하는 값은 다음과 같다.

- `id`
- `username`
- `email`
- `displayName`
- `avatarUrl`
- `UserAuthority`
- `UserStatus`
- `lastLoginAt`
- `createdAt`
- `updatedAt`

#### Entity Candidate: User Identity

`UserIdentity`는 현재 `User`에 종속된 외부 identity 모델로 본다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/UserIdentity.java`
- 식별: `id`
- 연결: `userId`, `providerName`, `providerSub`

독립 생명주기보다 OAuth 로그인 결과를 내부 사용자와 연결하는 역할이 강하다.

#### Entity Candidate: User Credential

`UserCredential`은 현재 `User`에 종속된 credential 모델로 본다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/UserCredential.java`
- 식별: `id`
- 연결: `userId`
- provider: 현재 `PAT`

현재 구현은 발급, 조회, 폐기 흐름에 집중되어 있다.

#### 주요 Value Objects

이 context의 주요 Value Object는 다음과 같다.

- `Username`
- `UserId`
- `UserStatus`
- `UserAuthority`

### 상태

`UserStatus`는 현재 `ACTIVE`, `PENDING` 등을 가진다. 문서 기준 핵심 상태는 다음 둘이다.

- `PENDING`
- `ACTIVE`

현재 흐름은 다음과 같다.

- OAuth 로그인으로 생성된 사용자는 `PENDING`일 수 있다.
- username 활성화 후 `ACTIVE`가 된다.

### 불변식

현재 기준 불변식은 다음과 같다.

1. `User` 생성 시 username은 필수다.
2. `UserIdentity` 생성 시 `userId`, `providerName`, `providerSub`는 필수다.
3. `UserCredential` 발급 시 `userId`, `name`, `passwordHash`는 필수다.
4. username 활성화는 `PENDING` 상태에서만 가능하다.
5. username 활성화 시 user username 중복이 없어야 한다.
6. username 활성화 시 organize name과의 충돌도 검증한다.

### 주요 시나리오

#### 1. OAuth 로그인

현재 흐름은 다음과 같다.

1. `OAuthLoginService`가 OAuth 로그인 요청을 받는다.
2. `UserService.loginOrSignUp(...)`로 사용자 로그인 또는 생성을 수행한다.
3. `TokenIssuerPort`가 앱 토큰을 발급한다.
4. `OAuthLoginResult`를 반환한다.

#### 2. Username 활성화

현재 흐름은 다음과 같다.

1. `UserProfileService.activate(...)`가 username 요청을 받는다.
2. `ActivationValidator`가 username 형식과 중복을 검증한다.
3. organize name 충돌을 검증한다.
4. 사용자 repository 존재 여부를 검증한다.
5. `user.activateWithUsername(...)`를 호출한다.
6. 변경된 `User`를 저장한다.

#### 3. PAT 발급과 조회

현재 흐름은 다음과 같다.

1. `UserCredentialService`가 현재 사용자 id를 조회한다.
2. 랜덤 토큰을 생성한다.
3. 토큰을 해시한다.
4. `UserCredential.issue(...)`로 credential을 만든다.
5. credential을 저장한다.
6. 발급 시 원문 토큰은 한 번만 반환한다.
7. 조회는 `provider=PAT` 기준으로 수행한다.
8. 폐기는 `credentialId + userId` 기준으로 수행한다.

### 외부 시스템과의 경계

외부 경계는 다음과 같다.

- `UserPersistencePort`
  - 사용자 저장, 조회
- `UserIdentityPersistencePort`
  - 외부 identity 저장, 조회
- `UserCredentialPersistencePort`
  - credential 저장, 조회, 삭제
- `CurrentUserPort`
  - 현재 사용자 식별
- `TokenIssuerPort`
  - 앱 토큰 발급
- `PasswordEncoder`
  - PAT 해시 생성

원칙:

- 외부 로그인 provider 정보는 `UserIdentity`로 분리한다.
- 앱 토큰 발급과 PAT 발급은 같은 token 개념이 아니라 다른 경계다.
- PAT 원문은 저장하지 않고 해시만 저장한다.

### 다른 Context와의 연결

- `Repository Context`
  - repository owner가 user일 수 있다.
- `Collaboration Context`
  - organize owner와 organize member가 user와 연결된다.
- `Shared / Cross-Cutting Topics`
  - namespace 충돌 규칙과 접근 정책에 영향을 준다.

### 미확정 쟁점

1. `User`를 명시적 aggregate root로 확정할지
   - 현재는 중심 모델이지만 aggregate 경계는 문서 수준에서만 암묵적이다.
2. `UserCredential`을 별도 aggregate로 승격할지
   - 현재는 `User` 종속 모델로 충분하지만 감사, 만료, 사용 이력 요구가 커지면 분리할 수 있다.
