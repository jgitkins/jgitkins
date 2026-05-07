# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.28 Repository Context에서 Branch 귀속 수준 정리 계획서

### 배경
- 현재 문서와 코드 모두 `Branch`를 `Repository Context` 안에서 다루고 있지만, `Repository` aggregate 내부 컬렉션으로 편입할지 여부는 아직 구현 기준으로 닫히지 않았다.
- `Branch`는 `repositoryId + branchName`에 식별이 의존하고, 기본 브랜치 삭제 금지 같은 규칙도 `Repository` 문맥 안에서만 의미가 있다.
- 반면 branch head와 commit graph는 외부 Git 상태이며, 특정 branch 작업을 위해 `Repository`와 branch 전체 집합을 항상 함께 복원하는 모델은 과하다.

### 한 줄 결론
- `Branch는 Repository Context에 귀속된 내부 Entity이지만, Repository aggregate가 branch 컬렉션을 직접 소유하는 방식으로는 모델링하지 않는다.`

### 목표
- `Branch`의 소속을 `Repository Context`로 명확히 고정한다.
- `Repository` aggregate와 `Branch` entity의 경계를 분리한다.
- branch 관련 포트, 서비스, 패키지 위치를 `Repository Context` 기준으로 재정렬한다.
- `Repository`가 직접 가져야 할 브랜치 규칙과, branch 전용 협력자가 가져갈 절차를 분리한다.

### 범위
- `server/src/main/java/io/jgitkins/server/repository/domain/entity/Branch.java`
- `server/src/main/java/io/jgitkins/server/repository/domain/repository/BranchRepository.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchManagementService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchLoadService.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/branch/BranchFactory.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/BranchGitPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/BranchQueryPort.java`
- 관련 테스트, package convention, 문서

### 핵심 판단
- `Branch`는 `Repository` 없이는 의미가 없으므로 `Repository Context` 소속이다.
- 하지만 `Repository` aggregate가 `List<Branch>`를 직접 소유하는 모델은 채택하지 않는다.
- 이유는 다음과 같다.
  - 특정 branch 조회/변경을 위해 `Repository`와 branch 전체 집합을 함께 복원하는 방향으로 흐르기 쉽다.
  - aggregate의 조회/일관성 경계를 불필요하게 키운다.
  - 실제 branch head와 commit graph는 외부 Git 상태라 aggregate 내부 상태와 결이 다르다.
- 따라서 `문맥 소유권`은 `Repository`, `영속/조작 단위`는 `Branch` 별도 entity로 유지한다.

### 책임 경계
- `Repository`
  - initialized 여부
  - default branch 이름
  - branch 생성 가능 여부를 판단하는 상위 규칙
- `Branch`
  - branch 메타데이터
  - default branch 삭제 금지 같은 개별 규칙
- `BranchRepository`
  - branch 단건/이름 기준 영속 조회
- `BranchFactory`
  - source branch 결정
  - branch 메타데이터 생성
  - branch 영속화
  - Git branch 생성 orchestration
- `BranchGitPort`
  - 실제 Git branch 생성/삭제

### 목표 패키지 방향
```text
server/src/main/java/io/jgitkins/server/repository/
  domain/
    entity/
      Branch.java
    repository/
      BranchRepository.java
  application/
    service/
      BranchManagementService.java
      BranchLoadService.java
    support/
      branch/
        BranchFactory.java
    port/
      out/
        BranchGitPort.java
        BranchQueryPort.java
```

### 단계별 계획
1. `Branch`를 `Repository Context` 소속 entity로 문서상 확정한다.
2. 현재 `Branch.java`와 `BranchRepository.java`의 목표 위치를 `repository.domain.*` 기준으로 고정한다.
3. `BranchManagementService`, `BranchLoadService`, `BranchFactory`, `BranchGitPort`, `BranchQueryPort`가 모두 `Repository Context` 아래에 있다는 점을 패키지로 드러낸다.
4. `Repository`가 직접 branch 컬렉션을 소유하지 않는다는 원칙을 테스트와 문서에 반영한다.
5. branch 생성/삭제 시 `Repository` aggregate 복원은 상위 규칙 확인용으로만 사용하고, branch 메타데이터 조작은 `BranchRepository`와 `BranchFactory`가 담당하도록 정리한다.

