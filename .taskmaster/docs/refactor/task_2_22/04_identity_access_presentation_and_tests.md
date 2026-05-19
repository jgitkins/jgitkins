# 04. Identity & Access Presentation and Tests 상세 계획

## 목적

Identity & Access Context의 REST API와 테스트를 새 package root 아래로 이동한다.

## AS-IS

```text
app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserController.java
app-server/src/main/java/io/jgitkins/server/presentation/api/rest/AdminUserController.java
app-server/src/main/java/io/jgitkins/server/presentation/api/rest/UserCredentialController.java

app-server/src/main/java/io/jgitkins/server/presentation/dto/OAuthLoginRequest.java
app-server/src/main/java/io/jgitkins/server/presentation/dto/UserUsernameUpdateRequest.java
app-server/src/main/java/io/jgitkins/server/presentation/dto/UserStatusUpdateRequest.java
app-server/src/main/java/io/jgitkins/server/presentation/dto/UserCredentialIssueRequest.java
```

## TO-BE

```text
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/api/rest/UserController.java
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/api/rest/AdminUserController.java
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/api/rest/UserCredentialController.java

app-server/src/main/java/io/jgitkins/server/identity/access/presentation/dto/OAuthLoginRequest.java
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/dto/UserUsernameUpdateRequest.java
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/dto/UserStatusUpdateRequest.java
app-server/src/main/java/io/jgitkins/server/identity/access/presentation/dto/UserCredentialIssueRequest.java
```

## 결정 사항

- `UserController`는 OAuth login, current profile, activation 요청을 담당한다.
- `AdminUserController`는 admin query/update를 담당한다.
- `UserCredentialController`는 PAT issue/query/revoke를 담당한다.
- endpoint path는 가능한 기존 계약을 유지한다.
- controller는 use case 호출만 하고 domain 계산 로직은 가지지 않는다.
- request/response DTO는 presentation package 안에만 남긴다.

## 테스트 기준

- OAuth login endpoint는 DTO binding과 use case invocation을 검증한다.
- username activation endpoint는 current user boundary를 검증한다.
- PAT issue/query/revoke endpoint는 credential lifecycle 경계를 검증한다.
- admin endpoint는 user status/identity update 경계를 검증한다.
- `ArchitecturePackageConventionTest`는 identity.access package가 top-level legacy package를 끌어오지 않는지 검증한다.

## 완료 기준

- user/admin/credential API ownership이 identity.access presentation으로 정리된다.
- DTO와 controller가 새 package root 기준으로 이동한다.
- 테스트가 새 패키지 경로를 기준으로 정렬된다.
