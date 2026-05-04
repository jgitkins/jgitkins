# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.25 Repository Context inbound UseCase 포트 통합 계획서

### 배경
- `repository.application.port.in` 아래 포트가 메서드 단위로 너무 잘게 나뉘어 있다.
- 현재 구조는 `Create`, `Delete`, `Add`, `Remove`, `Query`마다 별도 인터페이스가 존재해 controller와 테스트의 주입점이 과도하게 많다.
- 반면 실제 구현체는 이미 `BranchManagementService`, `RepositoryManagementService`, `RepositoryMemberService`, `BranchLoadService`, `RepositoryLoadService`처럼 `관리`와 `조회` 축으로 응집돼 있다.

### 목표
- inbound port를 `Management`와 `Load` 기준으로 재구성한다.
- controller, web adapter, overview service가 더 적은 포트에 의존하도록 단순화한다.
- 서비스 구현체의 책임 축과 포트 인터페이스 축을 일치시킨다.

### 용어 정리
- `ManagementUseCase`
  - 생성, 삭제, 추가, 제거처럼 상태를 변경하는 command 계열 입력 포트
- `LoadUseCase`
  - 조회 전용 read 입력 포트
- `QueryUseCase`
  - 의미상 가능하지만, 현재 코드베이스의 `LoadService` 명명과 맞추기 위해 `LoadUseCase`를 우선 사용

### 방법 조사 및 선택
- **방안 1**: 현재 포트 유지
  - 변경 비용이 가장 낮다.
  - 포트 수가 과도하고 구현체 응집도와 어긋난다.
- **방안 2**: aggregate별 단일 포트로 전부 통합
  - 예: `RepositoryUseCase`, `BranchUseCase`, `RepositoryMemberUseCase`
  - read/write가 한 인터페이스에 섞여 경계가 흐려진다.
- **방안 3**: aggregate별 `ManagementUseCase` / `LoadUseCase` 2축으로 통합
  - 이번 계획의 채택안이다.

### 선택 방안
- **채택안**: 방안 3
- `BranchCreateUseCase`, `BranchDeleteUseCase` -> `BranchManagementUseCase`
- `BranchLoadUseCase` -> 유지
- `RepositoryCreateUseCase`, `RepositoryDeleteUseCase` -> `RepositoryManagementUseCase`
- `RepositoryLoadUseCase` -> 유지
- `RepositoryMemberAddUseCase`, `RepositoryMemberRemoveUseCase` -> `RepositoryMemberManagementUseCase`
- `RepositoryMemberQueryUseCase` -> `RepositoryMemberLoadUseCase`

### 범위
- `server/src/main/java/io/jgitkins/server/repository/application/port/in/*`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchLoadService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryLoadService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryMemberService.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/BranchController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryManagementController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryMemberController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryContentController.java`
- `server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java`
- `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`
- 관련 controller/service 테스트와 package convention 정리

### 핵심 판단
- `Branch`
  - 생성/삭제는 모두 `BranchManagementService`에 모여 있으므로 하나의 `BranchManagementUseCase`가 자연스럽다.
  - 조회는 별도 `BranchLoadService`가 있으므로 `BranchLoadUseCase` 유지가 맞다.
- `Repository`
  - 생성/삭제는 모두 `RepositoryManagementService`가 담당하므로 `RepositoryManagementUseCase`가 적절하다.
  - 조회는 이미 `RepositoryLoadService` 기준으로 안정돼 있으므로 `RepositoryLoadUseCase` 유지가 맞다.
- `RepositoryMember`
  - 추가/삭제는 상태 변경 command이므로 `RepositoryMemberManagementUseCase`로 묶는 것이 자연스럽다.
  - 멤버 목록 조회는 read-only이므로 `RepositoryMemberLoadUseCase`로 분리한다.

### 목표 패키지 방향
- `repository.application.port.in`
  - `BranchManagementUseCase`
  - `BranchLoadUseCase`
  - `RepositoryManagementUseCase`
  - `RepositoryLoadUseCase`
  - `RepositoryMemberManagementUseCase`
  - `RepositoryMemberLoadUseCase`

### 단계별 계획
1. 신규 통합 포트 인터페이스를 추가한다.
2. 기존 service 구현체가 새 통합 포트를 구현하도록 변경한다.
3. controller, web adapter, overview service의 주입 타입을 새 포트로 교체한다.
4. 기존 세분화 포트를 제거한다.
5. 테스트와 package convention을 새 기준으로 정리한다.

### 점진 이관 순서
1. 새 통합 포트 추가
2. 구현체 다중 구현 전환
3. 호출부 주입 타입 교체
4. 구 포트 제거
5. 테스트/아키텍처 규칙 검증

### 검증 기준
- `BranchController`는 `BranchManagementUseCase`와 `BranchLoadUseCase`만 주입받아야 한다.
- `RepositoryManagementController`는 `RepositoryManagementUseCase`와 `RepositoryLoadUseCase`만 주입받아야 한다.
- `RepositoryMemberController`는 `RepositoryMemberManagementUseCase`와 `RepositoryMemberLoadUseCase`만 주입받아야 한다.
- 기존 `LoadService` / `ManagementService` 구조보다 더 복잡해지지 않아야 한다.
- 관련 테스트와 package convention 테스트가 유지되어야 한다.

### 예시 코드

#### 1. Branch 포트 통합 예시
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;

public interface BranchManagementUseCase {
    void createBranch(BranchCreateCommand command);
    void deleteBranch(Long repositoryId, String branchName);
}
```

