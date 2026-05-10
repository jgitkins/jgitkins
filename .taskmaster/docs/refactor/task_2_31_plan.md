# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.31 Repository Context presentation adapter 패키지 재정렬 계획서

### 배경
- `repository` application/domain은 상당 부분 context 기준으로 정리됐지만, inbound adapter는 아직 top-level `presentation/api/rest`, `presentation/api/web` 아래에 평면적으로 남아 있다.
- 이 때문에 `RepositoryManagementController`, `BranchController`, `RepositoryMemberController`, `RepositoryContentController`, `RepositoryCommitController`, `RepositoryFileController`, `WebRepositoryController`가 같은 context의 adapter임에도 코드 위치에서 응집이 드러나지 않는다.
- request dto와 request mapper도 여전히 top-level `presentation/dto`, `presentation/mapper`에 남아 있어 repository context 경계가 presentation 계층에서 끊겨 보인다.
- 특히 `RepositoryManagementController`는 management/load 외에 overview endpoint까지 함께 다뤄 repository context 기준 정렬이 덜 끝난 것처럼 보인다.

### 목표
- `repository` context 하위에 `presentation` 패키지를 추가한다.
- repository 관련 inbound adapter, request dto, request mapper를 `server/repository/presentation/...` 하위로 묶는다.
- `rest`/`web` adapter를 분리 유지하되, 같은 context 코드를 같은 루트 하위에서 찾을 수 있게 한다.
- 공통 presentation 자산은 기존 top-level `presentation`에 유지한다.
- 이번 단계에서는 패키지 재정렬을 우선하고, overview 책임 분리는 후속 후보로 남긴다.

### 방법 조사 및 선택
- **방안 1**: 현재 top-level `presentation/api/rest`, `presentation/api/web` 평면 구조 유지
  - 변경 비용은 가장 낮다.
  - repository context 응집도가 코드 구조에 드러나지 않는다.
- **방안 2**: controller만 `server/repository/presentation/...`으로 이동
  - controller 응집은 개선된다.
  - 하지만 request dto/mapper가 top-level `presentation`에 남아 repository presentation 경계가 반쯤 끊긴다.
- **방안 3**: `server/repository/presentation/...` 하위에 controller, request dto, mapper를 함께 재배치
  - 이번 계획의 채택안이다.

### 선택 방안
- **채택안**: 방안 3
- controller는 repository context 하위의 `presentation` 패키지로 재배치한다.
- 단, `ApiResponse`, `GlobalExceptionHandler`, 공통 에러/유틸은 top-level `presentation`에 유지한다.
- repository 관련 presentation 자산은 아래처럼 재배치한다.

```text
server/src/main/java/io/jgitkins/server/repository/presentation/api/rest/
  BranchController.java
  RepositoryCommitController.java
  RepositoryContentController.java
  RepositoryFileController.java
  RepositoryManagementController.java
  RepositoryMemberController.java

server/src/main/java/io/jgitkins/server/repository/presentation/api/web/
  WebRepositoryController.java

server/src/main/java/io/jgitkins/server/repository/presentation/dto/
  BranchCreateRequest.java
  RepositoryCreateRequest.java
  RepositoryMemberAddRequest.java
  RepositoryUpdateRequest.java

server/src/main/java/io/jgitkins/server/repository/presentation/mapper/
  BranchRequestMapper.java
  RepositoryRequestMapper.java
```

