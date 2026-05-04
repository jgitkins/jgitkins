# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.24 Repository Support collaborator 책임 분해 계획서

### 배경
- `repository.application.support` 아래에 추가된 `RepositoryOwnershipPolicy`, `BranchWritePolicy`, `RepositoryMembershipPolicy`는 서비스 비대화를 막는 1차 분해로는 유효했다.
- 하지만 현재는 이름과 실제 책임이 어긋난다.
- 특히 membership 쪽은 validator wrapper 성격이 강하고, branch 쪽은 생성 절차가 service 안에 직접 남아 있다.

### 목표
- 불필요한 중간 collaborator는 제거한다.
- 실제 생성 흐름이 있는 지점만 `Factory`로 올린다.
- 검증은 기존 validator가 직접 담당하고, service는 orchestration만 남긴다.
- collaborator와 port 사이를 넘는 조합값은 `repository.application.contract` 하위의 명시적 계약 클래스로 승격한다.

### 용어 정리
- `Policy`
  - 허용 여부, 삭제 가능 여부, 선택 기준 같은 규칙 판단 책임을 가진다.
- `Factory`
  - 입력을 받아 객체를 생성하거나, application 생성 절차를 묶어 수행한다.
- `Validator`
  - 식별자 검증, 중복 검증, 입력 제약 확인을 담당한다.

### 방법 조사 및 선택
- **방안 1**: 현재 구조 유지
  - 변경 비용은 낮다.
  - 이름과 책임 불일치가 그대로 남는다.
- **방안 2**: ownership, branch, membership를 모두 policy/factory로 세분화
  - 구조는 정교하다.
  - 현재 코드 기준으로는 wrapper와 indirection이 늘어날 가능성이 크다.
- **방안 3**: 실제 생성 흐름이 있는 지점만 factory로 분리하고, 나머지는 validator 또는 기존 policy로 환원
  - 이번 계획의 채택안이다.

### 선택 방안
- **채택안**: 방안 3
- `RepositoryOwnershipPolicy`는 유지한다.
- `RepositoryCreationFactory`는 도입하지 않는다.
- `BranchWritePolicy`는 제거하고 `BranchFactory`로 대체한다.
- `RepositoryMembershipPolicy`는 제거하고, `RepositoryMemberValidator + RepositoryMembershipFactory` 조합으로 정리한다.
- `RepositoryCreationPlan`, `BranchCreationContext`는 `repository.application.contract.internal` 하위 클래스로 둔다.

### 범위
- `server/src/main/java/io/jgitkins/server/repository/application/support/ownership/RepositoryOwnershipPolicy.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/branch/BranchWritePolicy.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/membership/RepositoryMembershipPolicy.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryMemberService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/contract/internal/RepositoryCreationPlan.java`
- `server/src/main/java/io/jgitkins/server/repository/application/contract/internal/BranchCreationContext.java`
- 관련 테스트, import, package convention 정리

### 핵심 판단
- `RepositoryOwnershipPolicy`
  - 현재 `prepareCreation(...)`은 owner 해석, namespace 계산, `InitialCommitOptions` 생성, draft 조립을 한 번에 담당한다.
  - 다만 호출부가 사실상 `RepositoryManagementService` 하나이고, 분리 이득보다 indirection 비용이 더 크다.
  - 지금 단계에서는 별도 factory를 추가하지 않고, 책임 축소 여부는 후속으로 다시 본다.
- `BranchWritePolicy`
  - branch 생성은 metadata 생성으로 끝나지 않는다.
  - source branch 결정, metadata 생성, DB 저장, git branch 생성까지 하나의 application 생성 흐름으로 읽힌다.
  - 따라서 `BranchFactory`가 적절하다.
- `RepositoryMembershipPolicy`
  - 현재 구조는 `RepositoryMemberValidator` thin wrapper에 가깝다.
  - wrapper를 유지할 이유가 약하다.
  - 생성은 `RepositoryMembershipFactory`, 검증은 `RepositoryMemberValidator`가 직접 담당하는 편이 더 자연스럽다.
- `RepositoryCreationPlan`, `BranchCreationContext`
  - 둘 다 단순 로컬 임시값이 아니라 collaborator/port 경계를 넘는 application 계약값이다.
  - 따라서 service 내부 record보다 `repository.application.contract.internal` 하위 명시적 클래스로 두는 편이 더 명확하다.

### 목표 패키지 방향
- `repository.application.support.ownership`
  - `RepositoryOwnershipPolicy`
