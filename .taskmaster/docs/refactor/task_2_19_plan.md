# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.19 Repository Context 기준 리팩토링 계획서
- **세부 계획 문서**: `./task_2_19_contract_exception_plan.md`

### 배경 (왜?)
- 현재 Repository 관련 책임이 `application.service`, `application.support`, `domain.aggregate`, `domain.model`, `domain.repository`에 분산되어 있다.
- `RepositoryManagementService`, `RepositoryLoadService`, `RepositoryProvisioner`, `BranchManagementService`, `RepositoryMemberService`가 모두 Repository Context에 속하지만 패키지 구조상 경계가 드러나지 않는다.
- Repository 메타데이터, Branch 메타데이터, Repository Member 관계 모델, Git 외부 상태 반영이 한 흐름 안에서 섞여 있어 후속 리팩토링 단위가 커진다.

### 목표 (Goals)
- `Repository Aggregate`, `Branch Entity Candidate`, `RepositoryMember relation model` 기준으로 패키지 재배치 기준을 고정한다.
- provisioning, branch metadata, membership, Git external state 경계를 분리한다.
- 선행 Task 2.18에서 정리한 `shared` seam과 `repository` seam을 Repository Context 계획에 반영한다.
- 점진 이관 순서와 테스트 기준을 정의한다.

### 용어 정리
- `Repository`는 Aggregate Root다.
- `Branch`는 Repository 내부 상태와 연결되는 메타데이터 Entity 후보로 본다.
- `RepositoryMember`는 Aggregate 내부 Entity보다 관계 모델로 본다.
- Git branch head, commit, tree는 Repository Context가 영속 소유하지 않는 외부 상태다.