### 범위
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryManagementController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/BranchController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryMemberController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryContentController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryCommitController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryFileController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java`
- `server/src/main/java/io/jgitkins/server/presentation/dto/RepositoryCreateRequest.java`
- `server/src/main/java/io/jgitkins/server/presentation/dto/RepositoryUpdateRequest.java`
- `server/src/main/java/io/jgitkins/server/presentation/dto/RepositoryMemberAddRequest.java`
- `server/src/main/java/io/jgitkins/server/presentation/dto/BranchCreateRequest.java`
- `server/src/main/java/io/jgitkins/server/presentation/mapper/RepositoryRequestMapper.java`
- `server/src/main/java/io/jgitkins/server/presentation/mapper/BranchRequestMapper.java`
- 관련 controller 테스트
- `server/src/main/java/io/jgitkins/server/application/port/in/RepositoryOverviewUseCase.java`
- `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`
  - 이번 단계의 직접 이동 대상은 아니지만, controller 책임 혼합 원인 분석 범위에는 포함한다.

### 핵심 판단
- `server/repository/presentation` 하위로 이동한다.
  - 현재 리팩토링 방향은 top-level 계층 루트보다 context-root 수직 패키지 정렬에 더 가깝다.
  - 이미 `repository/application`, `repository/domain`, `repository/infrastructure`가 존재하므로 `presentation`도 같은 루트 아래로 모으는 편이 더 일관적이다.
- controller만 이동하지 않는다.
  - request dto와 request mapper도 함께 이동해야 repository presentation 경계가 닫힌다.
- 공통 presentation 자산은 그대로 둔다.
  - `ApiResponse`, `GlobalExceptionHandler`, `presentation.common.error`, `presentation.util`은 여러 context가 공유하므로 top-level `presentation`에 유지한다.
- `RepositoryManagementController`의 overview endpoint는 이번 단계에서 유지한다.
  - 다만 이는 후속에 `RepositoryOverviewController` 분리 후보로 문서에 남긴다.
- `RepositoryOverviewUseCase/Service`는 이번 단계에서 이동하지 않는다.
  - presentation 재정렬 작업과 application/context 재정렬 작업을 분리해 범위를 제어한다.

### 목표 패키지 방향
```text
io.jgitkins.server.repository.presentation.api.rest
io.jgitkins.server.repository.presentation.api.web
io.jgitkins.server.repository.presentation.dto
io.jgitkins.server.repository.presentation.mapper
```

### 단계별 계획
1. repository 관련 rest/web controller, dto, mapper의 목표 패키지를 확정한다.
2. request dto와 request mapper를 먼저 `repository.presentation` 하위로 이동한다.
3. controller package 선언과 import를 새 위치 기준으로 수정한다.
4. 관련 controller 테스트의 package/import/주입 타입을 함께 정리한다.
5. architecture/package convention 및 스프링 component scan 영향이 없는지 검증한다.
6. overview 책임 분리 후보는 별도 후속 작업으로 남긴다.

### 점진 이관 순서
1. `BranchCreateRequest`, `RepositoryCreateRequest`, `RepositoryMemberAddRequest`, `RepositoryUpdateRequest`와 `BranchRequestMapper`, `RepositoryRequestMapper`를 먼저 이동한다.
2. `BranchController`, `RepositoryMemberController`처럼 책임이 선명한 controller부터 이동한다.
3. `RepositoryContentController`, `RepositoryCommitController`, `RepositoryFileController`, `WebRepositoryController`를 이동한다.
4. `RepositoryManagementController`를 마지막에 이동하고, overview endpoint 유지 여부를 재확인한다.
5. controller 테스트와 패키지 규칙 테스트를 정리한다.

### 검증 기준
- repository 관련 controller가 더 이상 top-level `presentation/api/rest`, `presentation/api/web` 루트에 남아 있지 않아야 한다.
- repository 전용 request dto와 request mapper가 더 이상 top-level `presentation/dto`, `presentation/mapper`에 남아 있지 않아야 한다.
- `rest`와 `web` controller는 계속 별도 패키지로 유지되어야 한다.
- `ApiResponse`, `GlobalExceptionHandler`, 공통 presentation error/util은 기존 top-level `presentation`에 남아 있어야 한다.
- `RepositoryManagementController`는 여전히 `RepositoryManagementUseCase`, `RepositoryLoadUseCase`, `RepositoryOverviewUseCase`를 주입받되, 위치만 repository context 하위로 정리되어야 한다.
- controller 테스트와 package convention 테스트가 통과해야 한다.

### 예시 코드

#### 1. RepositoryManagementController 목표 위치
```java
package io.jgitkins.server.repository.presentation.api.rest;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.repository.presentation.dto.RepositoryCreateRequest;
import io.jgitkins.server.repository.presentation.mapper.RepositoryRequestMapper;
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Management", description = "저장소 관리")
@RequestMapping("/api/repositories")
@Validated
public class RepositoryManagementController {

    private final RepositoryManagementUseCase repositoryManagementUseCase;
    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryOverviewUseCase repositoryOverviewUseCase;
    private final RepositoryRequestMapper repositoryRequestMapper;

    @Operation(summary = "Create Repository", description = "ownerType required.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResult>> create(@Valid @RequestBody RepositoryCreateRequest request) {
        RepositoryCreateCommand createCommand = repositoryRequestMapper.toCommand(request);
        RepositoryResult result = repositoryManagementUseCase.create(createCommand);
        return ApiResponse.created(result.id(), result);
    }

    @Operation(summary = "Get Repository Metadata")
    @GetMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<RepositoryResult>> getRepository(@PathVariable Long repositoryId) {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepository(repositoryId));
    }

    @Operation(summary = "Get Repositories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getRepositories() {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepositories());
    }

    @Operation(summary = "Get User Repositories by Username")
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getUserRepositories(
            @PathVariable("username") @NotBlank String username
    ) {
        return ApiResponse.ok(repositoryLoadUseCase.loadUserRepositories(username));
    }

    @Operation(summary = "Delete Repository")
    @DeleteMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(@PathVariable Long repositoryId) {
        repositoryManagementUseCase.deleteRepository(repositoryId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "Get Repository Overview")
    @GetMapping("/{repositoryId}/overview")
    public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getOverview(
            @PathVariable Long repositoryId,
            @RequestParam(name = "branch", required = false) String branch
    ) throws IOException {
        return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repositoryId, branch));
    }
}
```

#### 2. BranchController 목표 위치
```java
package io.jgitkins.server.repository.presentation.api.rest;