### 검증 기준
- `Repository` aggregate는 branch 컬렉션을 직접 필드로 소유하지 않아야 한다.
- branch 생성/삭제는 `Repository` 전체와 branch 목록을 함께 복원하지 않아야 한다.
- `BranchManagementService`는 `Repository`를 상위 규칙 확인용으로만 사용해야 한다.
- `BranchRepository`와 `BranchGitPort`는 각각 DB 메타데이터와 외부 Git 상태 경계를 유지해야 한다.
- 문서와 패키지 구조가 `Branch는 Repository Context 소속, but not embedded collection` 결론과 일치해야 한다.

### 예시 코드

#### 1. 지양하는 모델
```java
package io.jgitkins.server.repository.domain.aggregate;

import java.util.ArrayList;
import java.util.List;

public class Repository {

    private final List<Branch> branches = new ArrayList<>();

    public Branch findBranch(String branchName) {
        return branches.stream()
                .filter(branch -> branch.getName().equals(branchName))
                .findFirst()
                .orElseThrow();
    }

    public void createBranch(String branchName) {
        boolean exists = branches.stream()
                .anyMatch(branch -> branch.getName().equals(branchName));
        if (exists) {
            throw new IllegalArgumentException("duplicate branch");
        }
        branches.add(Branch.create(getId().getValue(), branchName));
    }
}
```

이 모델은 branch 한 개를 다루기 위해 aggregate와 branch 집합 전체를 함께 끌고 오게 만들기 쉽다.

#### 2. 채택하는 모델
```java
package io.jgitkins.server.repository.domain.entity;

import io.jgitkins.server.domain.exception.DefaultBranchDeletionNotAllowedException;
import lombok.Getter;

@Getter
public class Branch {

    private final Long repositoryId;
    private final String name;
    private final boolean locked;
    private final boolean ciEnabled;
    private final boolean defaultBranch;

    private Branch(Long repositoryId, String name, boolean locked, boolean ciEnabled, boolean defaultBranch) {
        this.repositoryId = repositoryId;
        this.name = name;
        this.locked = locked;
        this.ciEnabled = ciEnabled;
        this.defaultBranch = defaultBranch;
    }

    public static Branch create(Long repositoryId, String name) {
        return create(repositoryId, name, false, false, false);
    }

    public static Branch create(
            Long repositoryId,
            String name,
            boolean locked,
            boolean ciEnabled,
            boolean defaultBranch
    ) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("Repository ID cannot be null.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name cannot be empty.");
        }
        return new Branch(repositoryId, name, locked, ciEnabled, defaultBranch);
    }

    public void delete() {
        if (defaultBranch) {
            throw new DefaultBranchDeletionNotAllowedException(name);
        }
    }
}
```

#### 3. BranchManagementService 예시
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchManagementUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final RepositoryRepository repositoryRepository;
    private final BranchFactory branchFactory;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        BranchRepositoryContext context = loadBranchRepositoryContext(command.repositoryId());
        branchFactory.create(command, context.namespace(), context.repository());
    }

    private BranchRepositoryContext loadBranchRepositoryContext(Long repositoryId) {
        Repository repository = repositoryRepository.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchRepositoryContext(repository, namespace);
    }

    private record BranchRepositoryContext(Repository repository, String namespace) {
    }
}
```

핵심은 `Repository`를 branch 목록 컨테이너로 쓰는 것이 아니라, branch 작업에 필요한 상위 규칙 확인용 aggregate로만 사용한다는 점이다.

#### 4. BranchFactory 예시
```java
package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchFactory {

    private final BranchCreationValidator branchCreationValidator;
    private final BranchRepository branchRepository;
    private final BranchGitPort branchGitPort;

    public Branch create(BranchCreateCommand command, String namespace, Repository repository) {
        String sourceBranch = branchCreationValidator.validateAndResolveSource(command, repository);
        Branch branch = Branch.create(command.repositoryId(), command.branchName());

        branchRepository.save(branch);
        branchGitPort.createBranch(
                new BranchCreationContext(
                        command.repositoryId(),
                        namespace,
                        repository.getName().getValue(),
                        command.branchName(),
                        sourceBranch
                )
        );

        return branch;
    }
}
```

이 구조에서는 `Repository`가 브랜치 집합 전체를 직접 관리하지 않아도, 필요한 규칙과 절차를 충분히 분리할 수 있다.