### 범위 (Scope)
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryManagementService.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryLoadService.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/service/BranchManagementService.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/service/BranchLoadService.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryMemberService.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/support/provisioning/RepositoryProvisioner.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/RepositoryCreateUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/RepositoryDeleteUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/RepositoryLoadUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/BranchCreateUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/BranchDeleteUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/in/BranchLoadUseCase.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryPersistencePort.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryMemberPersistencePort.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/RepositoryGitPort.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/BranchGitPort.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/BranchQueryPort.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/application/port/out/CommitGitPort.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/command/RepositoryCreateCommand.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/command/BranchCreateCommand.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/command/BranchCreationContext.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/command/RepositoryMemberAddCommand.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/result/RepositoryResult.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/result/BranchSearchResult.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/result/RepositoryMemberSummary.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/CommitFile.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/dto/CommitHistory.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/RepositoryNotFoundException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/BranchNotFoundException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/BranchAlreadyExistsException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/SourceBranchNotFoundException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/UserNotFoundException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/application/exception/CommitNotFoundException.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/domain/aggregate/Repository.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/domain/model/RepositoryMember.java`
- **수정 대상 문서 계획 범위**: `server/src/main/java/io/jgitkins/server/domain/repository/BranchRepository.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/RepositoryPersistenceAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/RepositoryMemberPersistenceAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/repository/BranchRepositoryAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/query/BranchQueryAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git/RepositoryGitAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git/RepositoryGitCommitAdapter.java`
- **현재 수정 대상 범위**: `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git/BranchGitAdapter.java`
- **참조 대상**: `docs/modeling/contexts/repository/repository-context.md`
- **참조 대상**: `docs/modeling/contexts/repository/repository-context-diagrams.md`
- **수정 제외 대상**: Pull Request 상태 전이, mergeability 계산, CI policy 해석, runner dispatch는 이번 계획 범위에서 제외한다.

### 방법 조사 및 선택
- **선택 방안**: 방안 2를 선택한다.
  `repository` 최상위 패키지 아래에 application/domain seam을 점진적으로 재배치한다.
  문서와 코드 구조가 직접 연결되고, 후속 context 분리와도 정합성이 맞는다.

### 목표 패키지 방향
- `repository.application.service`
- `repository.application.port.in`
- `repository.application.port.out`
- `repository.application.support.provisioning`
- `repository.application.support.branch`
- `repository.application.support.membership`
- `repository.application.contract.command`
- `repository.application.contract.result`
- `repository.infrastructure.adapter.persistence`
- `repository.infrastructure.adapter.git`
- `repository.domain.aggregate`
- `repository.domain.model`
- `repository.domain.vo`
- `repository.domain.relation`

### 변경 목록
- `RepositoryManagementService`는 Repository 생성/삭제 오케스트레이션 전용으로 본다.
- `RepositoryLoadService`는 Repository 조회와 visibility filtering 전용으로 본다.
- `RepositoryProvisioner`는 provisioning support로 분리한다.
- `BranchManagementService`는 Repository Context 내부 branch metadata write orchestration으로 재배치한다.
- `BranchLoadService`는 Repository Context 내부 branch metadata read orchestration으로 재배치한다.
- `RepositoryMemberService`는 relation model 관리 서비스로 재배치한다.
- `RepositoryCreateUseCase`, `RepositoryDeleteUseCase`, `RepositoryLoadUseCase`, `RepositoryMember*UseCase`는 repository application port.in으로 함께 재배치한다.
- `BranchCreateUseCase`, `BranchDeleteUseCase`, `BranchLoadUseCase`는 Branch가 Repository 하위 개념이므로 repository application port.in으로 함께 재배치한다.
- `RepositoryPersistencePort`, `RepositoryMemberPersistencePort`, `RepositoryGitPort`, `BranchGitPort`, `BranchQueryPort`, `CommitGitPort`는 repository application port.out으로 함께 재배치한다.
- `RepositoryPersistenceAdapter`, `RepositoryMemberPersistenceAdapter`, `RepositoryGitAdapter`, `RepositoryGitCommitAdapter`, `BranchRepositoryAdapter`, `BranchQueryAdapter`, `BranchGitAdapter`는 repository infrastructure adapter로 함께 재배치한다.
- `BranchRepository`는 이번 단계에서 application port로 올리지 않고, 다른 domain CRUD 재정렬 시점까지 domain repository 위치를 유지한다.
- `Repository`는 장기적으로 `initialized` boolean 대신 `RepositoryState`로 승격하는 계획을 포함한다.
- `RepositoryMember`는 Aggregate 내부 entity가 아니라 관계 모델이라는 판단을 코드 구조에 반영한다.
- Git 반영 포트는 Repository Context의 outbound adapter 경계로만 사용한다.
- `RepositoryOwnershipPolicy`는 owner 해석, draft aggregate 생성, 삭제 권한 검증을 담당하는 application support로 둔다.
- `BranchWritePolicy`는 source branch 결정과 branch metadata 생성 규칙만 담당한다.
- `RepositoryMembershipPolicy`는 membership add/remove/query 입력 검증과 기본 role 결정 규칙만 담당한다.
- `InitialCommitOptions`는 `Repository` aggregate 소유값이 아니라 provisioning 절차 입력값으로만 사용한다.
- `RepositoryCreateCommand`, `BranchCreateCommand`, `RepositoryMemberAddCommand`, `RepositoryResult`, `BranchSearchResult`, `RepositoryMemberSummary`는 현재 Repository Context 전용 계약이므로 장기적으로 `repository.application.contract` 하위 이동 후보로 본다.
- `CommitFile`, `CommitHistory`, `BranchCreationContext`는 Git adapter/provisioning 흐름에서만 쓰이므로 장기적으로 `repository.application.contract` 또는 `repository.infrastructure.git.contract`로 축소 이관한다.
- `RepositoryNotFoundException`, `BranchNotFoundException`, `BranchAlreadyExistsException`, `SourceBranchNotFoundException`, `CommitNotFoundException`는 Repository Context 전용 예외 후보로 본다.
- `UserNotFoundException`은 User Context/공용 조회 계약과 연결되므로 이번 단계에서는 `application.exception`에 유지한다.
- 이번 단계는 서비스/포트/어댑터 패키지 정리를 우선하고, DTO/Exception은 호출 계층 영향이 큰 만큼 “위치 결정 문서화 + 후속 점진 이관”으로 처리한다.

### 단계별 계획 (Plan)
- **단계 1**: Repository Context 내부 개념을 `aggregate / entity candidate / relation model / external state`로 다시 분류한다.
- **단계 2**: 현재 application service와 support를 `management / load / provisioning / branch / membership` 축으로 재배치한다.
- **단계 3**: `RepositoryProvisioner`와 branch 생성 흐름의 Git 포트 의존을 외부 상태 반영 경계로 문서화한다.
- **단계 4**: `Repository`, `Branch`, `RepositoryMember`의 목표 패키지와 점진 이관 순서를 확정한다.
- **단계 5**: Repository Context 전용 DTO/Exception과 공용 DTO/Exception을 분류하고 목표 위치를 고정한다.
- **단계 6**: 관련 테스트를 provisioning, load, management, branch, membership 단위로 재배열할 기준을 정의한다.

### 점진 이관 순서
1. `RepositoryProvisioner`를 Repository Context support로 이동한다.
2. `RepositoryManagementService`, `RepositoryLoadService`를 Repository Context application service로 이동한다.
3. `BranchManagementService`, `BranchLoadService`, `BranchCreateUseCase`, `BranchDeleteUseCase`, `BranchLoadUseCase`, `BranchGitPort`를 Repository Context branch 영역으로 이동한다.
4. `RepositoryMemberService`와 `RepositoryMember`를 relation model 기준 구조로 이동한다.
5. persistence/git adapter를 Repository Context infrastructure adapter로 이동한다.
6. Repository Context 전용 DTO/Exception은 `repository.application.contract` 및 `repository.application.exception` 후보로 분류하고, 공용 계약은 `application`에 유지한다.
7. 마지막으로 `Repository`, `Branch`, `RepositoryMember`와 관련 domain package를 Repository Context 기준으로 재정렬한다.

### 검증 기준
- Repository 생성/삭제 흐름은 기존과 동일해야 한다.
- load 계열 서비스는 상태 변경 포트를 직접 수행하지 않아야 한다.
- provisioning 단계는 bare repository 생성, default branch row 생성, initial content 반영 경계를 유지해야 한다.
- branch write 흐름은 initialized 상태 검증과 권한 검증을 유지해야 한다.
- membership 흐름은 `repositoryId + userId` 중복 방지 규칙을 유지해야 한다.
- DTO/Exception 분류 후에도 presentation, application, repository 계층 간 import 순환이 생기지 않아야 한다.

### 개선 사항 점검
- **개선안 1**: `RepositoryState` enum 도입을 후속 구현 단위로 분리한다.
- **개선안 2**: branch metadata와 Git branch head를 같은 모델로 취급하지 않도록 테스트 이름과 문서 용어를 정리한다.
- **개선안 3**: `RepositoryMemberService`와 `OrganizeMember` 계열을 장기적으로 공통 membership abstraction 후보로 남긴다.
- **선택 개선안**: 개선안 1, 2를 우선 반영한다.

### 기대효과 (Expected Benefits)
- Repository Context의 구조가 문서와 코드 양쪽에서 일치하게 된다.
- provisioning, branch, membership 경계가 선명해져 후속 구현 단위가 작아진다.
- Git 외부 상태와 Repository 영속 상태를 구분하기 쉬워진다.

### 예시 (방안 2 기준)

#### AS-IS 패키지 분산 구조
```text
server/application/service/RepositoryManagementService.java
server/application/service/RepositoryLoadService.java
server/application/service/BranchManagementService.java
server/application/service/RepositoryMemberService.java
server/application/support/RepositoryProvisioner.java
server/domain/aggregate/Repository.java
server/domain/model/RepositoryMember.java
server/domain/repository/BranchRepository.java
server/shared/application/support/RepositoryNamespaceResolver.java
server/shared/application/support/RepositoryAccessibilityService.java
```

```java
package io.jgitkins.server.application.service;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryCreateUseCase, RepositoryDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryGitPort repositoryGitPort;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryValidator repositoryValidator;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        Repository repository = createRepository(command);
        validateRepositoryCreation(repository, command.organizeId());

        Repository saved = repositoryPort.save(repository);
        Repository provisioned = repositoryProvisioner.provision(saved, createInitialCommitOptions(command));
        return repositoryApplicationMapper.toDto(provisioned);
    }
}
```

```java
package io.jgitkins.server.application.support;