import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.repository.presentation.dto.BranchCreateRequest;
import io.jgitkins.server.repository.presentation.mapper.BranchRequestMapper;
import io.jgitkins.server.presentation.util.LocationUriBuilder;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repositories/{repositoryId}/branches")
@Tag(name = "Branch Management", description = "브랜치 조회/생성/삭제")
public class BranchController {

    private final BranchLoadUseCase branchLoadUseCase;
    private final BranchManagementUseCase branchManagementUseCase;
    private final BranchRequestMapper branchRequestMapper;

    @Operation(summary = "Create branch")
    @PostMapping
    public ResponseEntity<Void> create(@PathVariable Long repositoryId, @RequestBody BranchCreateRequest request) {
        BranchCreateCommand createCommand = branchRequestMapper.toCommand(repositoryId, request);
        branchManagementUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(@PathVariable Long repositoryId) {
        return ApiResponse.ok(branchLoadUseCase.loadBranches(repositoryId));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(
            @PathVariable Long repositoryId,
            @PathVariable String branchName
    ) {
        return ApiResponse.ok(branchLoadUseCase.loadBranch(repositoryId, branchName));
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable Long repositoryId,
            @PathVariable String branchName
    ) {
        branchManagementUseCase.deleteBranch(repositoryId, branchName);
        return ApiResponse.noContent();
    }
}
```

#### 3. RepositoryMemberController 목표 위치
```java
package io.jgitkins.server.repository.presentation.api.rest;

import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.repository.presentation.dto.RepositoryMemberAddRequest;
import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Members")
@RequestMapping("/api/repositories/{repositoryId}/members")
public class RepositoryMemberController {

    private final RepositoryMemberManagementUseCase repositoryMemberManagementUseCase;
    private final RepositoryMemberLoadUseCase repositoryMemberLoadUseCase;

    @Operation(summary = "Add repository member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Long repositoryId,
            @RequestBody RepositoryMemberAddRequest request
    ) {
        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(repositoryId, request.userId(), request.role());
        repositoryMemberManagementUseCase.addRepositoryMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove repository member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long repositoryId, @PathVariable Long userId) {
        repositoryMemberManagementUseCase.removeRepositoryMember(repositoryId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List repository members")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryMemberSummary>>> listMembers(@PathVariable Long repositoryId) {
        return ApiResponse.ok(repositoryMemberLoadUseCase.getRepositoryMembers(repositoryId));
    }
}
```

#### 4. WebRepositoryController 목표 위치
```java
package io.jgitkins.server.repository.presentation.api.web;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Web Repository")
@RequestMapping("/api/internal/repositories")
@Validated
public class WebRepositoryController {

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryOverviewUseCase repositoryOverviewUseCase;

    @Operation(summary = "Get User Repositories by Username (Web)")
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getUserRepositories(
            @PathVariable("username") @NotBlank String username
    ) {
        return ApiResponse.ok(repositoryLoadUseCase.loadUserRepositories(username));
    }

    @Operation(summary = "Get Repository Overview by Namespace/Repo (Web)")
    @GetMapping("/{namespace}/{repoName}/overview")
    public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getRepositoryOverviewByPath(
            @PathVariable String namespace,
            @PathVariable String repoName,
            @RequestParam(name = "branch", required = false) String branch
    ) throws java.io.IOException {
        RepositoryResult repository = repositoryLoadUseCase.loadRepositoryByPath(namespace, repoName);
        return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repository.id(), branch));
    }
}
```

#### 5. RepositoryRequestMapper 목표 위치
```java
package io.jgitkins.server.repository.presentation.mapper;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.presentation.dto.RepositoryCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class RepositoryRequestMapper {

    public RepositoryCreateCommand toCommand(RepositoryCreateRequest request) {
        return new RepositoryCreateCommand(
                request.name(),
                request.path(),
                request.ownerType(),
                request.ownerId(),
                request.visibility()
        );
    }
}
```

#### 6. RepositoryCreateRequest 목표 위치
```java
package io.jgitkins.server.repository.presentation.dto;

import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RepositoryCreateRequest(
        @NotBlank String name,
        @NotBlank String path,
        @NotNull OwnerType ownerType,
        @NotNull Long ownerId,
        @NotNull RepositoryVisibility visibility
) {
}
```

### 결론
- repository 관련 presentation 자산은 `server/repository/presentation/...`으로 모은다.
- controller만이 아니라 request dto와 request mapper도 함께 이동한다.
- 공통 presentation 자산은 top-level `presentation`에 유지한다.
- 이번 단계의 핵심은 repository context의 수직 패키지 정렬을 presentation 계층까지 닫는 것이다.
- overview 책임 분리는 후속 후보로 남기고, 이번 작업은 패키지 재정렬과 테스트 정리에 집중한다.