```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import java.util.List;

public interface BranchLoadUseCase {
    List<BranchSearchResult> loadBranches(Long repositoryId);
    BranchSearchResult loadBranch(Long repositoryId, String branchName);
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchManagementUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchFactory branchFactory;
    private final BranchGitPort branchGitPort;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        BranchRepositoryContext context = loadWriteContext(command.repositoryId());
        branchFactory.create(command, context.namespace(), context.repository());
    }

    @Override
    @Transactional
    public void deleteBranch(Long repositoryId, String branchName) {
        BranchRepositoryContext context = loadWriteContext(repositoryId);
        Branch branch = loadExistingBranch(repositoryId, branchName);

        branch.delete();
        branchRepository.delete(branch);
        branchGitPort.deleteBranch(
                context.namespace(),
                context.repository().getName().getValue(),
                branchName
        );
    }

    private BranchRepositoryContext loadWriteContext(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchRepositoryContext(repository, namespace);
    }

    private Branch loadExistingBranch(Long repositoryId, String branchName) {
        return branchRepository.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new BranchNotFoundException(branchName));
    }

    private record BranchRepositoryContext(Repository repository, String namespace) {
    }
}
```

`BranchRepositoryContext`는 `BranchCreationContext`로 rename 하지 않는다. `BranchCreationContext`는 이미 factory와 git port 사이를 넘는 계약 클래스이고, `BranchRepositoryContext`는 `BranchManagementService` 내부에서 repository/namespace를 잠깐 묶는 로컬 조합값이기 때문이다. 이름을 같게 맞추면 오히려 두 값의 책임이 섞인다. 또한 `loadWriteContext`, `loadExistingBranch`는 현재 `BranchManagementService` 내부 orchestration 보조 메서드로만 쓰이므로 이번 작업에서는 별도 collaborator로 분리하지 않는다.

```java
package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.presentation.mapper.BranchRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.BranchCreateRequest;
import io.jgitkins.server.presentation.util.LocationUriBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
    public ResponseEntity<Void> create(@PathVariable Long repositoryId,
                                       @RequestBody BranchCreateRequest request) {

        BranchCreateCommand createCommand = branchRequestMapper.toCommand(repositoryId, request);
        branchManagementUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long repositoryId,
                                                          @PathVariable String branchName) {
        branchManagementUseCase.deleteBranch(repositoryId, branchName);
        return ApiResponse.noContent();
    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(@PathVariable Long repositoryId) {
        return ApiResponse.ok(branchLoadUseCase.loadBranches(repositoryId));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(@PathVariable Long repositoryId,
                                                                     @PathVariable String branchName) {
        return ApiResponse.ok(branchLoadUseCase.loadBranch(repositoryId, branchName));
    }
}
```

#### 2. Repository 포트 통합 예시
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

public interface RepositoryManagementUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
    void deleteRepository(Long repositoryId);
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryManagementUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryOwnershipPolicy repositoryOwnershipPolicy;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        RepositoryCreationPlan creationPlan = repositoryOwnershipPolicy.prepareCreation(command);
        Repository saved = repositoryPort.save(creationPlan.repository());
        Repository provisioned = repositoryProvisioner.provision(saved, creationPlan.initialCommitOptions());
        return repositoryApplicationMapper.toDto(provisioned);
    }

    @Override
    @Transactional
    public void deleteRepository(Long repositoryId) {
        RepositoryId id = RepositoryId.of(repositoryId);
        Repository repository = repositoryPort.findById(id)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        repositoryOwnershipPolicy.validateDeletion(repository);
        repositoryProvisioner.delete(repository);
        repositoryPort.deleteById(id);
    }
}
```

#### 3. RepositoryMember 포트 통합 예시
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;

public interface RepositoryMemberManagementUseCase {
    void addRepositoryMember(RepositoryMemberAddCommand command);
    void removeRepositoryMember(Long repositoryId, Long userId);
}
```

```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import java.util.List;

public interface RepositoryMemberLoadUseCase {
    List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId);
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.application.validate.RepositoryMemberValidator;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryMemberService implements RepositoryMemberManagementUseCase,
                                                RepositoryMemberLoadUseCase {

    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final RepositoryMemberValidator repositoryMemberValidator;
    private final RepositoryMembershipFactory repositoryMembershipFactory;

    @Override
    @Transactional
    public void addRepositoryMember(RepositoryMemberAddCommand command) {
        repositoryMemberValidator.validateAddCommand(command);

        RepositoryMember member = repositoryMembershipFactory.createMember(command);
        if (repositoryMemberValidator.isAlreadyMember(member.getRepositoryId(), member.getUserId())) {
            return;
        }
        repositoryMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeRepositoryMember(Long repositoryId, Long userId) {
        repositoryMemberValidator.validateMemberIdentifiers(repositoryId, userId);
        repositoryMemberPort.deleteByRepositoryIdAndUserId(
                RepositoryId.of(repositoryId),
                io.jgitkins.server.domain.model.vo.UserId.of(userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId) {
        repositoryMemberValidator.validateRepositoryId(repositoryId);
        return repositoryMemberPort.findAllByRepositoryId(RepositoryId.of(repositoryId))
                .stream()
                .map(member -> new RepositoryMemberSummary(
                        member.getUserId().getValue(),
                        member.getRole(),
                        member.getAddedAt()
                ))
                .toList();
    }
}
```

### 결론
- `UpdateUseCase`보다 `ManagementUseCase`가 더 정확하다.
- 현재 repository context는 메서드 단위 포트보다 `관리 / 조회` 2축 포트가 더 자연스럽다.
- 다음 구현은 `통합 포트 추가 -> 구현체 전환 -> controller 주입 정리 -> 구 포트 제거 -> 테스트 정리` 순서로 진행한다.