@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchRepository branchPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    public Repository provision(Repository repository, InitialCommitOptions initialCommitOptions) {
        initializeGitRepository(repository);
        createDefaultBranch(repository);
        return initializeContentIfNeeded(repository, initialCommitOptions);
    }
}
```

#### TO-BE 패키지 구조
```text
server/src/main/java/io/jgitkins/server/repository/
  application/
    port/
      in/
        RepositoryCreateUseCase.java
        RepositoryDeleteUseCase.java
        RepositoryLoadUseCase.java
        BranchCreateUseCase.java
        BranchDeleteUseCase.java
        BranchLoadUseCase.java
        RepositoryMemberAddUseCase.java
        RepositoryMemberRemoveUseCase.java
        RepositoryMemberQueryUseCase.java
      out/
        RepositoryPersistencePort.java
        RepositoryMemberPersistencePort.java
        RepositoryGitPort.java
        BranchGitPort.java
        CommitGitPort.java
    service/
      RepositoryManagementService.java
      RepositoryLoadService.java
      BranchManagementService.java
      BranchLoadService.java
      RepositoryMemberService.java
    contract/
      command/
        RepositoryCreateCommand.java
        BranchCreateCommand.java
        BranchCreationContext.java
        RepositoryMemberAddCommand.java
      result/
        RepositoryResult.java
        BranchSearchResult.java
        RepositoryMemberSummary.java
    support/
      provisioning/
        RepositoryProvisioner.java
      branch/
        BranchWritePolicy.java
      ownership/
        RepositoryOwnershipPolicy.java
      membership/
        RepositoryMembershipPolicy.java
    exception/
      RepositoryNotFoundException.java
      BranchNotFoundException.java
      BranchAlreadyExistsException.java
      SourceBranchNotFoundException.java
  domain/
    aggregate/
      Repository.java
    model/
      Branch.java
    repository/
      BranchRepository.java
    relation/
      RepositoryMember.java
    vo/
      RepositoryState.java
  infrastructure/
    adapter/
      persistence/
        RepositoryPersistenceAdapter.java
        RepositoryMemberPersistenceAdapter.java
        BranchRepositoryAdapter.java
      git/
        RepositoryGitAdapter.java
        RepositoryGitCommitAdapter.java
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.repository.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryDeleteUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;