- `repository.application.support.branch`
  - `BranchFactory`
- `repository.application.support.membership`
  - `RepositoryMembershipFactory`
- `repository.application.contract.internal`
  - `RepositoryCreationPlan`
  - `BranchCreationContext`

### 단계별 계획
1. `RepositoryOwnershipPolicy`는 유지하되, 추가 분리는 보류한다.
2. `BranchWritePolicy`를 제거하고 `BranchFactory`를 도입한다.
3. `RepositoryMembershipPolicy`를 제거하고 `RepositoryMembershipFactory`를 추가한다.
4. `RepositoryCreationPlan`, `BranchCreationContext`를 `repository.application.contract.internal`로 이동한다.
5. `RepositoryMemberService`는 `RepositoryMemberValidator`를 직접 주입받도록 정리한다.
6. 관련 테스트, import, 아키텍처 규칙을 정리한다.

### 점진 이관 순서
1. `BranchFactory` 도입
2. `BranchManagementService` 호출부 정리
3. `RepositoryMembershipFactory` 도입
4. `RepositoryCreationPlan`, `BranchCreationContext` 계약 클래스 이동
5. `RepositoryMembershipPolicy` 제거 및 `RepositoryMemberValidator` 직접 사용
6. 테스트 보강 및 회귀 검증

### 검증 기준
- `RepositoryManagementService`는 기존보다 더 복잡해지지 않아야 한다.
- `BranchManagementService`는 branch 생성 절차의 세부 단계를 직접 다루지 않아야 한다.
- `RepositoryMemberService`는 생성 규칙만 factory에 위임하고, 검증은 validator로 직접 읽혀야 한다.
- `RepositoryCreationPlan`, `BranchCreationContext`는 로컬 record가 아니라 contract 클래스로 읽혀야 한다.
- 전체 테스트와 관련 package convention 테스트가 유지되어야 한다.

### 예시 코드

#### 1. `RepositoryOwnershipPolicy`는 유지
```java
package io.jgitkins.server.repository.application.support.ownership;

import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryOwnershipPolicy {

    private final RepositoryValidator repositoryValidator;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;

    public RepositoryCreationPlan prepareCreation(RepositoryCreateCommand command) {
        OwnerType ownerType = command.ownerType();
        OwnerId ownerId = resolveOwnerId(ownerType, command.organizeId());
        RepositoryName repositoryName = RepositoryName.from(command.repoName());

        repositoryValidator.validateRepositoryNameUnique(ownerType, ownerId, repositoryName);

        String namespace = repositoryNamespaceResolver.resolve(ownerType, ownerId);
        InitialCommitOptions initialCommitOptions = InitialCommitOptions.of(
                command.readme(),
                command.message(),
                command.authorName(),
                command.authorEmail()
        );

        Repository repository = Repository.create(
                ownerType,
                ownerId,
                repositoryName,
                RepositoryPath.from(command.repoName()),
                BranchName.of(command.mainBranch()),
                command.visibility() != null ? command.visibility() : RepositoryVisibility.PRIVATE,
                command.description(),
                RepositoryPathHelper.buildClonePath(namespace, command.repoName()),
                command.credentialId(),
                initialCommitOptions.requiresInitialContent()
        );

        return new RepositoryCreationPlan(repository, initialCommitOptions);
    }

    public void validateDeletion(Repository repository) {
        repositoryValidator.enforceDeletionPermission(repository);
    }

    private OwnerId resolveOwnerId(OwnerType ownerType, Long organizeId) {
        repositoryValidator.validateOwnership(ownerType, organizeId);
        if (ownerType == OwnerType.ORGANIZATION) {
            return OwnerId.of(organizeId);
        }
        return OwnerId.of(repositoryValidator.requireCurrentUserId());
    }

}
```
현재 기준으로 `RepositoryOwnershipPolicy`는 더 이상 잘게 분리하지 않는다. 생성 흐름 호출부가 사실상 `RepositoryManagementService` 하나이고, 여기서 추가 factory를 도입해도 응집도 이득보다 indirection 비용이 더 크다. 후속으로 생성 규칙이 늘어나거나 다른 use case가 동일 조립 흐름을 재사용할 때만 다시 분리를 검토한다.

```java
package io.jgitkins.server.repository.application.contract.internal;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;

public record RepositoryCreationPlan(
        Repository repository,
        InitialCommitOptions initialCommitOptions
) {
}
```

