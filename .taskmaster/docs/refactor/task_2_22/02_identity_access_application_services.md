# 02. Identity & Access Application Services 상세 계획

## 목적

Identity & Access Context의 application seam을 분리한다.

현재 `OAuthLoginService`, `UserProfileService`, `UserCredentialService`, `AdminUserService`, `PublicUserQueryService`, `UserService`가 하나의 top-level application 네임스페이스에 섞여 있다. 이 문서는 이들을 `identity.access.application` 아래로 재배치하는 계획을 정의한다.

## AS-IS

```text
app-server/src/main/java/io/jgitkins/server/application/service/OAuthLoginService.java
app-server/src/main/java/io/jgitkins/server/application/service/UserProfileService.java
app-server/src/main/java/io/jgitkins/server/application/service/UserCredentialService.java
app-server/src/main/java/io/jgitkins/server/application/service/AdminUserService.java
app-server/src/main/java/io/jgitkins/server/application/service/PublicUserQueryService.java

app-server/src/main/java/io/jgitkins/server/application/support/UserService.java
app-server/src/main/java/io/jgitkins/server/application/support/UserProfileUpdater.java
app-server/src/main/java/io/jgitkins/server/application/support/UsernameAllocator.java

app-server/src/main/java/io/jgitkins/server/application/port/in/OAuthLoginUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/UserCredentialIssueUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/UserCredentialQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/UserCredentialRevokeUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/PublicUserQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/AdminUserQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/application/port/in/AdminUserUpdateUseCase.java

app-server/src/main/java/io/jgitkins/server/application/port/out/UserPersistencePort.java
app-server/src/main/java/io/jgitkins/server/application/port/out/UserIdentityPersistencePort.java
app-server/src/main/java/io/jgitkins/server/application/port/out/UserCredentialPersistencePort.java
app-server/src/main/java/io/jgitkins/server/application/port/out/TokenIssuerPort.java
app-server/src/main/java/io/jgitkins/server/application/port/out/CurrentUserPort.java
```

## TO-BE

```text
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/OAuthLoginService.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/UserProfileService.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/UserCredentialService.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/AdminUserService.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/service/PublicUserQueryService.java

app-server/src/main/java/io/jgitkins/server/identity/access/application/support/UserService.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/support/UserProfileUpdater.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/support/UsernameAllocator.java

app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/OAuthLoginUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/UserCredentialIssueUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/UserCredentialQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/UserCredentialRevokeUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/PublicUserQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/AdminUserQueryUseCase.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/in/AdminUserUpdateUseCase.java

app-server/src/main/java/io/jgitkins/server/identity/access/application/port/out/UserPersistencePort.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/out/UserIdentityPersistencePort.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/out/UserCredentialPersistencePort.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/out/TokenIssuerPort.java
app-server/src/main/java/io/jgitkins/server/identity/access/application/port/out/CurrentUserPort.java
```

## 결정 사항

- `OAuthLoginService`는 외부 identity 확인, user upsert, app token 발급만 수행한다.
- `UserProfileService`는 username activation과 profile update만 수행한다.
- `UserCredentialService`는 PAT issue / query / revoke만 수행한다.
- `AdminUserService`는 user / identity 조회와 status update만 수행한다.
- `PublicUserQueryService`는 공개 프로필 조회만 수행한다.
- `UserService`는 `OAuthLoginService` 내부 helper가 아니라, login-or-signup orchestration helper로 제한한다.
- `UsernameAllocator`는 username / namespace collision 검증 helper로 유지한다.
- `UserProfileUpdater`는 identity profile sync helper로 유지한다.
- `CurrentUserPort`와 `TokenIssuerPort`는 application boundary 외부 입력/출력용 port로 유지한다.

## 코드 스니펫

### OAuth login flow

```java
public OAuthLoginResult login(OAuthLoginCommand command) {
    UserIdentity identity = userService.loginOrSignUp(command);
    String token = tokenIssuerPort.issue(identity.getUserId(), identity.getProviderName());
    return oauthLoginMapper.toResult(identity, token);
}
```

### Username activation flow

```java
public UserActivationResult activate(UserUsernameUpdateCommand command) {
    User user = currentUserPort.getCurrentUser();
    usernameAllocator.validate(command.username(), user.getId());
    user.activateWithUsername(new Username(command.username()));
    return userMapper.toActivationResult(userPersistencePort.save(user));
}
```

### PAT issue flow

```java
public UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command) {
    Long userId = currentUserPort.getCurrentUserId();
    String token = tokenFactory.generate();
    UserCredential credential = UserCredential.issue(userId, command.name(), command.description(), tokenHasher.hash(token));
    UserCredential saved = userCredentialPort.save(credential);
    return new UserCredentialIssueResult(saved.getId(), token);
}
```

## 테스트 기준

- OAuth login은 외부 identity 연결과 앱 토큰 발급이 분리되어야 한다.
- Username activation은 현재 사용자 경계와 namespace collision 검증을 모두 통과해야 한다.
- PAT issue는 원문 token과 hash 저장 경계가 분리되어야 한다.
- PAT revoke는 credentialId + current user 경계를 벗어나면 실패해야 한다.
- Admin user update는 status / identity / profile 업데이트 경계를 섞지 않아야 한다.

## 완료 기준

- login / activation / credential / admin query의 application seam이 분리된다.
- port 소유권이 `identity.access.application` 아래로 정리된다.
- helper와 use case가 같은 패키지에서 역할별로 구분된다.