/*
 * 현재 구현은 기존 presentation/controller 계약 영향도를 줄이기 위해
 * command/result/exception을 application 패키지에 유지한다.
 * 다만 Repository Context 전용 타입은 후속 단계에서
 * repository.application.contract / repository.application.exception 으로 이동한다.
 */
import io.jgitkins.server.repository.domain.aggregate.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryCreateUseCase, RepositoryDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryOwnershipPolicy repositoryOwnershipPolicy;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        Repository draft = repositoryOwnershipPolicy.createDraft(command);
        Repository saved = repositoryPort.save(draft);
        InitialCommitOptions initialCommitOptions = repositoryOwnershipPolicy.resolveInitialCommitOptions(command);

        Repository provisioned = repositoryProvisioner.provision(
                saved,
                repositoryNamespaceResolver.resolve(saved),
                initialCommitOptions
        );

        return repositoryApplicationMapper.toDto(provisioned);
    }

    @Override
    @Transactional
    public void deleteRepository(Long repositoryId) {
        Repository repository = repositoryOwnershipPolicy.loadManagedRepository(repositoryId);
        repositoryOwnershipPolicy.validateDeletion(repository);
        repositoryProvisioner.delete(repository, repositoryNamespaceResolver.resolve(repository));
        repositoryPort.deleteById(repository.getId());
    }
}
```

```java
package io.jgitkins.server.repository.application.support.ownership;

import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryOwnershipPolicy {

    private final CurrentUserPort currentUserPort;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;

    public Repository createDraft(RepositoryCreateCommand command) {
        OwnerType ownerType = command.ownerType();
        OwnerId ownerId = resolveOwnerId(ownerType, command.organizeId());
        String namespace = repositoryNamespaceResolver.resolve(ownerType, ownerId);

        return Repository.create(
                ownerType,
                ownerId,
                RepositoryName.from(command.repoName()),
                RepositoryPath.from(command.repoName()),
                BranchName.of(command.mainBranch()),
                command.visibility() != null ? command.visibility() : RepositoryVisibility.PRIVATE,
                command.description(),
                RepositoryPathHelper.buildClonePath(namespace, command.repoName()),
                command.credentialId(),
                null
        );
    }

    public Repository loadManagedRepository(Long repositoryId) {
        return repositoryPort.findById(io.jgitkins.server.domain.model.vo.RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    }

    public void validateDeletion(Repository repository) {
        Long currentUserId = currentUserPort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.UNAUTHENTICATED,
                        "current user required"));
        if (repository.getOwnerType() == OwnerType.USER
                && !repository.getOwnerId().getValue().equals(currentUserId)) {
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "repository delete permission denied");
        }
    }

    public InitialCommitOptions resolveInitialCommitOptions(RepositoryCreateCommand command) {
        return InitialCommitOptions.of(
                command.readme(),
                command.message(),
                command.authorName(),
                command.authorEmail()
        );
    }

    private OwnerId resolveOwnerId(OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return OwnerId.of(organizeId);
        }
        Long currentUserId = currentUserPort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.UNAUTHENTICATED,
                        "current user required"));
        return OwnerId.of(currentUserId);
    }
}
```

```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryResult;
import java.util.List;