#### 2. `BranchFactory` 도입
```java
package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchFactory {

    private final BranchCreationValidator branchCreationValidator;
    private final BranchRepository branchRepository;
    private final BranchGitPort branchGitPort;

    public Branch create(
            BranchCreateCommand command,
            String namespace,
            Repository repository
    ) {
        String sourceBranch = branchCreationValidator.validateAndResolveSource(command, repository);
        Branch branch = Branch.create(command.repositoryId(), command.branchName());
        BranchCreationContext creationContext = BranchCreationContext.of(
                command,
                namespace,
                repository,
                sourceBranch
        );

        branchRepository.save(branch);
        branchGitPort.createBranch(creationContext);
        return branch;
    }
}
```
`BranchRepository` 구현체가 DB 영속화와 실제 파일시스템/git 프로비저닝을 함께 담당하는 구조는 피한다. 영속화는 `BranchRepository`가, git 브랜치 생성/삭제 같은 외부 상태 변경은 `BranchGitPort`가 담당해야 한다. `BranchFactory`는 두 collaborator를 조합해 생성 절차를 orchestration할 수 있지만, 저장소와 파일시스템 책임을 한 adapter로 합치지는 않는다.

```java
package io.jgitkins.server.repository.application.contract.internal;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.domain.aggregate.Repository;

public record BranchCreationContext(
        BranchCreateCommand command,
        String namespace,
        Repository repository,
        String sourceBranch
) {
    public static BranchCreationContext of(
            BranchCreateCommand command,
            String namespace,
            Repository repository,
            String sourceBranch
    ) {
        return new BranchCreationContext(command, namespace, repository, sourceBranch);
    }
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.repository.application.port.in.BranchDeleteUseCase;
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
public class BranchManagementService implements BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchFactory branchFactory;
    private final BranchGitPort branchGitPort;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        BranchWriteContext context = loadWriteContext(command.repositoryId());
        branchFactory.create(command, context.namespace(), context.repository());
    }

    @Override
    @Transactional
    public void deleteBranch(Long repositoryId, String branchName) {
        BranchWriteContext context = loadWriteContext(repositoryId);
        Branch branch = loadExistingBranch(repositoryId, branchName);

        branch.delete();
        branchRepository.delete(branch);
        branchGitPort.deleteBranch(
                context.namespace(),
                context.repository().getName().getValue(),
                branchName
        );
    }

    private BranchWriteContext loadWriteContext(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchWriteContext(repository, namespace);
    }

    private Branch loadExistingBranch(Long repositoryId, String branchName) {
        return branchRepository.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new BranchNotFoundException(branchName));
    }

    private record BranchWriteContext(Repository repository, String namespace) {
    }
}

```
`BranchWriteContext`는 별도 contract 클래스로 분리하지 않는다. 현재는 `BranchManagementService` 내부에서만 쓰이는 짧은 로컬 조합값이라 private record로 두는 편이 더 단순하다. 반면 `BranchCreationContext`는 factory와 git port 사이를 넘는 계약값이므로 `repository.application.contract.internal`로 승격한다.

#### 3. `RepositoryMembershipFactory + RepositoryMemberValidator` 조합
```java
package io.jgitkins.server.repository.application.support.membership;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMembershipFactory {

    public RepositoryMember createMember(RepositoryMemberAddCommand command) {
        RepositoryId repositoryId = RepositoryId.of(command.repositoryId());
        UserId userId = UserId.of(command.userId());
        RepositoryMemberRole role = command.role() != null ? command.role() : RepositoryMemberRole.READER;
        return RepositoryMember.create(repositoryId, userId, role, null);
    }
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberRemoveUseCase;
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
public class RepositoryMemberService implements RepositoryMemberAddUseCase,
                                                RepositoryMemberRemoveUseCase,
                                                RepositoryMemberQueryUseCase {

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
- `RepositoryOwnershipPolicy`는 지금 단계에서는 유지가 더 실용적이다.
- `BranchFactory`는 실제 생성 흐름을 감싸므로 도입 가치가 있다.
- membership는 별도 policy보다 `validator + factory` 조합이 더 단순하고 명확하다.
- `RepositoryCreationPlan`, `BranchCreationContext`는 `repository.application.contract.internal` 하위 명시적 계약 클래스로 둔다.
- 다음 구현은 `branch factory 도입 -> contract internal 이동 -> membership wrapper 제거 -> 테스트 정리` 순서로 진행한다.
