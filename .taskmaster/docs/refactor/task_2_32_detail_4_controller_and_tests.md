# Task 2.32 Detail 4: Controller와 Test 수정 계획

### 목적
- 이 문서는 Repository Overview 이관 이후 presentation adapter와 테스트를 어떻게 바꿀지 정의한다.
- endpoint path와 response schema는 유지한다.
- `WebRepositoryController`는 web internal/BFF adapter 성격에 맞춰 `io.jgitkins.server.presentation.api.web`에 둔다.

### `WebRepositoryController` 위치 변경
```text
FROM server/src/main/java/io/jgitkins/server/repository/presentation/api/web/WebRepositoryController.java
TO   server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java
```

```java
package io.jgitkins.server.presentation.api.web;
```

### `WebRepositoryController` import 변경
```java
// AS-IS
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```

```java
// 1차 TO-BE
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```

### `WebRepositoryController` 1차 구현
```java
@Operation(summary = "Get Repository Overview by Namespace/Repo (Web)")
@GetMapping("/{namespace}/{repoName}/overview")
public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getRepositoryOverviewByPath(
        @PathVariable String namespace,
        @PathVariable String repoName,
        @RequestParam(name = "branch", required = false) String branch
) {
    RepositoryResult repository = repositoryLoadUseCase.loadRepositoryByPath(namespace, repoName);
    return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repository.id(), branch));
}
```

### `WebRepositoryController` 2차 개선 후보
```java
@Operation(summary = "Get Repository Overview by Namespace/Repo (Web)")
@GetMapping("/{namespace}/{repoName}/overview")
public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getRepositoryOverviewByPath(
        @PathVariable @NotBlank String namespace,
        @PathVariable @NotBlank String repoName,
        @RequestParam(name = "branch", required = false) String branch
) {
    return ApiResponse.ok(repositoryOverviewUseCase.getOverviewByPath(namespace, repoName, branch));
}
```

### `RepositoryManagementController` import 변경
```java
// AS-IS
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```

```java
// TO-BE
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```

### `RepositoryManagementController` endpoint 유지
```java
@Operation(summary = "Get Repository Overview")
@GetMapping("/{repositoryId}/overview")
public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getOverview(
        @PathVariable Long repositoryId,
        @RequestParam(name = "branch", required = false) String branch
) {
    return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repositoryId, branch));
}
```

### `RepositoryOverviewServiceTest` 이동
```text
FROM server/src/test/java/io/jgitkins/server/application/service/RepositoryOverviewServiceTest.java
TO   server/src/test/java/io/jgitkins/server/repository/application/service/RepositoryOverviewServiceTest.java
```

### `RepositoryOverviewServiceTest` package
```java
package io.jgitkins.server.repository.application.service;
```

### `RepositoryOverviewServiceTest` import
```java
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import java.util.List;
import java.util.Optional;
```

### service test 검증 포인트
- repository id 조회 시 `RepositoryQueryPort.loadRepository(repositoryId)`를 호출한다.
- branch 목록 조회 시 `BranchQueryPort.findAllByRepositoryId(repositoryId)`를 호출한다.
- file tree 조회 시 `FileGitPort.listTree(namespace, repoName, selectedBranch, "")`를 호출한다.
- 권한 계산 시 `GitRepositoryAccessService.resolvePermission(null, namespace, repoName, userId)`를 호출한다.

```java
@Test
void getOverview_loadsRepositoryBranchesTreeAndPermission() {
    RepositoryResult repository = new RepositoryResult(
            1L, "USER", "demo", "team/demo", "main", "PUBLIC",
            null, 10L, null, "team/demo.git", null, false,
            null, null, null);
    List<BranchSearchResult> branches = List.of(new BranchSearchResult(1L, "main", false, true, true));
    List<FileEntry> tree = List.of(FileEntry.builder().name("README.md").build());

    given(repositoryQueryPort.loadRepository(1L)).willReturn(Optional.of(repository));
    given(branchQueryPort.findAllByRepositoryId(1L)).willReturn(branches);
    given(fileGitPort.listTree("team", "demo", "main", "")).willReturn(tree);
    given(currentUserPort.resolveCurrentUserId()).willReturn(Optional.of(10L));
    given(gitRepositoryAccessService.resolvePermission(null, "team", "demo", 10L))
            .willReturn(new RepositoryPermission("OWNER", true, true));

    RepositoryOverviewResult result = service.getOverview(1L, null);

    assertEquals("main", result.selectedBranch());
    assertTrue(result.writable());
}
```

### controller test 예시
```java
@Test
void getRepositoryOverviewByPath_resolvesRepositoryAndDelegatesToOverviewUseCase() throws Exception {
    RepositoryResult repository = new RepositoryResult(
            1L, "USER", "demo", "team/demo", "main", "PUBLIC",
            null, 10L, null, "team/demo.git", null, false,
            null, null, null);
    RepositoryOverviewResult overview = new RepositoryOverviewResult(
            repository,
            List.of(),
            List.of(),
            "main",
            "OWNER",
            true);

    given(repositoryLoadUseCase.loadRepositoryByPath("team", "demo")).willReturn(repository);
    given(repositoryOverviewUseCase.getOverview(1L, "main")).willReturn(overview);

    mockMvc.perform(get("/api/internal/repositories/team/demo/overview")
                    .param("branch", "main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.selectedBranch").value("main"))
            .andExpect(jsonPath("$.data.writable").value(true));
}
```

### ArchitecturePackageConventionTest 수정
```java
import io.jgitkins.server.repository.application.service.RepositoryOverviewService;

@Test
void applicationServices_resideInUnifiedServicePackage() {
    List<Class<?>> serviceClasses = List.of(
            AdminUserService.class,
            CommitService.class,
            MergeService.class,
            OAuthLoginService.class,
            OrganizeMemberService.class,
            OrganizeService.class,
            PublicUserQueryService.class,
            PushEventHandleService.class,
            RepositoryFileService.class,
            UserCredentialService.class,
            UserProfileService.class);

    serviceClasses.forEach(serviceClass -> assertEquals(APPLICATION_SERVICE_PACKAGE, serviceClass.getPackageName()));
}

@Test
void repositoryContextServices_resideInRepositoryServicePackage() {
    List<Class<?>> serviceClasses = List.of(
            BranchLoadService.class,
            BranchManagementService.class,
            RepositoryLoadService.class,
            RepositoryManagementService.class,
            RepositoryMemberService.class,
            RepositoryOverviewService.class);

    serviceClasses.forEach(serviceClass -> assertEquals(REPOSITORY_SERVICE_PACKAGE, serviceClass.getPackageName()));
}
```

### 완료 조건
- controller endpoint path가 바뀌지 않는다.
- `RepositoryOverviewResult` field 이름이 바뀌지 않는다.
- controller test는 새 `RepositoryOverviewUseCase` package를 mock으로 사용한다.
- service test는 inbound UseCase mock 대신 query/out port mock을 사용한다.