public interface RepositoryLoadUseCase {

    RepositoryResult loadRepository(Long repositoryId);

    RepositoryResult loadRepositoryByPath(String namespace, String repoName);

    List<RepositoryResult> loadRepositories();

    List<RepositoryResult> loadUserRepositories(String username);
}
```

```java
package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import java.util.List;
import java.util.Optional;

public interface RepositoryPersistencePort {

    Repository save(Repository repository);

    Repository update(Repository repository);

    Optional<Repository> findById(RepositoryId repositoryId);

    Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name);

    List<Repository> findAll();

    List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId);

    void deleteById(RepositoryId repositoryId);
}
```

```java
package io.jgitkins.server.repository.application.support.provisioning;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.common.factory.CommitFileFactory;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchRepository branchRepository;
    private final RepositoryGitPort repositoryGitPort;
    private final CommitGitPort commitGitPort;
    private final BranchGitPort branchGitPort;

    public Repository provision(
            Repository repository,
            String namespace,
            InitialCommitOptions initialCommitOptions) {
        initializeBareRepository(namespace, repository.getName().getValue());
        createDefaultBranchMetadata(repository);

        if (initialCommitOptions == null || !initialCommitOptions.requiresInitialContent()) {
            return repository;
        }

        createInitialCommit(
                namespace,
                repository.getName().getValue(),
                repository.getDefaultBranch().getValue(),
                initialCommitOptions
        );
        updateHeadReference(namespace, repository.getName().getValue(), repository.getDefaultBranch().getValue());

        return repositoryPort.update(repository.markInit(LocalDateTime.now()));
    }

    public void delete(Repository repository, String namespace) {
        repositoryGitPort.deleteRepository(namespace, repository.getName().getValue());
    }

    private void initializeBareRepository(String namespace, String repositoryName) {
        repositoryGitPort.initialize(namespace, repositoryName);
    }

    private void createDefaultBranchMetadata(Repository repository) {
        Branch defaultBranch = Branch.create(
                repository.getId().getValue(),
                repository.getDefaultBranch().getValue(),
                false,
                true,
                true
        );
        branchRepository.save(defaultBranch);
    }

    private void createInitialCommit(
            String namespace,
            String repositoryName,
            String defaultBranch,
            InitialCommitOptions initialCommitOptions) {
        List<CommitFile> files = commitFileFactory.prepareInitialFile(repositoryName);
        commitGitPort.commit(
                namespace,
                repositoryName,
                defaultBranch,
                initialCommitOptions.commitMessage(),
                initialCommitOptions.authorName(),
                initialCommitOptions.authorEmail(),
                files
        );
    }

    private void updateHeadReference(String namespace, String repositoryName, String defaultBranch) {
        repositoryGitPort.updateHeadReference(namespace, repositoryName, defaultBranch);
    }
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.dto.command.BranchCreationContext;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.repository.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.repository.application.port.in.BranchDeleteUseCase;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.branch.BranchWritePolicy;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchWritePolicy branchWritePolicy;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final BranchGitPort branchGitPort;
    private final BranchRepository branchRepository;
    private final RepositoryPersistencePort repositoryPort;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        Repository repository = repositoryPort.findById(RepositoryId.of(command.repositoryId()))
                .orElseThrow();

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());

        String sourceBranch = branchWritePolicy.resolveSourceBranch(command, repository);
        Branch branch = branchWritePolicy.createMetadata(command, repository);
        branchRepository.save(branch);

        branchGitPort.createBranch(BranchCreationContext.of(command, namespace, repository, sourceBranch));
    }
}
```

```java
package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.Branch;
import org.springframework.stereotype.Component;

@Component
public class BranchWritePolicy {

    public String resolveSourceBranch(BranchCreateCommand command, Repository repository) {
        if (command.sourceBranch() != null && !command.sourceBranch().isBlank()) {
            return command.sourceBranch();
        }
        return repository.getDefaultBranch().getValue();
    }

    public Branch createMetadata(BranchCreateCommand command, Repository repository) {
        return Branch.create(
                repository.getId().getValue(),
                command.branchName(),
                false,
                true,
                false
        );
    }
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberRemoveUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipPolicy;
import io.jgitkins.server.repository.domain.relation.RepositoryMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryMemberService implements RepositoryMemberAddUseCase,
        RepositoryMemberRemoveUseCase,
        RepositoryMemberQueryUseCase {

    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final RepositoryMembershipPolicy repositoryMembershipPolicy;

    @Override
    @Transactional
    public void addRepositoryMember(RepositoryMemberAddCommand command) {
        repositoryMembershipPolicy.validateAdd(command);

        RepositoryId repositoryId = RepositoryId.of(command.repositoryId());
        UserId userId = UserId.of(command.userId());
        if (repositoryMembershipPolicy.isAlreadyMember(repositoryId, userId)) {
            return;
        }

        RepositoryMemberRole role = repositoryMembershipPolicy.resolveRole(command.role());
        RepositoryMember member = RepositoryMember.create(repositoryId, userId, role, null);
        repositoryMemberPort.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId) {
        repositoryMembershipPolicy.validateRepositoryId(repositoryId);
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

```java
package io.jgitkins.server.repository.application.support.membership;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMembershipPolicy {

    private final RepositoryMemberPersistencePort repositoryMemberPort;

    public void validateAdd(RepositoryMemberAddCommand command) {
        if (command.repositoryId() == null || command.userId() == null) {
            throw new IllegalArgumentException("repositoryId and userId must not be null");
        }
    }

    public void validateRepositoryId(Long repositoryId) {
        if (repositoryId == null || repositoryId <= 0) {
            throw new IllegalArgumentException("repositoryId must be positive");
        }
    }

    public boolean isAlreadyMember(RepositoryId repositoryId, UserId userId) {
        return repositoryMemberPort.existsByRepositoryIdAndUserId(repositoryId, userId);
    }

    public RepositoryMemberRole resolveRole(RepositoryMemberRole role) {
        return role != null ? role : RepositoryMemberRole.READER;
    }
}
```

```java
package io.jgitkins.server.repository.infrastructure.adapter.persistence;

import io.jgitkins.server.infrastructure.mapper.RepositoryDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.RepositoryEntityMbgMapper;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryPersistenceAdapter implements RepositoryPersistencePort {

    private final RepositoryEntityMbgMapper mapper;
    private final RepositoryDomainMapper domainMapper;

    @Override
    public Optional<Repository> findById(RepositoryId repositoryId) {
        return Optional.ofNullable(mapper.selectByPrimaryKey(repositoryId.getValue()))
                .map(domainMapper::toDomain);
    }

    @Override
    public Repository save(Repository repository) {
        var entity = domainMapper.toEntity(repository);
        mapper.insert(entity);
        return domainMapper.toDomain(entity);
    }
}
```

### 주의사항
- **포맷팅 금지**: 계획 단계에서 포맷팅은 수행하지 않는다.
- **기존 기능 보장**: API URL, DTO 계약, 인증 정책은 유지한다.
- **계획우선**: 이 문서는 구현 전 기준선 고정 목적이다.
- **예시전체나열**: management, provisioning, branch, membership의 before/after 예시를 모두 남긴다.

### 결론
- `Repository Context`는 service만 옮기는 작업이 아니라 `port.in`, `port.out`, persistence/git adapter, domain package까지 함께 정렬해야 의미가 유지된다.
- 우선순위는 provisioning, management/load, branch, membership, adapter, domain package 순서로 점진 이관한다.
- shared seam은 유지하고, Repository 고유 책임만 `repository` 최상위 패키지 아래로 이동하는 방향을 채택한다.
- `InitialCommitOptions`는 새 request model로 감싸지 않고 기존 타입을 유지한다.
- `InitialCommitOptions`는 aggregate 생성 입력이 아니라 provisioning 절차 입력으로만 사용한다.
- `OwnerId`, `OwnerType`, `RepositoryId`, `UserId`처럼 다른 context와 공유되는 식별/기초 VO는 우선 shared 성격으로 유지하고, `RepositoryState`, `RepositoryMemberRole`처럼 Repository Context 전용 의미가 강한 값부터 우선 정리한다.
