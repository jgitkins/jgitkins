# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.32 Repository Overview 연관 객체 Repository Context 이관 계획서

### 상세 문서
- **Detail 1**: `.taskmaster/docs/refactor/task_2_32_detail_1_boundary_and_decisions.md`
  - Repository Overview의 소유 경계, 3가지 방법 비교, 대상 객체 분류, UseCase 간 의존성 해소 기준을 다룬다.
- **Detail 2**: `.taskmaster/docs/refactor/task_2_32_detail_2_contract_and_port_migration.md`
  - `RepositoryOverviewResult`, `RepositoryOverviewUseCase` 이관과 import 변경 스니펫을 다룬다.
- **Detail 3**: `.taskmaster/docs/refactor/task_2_32_detail_3_service_orchestration.md`
  - `RepositoryOverviewService` 이동과 inbound UseCase 의존성 제거 설계를 다룬다.
- **Detail 4**: `.taskmaster/docs/refactor/task_2_32_detail_4_controller_and_tests.md`
  - `WebRepositoryController`, `RepositoryManagementController`, 테스트 패키지와 convention test 수정을 다룬다.
- **Detail 5**: `.taskmaster/docs/refactor/task_2_32_detail_5_validation_and_followups.md`
  - 검증 명령, 실패 모드, 후속 task 후보, 선택 개선안을 다룬다.
- **Final Review**: `.taskmaster/docs/refactor/task_2_32_final_review.md`
  - 구현 전 최종검수 결과, 수정한 문제, 잔여 리스크, 최종 구현 순서를 다룬다.

### 배경
- 최초 문제 발견 시점의 `WebRepositoryController`는 `io.jgitkins.server.repository.presentation.api.web` 패키지에 위치했다.
- 해당 컨트롤러는 위치상 Repository Context의 presentation adapter이지만 `io.jgitkins.server.application.port.in.RepositoryOverviewUseCase`를 직접 의존한다.
- `RepositoryOverviewService`와 `RepositoryOverviewResult`도 top-level `application` 패키지에 남아 있어 Repository Context로 재정렬된 presentation adapter와 application use case 사이에 패키지 불일치가 발생한다.
- 이 불일치는 `Repository Context presentation adapter 패키지 재정렬` 이후 남은 후속 정리 지점이다.
- 현재 구조는 기능적으로 동작할 수 있으나, bounded context 기준으로 보면 `repository.presentation`이 top-level `application`의 repository 화면 조합 유스케이스를 호출하는 모양이 되어 응집도가 낮아진다.
- 최종 구현에서는 `RepositoryOverview*` application 객체는 Repository Context로 옮기고, web internal/BFF adapter인 `WebRepositoryController`는 `WebOrganizeController`와 같은 `io.jgitkins.server.presentation.api.web`로 이동한다.

### 최초 코드 관찰
- `server/src/main/java/io/jgitkins/server/repository/presentation/api/web/WebRepositoryController.java`는 다음 의존성을 가졌다.
```java
package io.jgitkins.server.repository.presentation.api.web;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
```
- 최종 구현에서는 다음처럼 web internal/BFF adapter 위치를 top-level presentation으로 정리한다.
```java
package io.jgitkins.server.presentation.api.web;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
```
- `RepositoryOverviewUseCase`는 top-level application package에 위치한다.
```java
package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
```
- `RepositoryOverviewService`는 top-level application service이지만 Repository Context use case를 다수 사용한다.
```java
package io.jgitkins.server.application.service;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final BranchLoadUseCase branchLoadUseCase;
    private final FileTreeLoadUseCase fileTreeLoadUseCase;
    private final CurrentUserPort currentUserPersistencePort;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;
}
```
- `RepositoryManagementController`도 동일하게 top-level `RepositoryOverviewUseCase`를 사용한다.
```java
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```
- `ArchitecturePackageConventionTest`는 현재 `RepositoryOverviewService`를 top-level application service 목록에 포함한다.
```java
private static final String APPLICATION_SERVICE_PACKAGE = "io.jgitkins.server.application.service";
private static final String REPOSITORY_SERVICE_PACKAGE = "io.jgitkins.server.repository.application.service";

@Test
void applicationServices_resideInUnifiedServicePackage() {
    List<Class<?>> serviceClasses = List.of(
            RepositoryFileService.class,
            RepositoryOverviewService.class);

    serviceClasses.forEach(serviceClass -> assertEquals(APPLICATION_SERVICE_PACKAGE, serviceClass.getPackageName()));
}
```

### 주요 개념 정리
- **Bounded Context**는 특정 도메인 언어와 모델이 일관되게 유지되는 경계다. Repository Context 내부에서는 repository, branch, repository member, repository permission 같은 용어와 흐름이 같은 의미를 가져야 한다.
- **Presentation Adapter**는 HTTP 요청, path variable, request parameter, response wrapper처럼 외부 입출력 형식을 application use case 호출로 변환하는 계층이다.
- **Application UseCase**는 사용자 시나리오를 오케스트레이션하는 진입점이다. 이 작업에서 `RepositoryOverviewUseCase`는 저장소 화면 개요 조회 시나리오를 대표한다.
- **Read Model Result**는 조회 화면에 필요한 데이터를 모아 반환하는 application contract다. `RepositoryOverviewResult`는 repository metadata, branch 목록, file tree, 권한을 모은 read model이다.
- **Blast Radius**는 변경이 직접 영향을 주는 파일 범위다. 이번 작업은 `RepositoryOverview*` 직접 호출부와 package convention test를 1차 blast radius로 본다.

### 목표
- `RepositoryOverviewUseCase`, `RepositoryOverviewService`, `RepositoryOverviewResult`를 Repository Context의 application 계층으로 이관한다.
- `WebRepositoryController`와 `RepositoryManagementController`가 Repository Context의 application contract를 사용하도록 정렬한다.
- `RepositoryOverviewService`가 다른 inbound UseCase를 주입하지 않도록 query/out port와 support collaborator를 직접 사용하게 한다.
- `RepositoryKey`, `FileEntry`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase`, `CurrentUserPort`는 즉시 이관하지 않고 성격별로 분류한다.
- path 기반 overview 조회 흐름을 application use case로 끌어올릴지 검토하고, 컨트롤러 조합 로직을 줄이는 방향을 제시한다.
- 구현 단계에서 컴파일, 단위 테스트, controller 테스트, package convention 테스트가 깨지는 지점을 미리 정의한다.

### 비목표
- 이번 계획 단계에서는 Java 소스 구현을 수행하지 않는다.
- `RepositoryFileService`, `FileEntry`, `FileTreeLoadUseCase`, `FileGitPort` 전체를 Repository Context로 즉시 이관하지 않는다.
- `GitRepositoryAccessUseCase`의 top-level interface 제거는 이번 작업에서 확정하지 않는다.
- REST endpoint path 변경은 하지 않는다.
- API response schema 변경은 하지 않는다.
- web frontend 호출부 변경은 이번 작업에 포함하지 않는다.

### 방법 조사 및 선택
- **방안 1: WebRepositoryController를 top-level presentation으로 되돌린다.**
  - 장점은 변경량이 작다.
  - 단점은 이미 진행된 Repository Context presentation 재정렬 방향과 반대다.
  - `RepositoryManagementController`, `BranchController`, `RepositoryContentController`가 `repository.presentation`으로 정렬된 이후에는 되돌리기가 장기 구조를 더 흐리게 만든다.
  - 평가 점수는 4/10이다.
- **방안 2: `RepositoryOverviewUseCase` import만 유지하고 문서로 예외 처리한다.**
  - 장점은 구현 변경이 없다.
  - 단점은 `repository.presentation`이 top-level application을 직접 바라보는 구조가 계속 남는다.
  - `ArchitecturePackageConventionTest`가 구조적 의도를 강제하지 못한다.
  - 평가 점수는 3/10이다.
- **방안 3: `RepositoryOverview*` 3종을 Repository Context application 하위로 이관한다.**
  - 장점은 presentation adapter, use case, result contract의 소유 경계가 맞아진다.
  - 장점은 `WebRepositoryController`가 동떨어져 보이는 원인을 직접 제거한다.
  - 장점은 `RepositoryOverviewService`가 이미 `RepositoryLoadUseCase`, `BranchLoadUseCase`, `RepositoryPermission`을 사용하고 있어 Repository Context 응집도가 높다.
  - 단점은 `RepositoryFileService`, `FileTreeLoadUseCase`, `FileEntry`, `GitRepositoryAccessUseCase`와의 경계가 여전히 남는다.
  - 평가 점수는 9/10이다.
- **선택 방안은 방안 3이다.**
- 선택 이유는 작은 변경으로 현재 관찰된 구조 불일치의 핵심 원인을 제거하고, 나머지 cross-context 객체는 별도 판단 가능한 상태로 남길 수 있기 때문이다.

### 대상 객체 분류
| 객체 | 현재 위치 | 목표 위치 | 분류 | 이유 |
|---|---|---|---|---|
| `RepositoryOverviewUseCase` | `application.port.in` | `repository.application.port.in` | 즉시 이관 | Repository overview는 Repository Context 화면 조회 유스케이스다. |
| `RepositoryOverviewService` | `application.service` | `repository.application.service` | 즉시 이관 | 내부 의존성 대부분이 Repository Context 조회 포트다. |
| `RepositoryOverviewResult` | `application.dto.result` | `repository.application.contract.result` | 즉시 이관 | `RepositoryResult`, `BranchSearchResult`, `RepositoryPermission`과 같은 contract 계열이다. |
| `RepositoryKey` | `application.dto` | 유지 또는 후속 이관 | 후속 검토 | `RepositoryContentController`와 file service 흐름도 사용하므로 별도 path parsing utility 논의가 필요하다. |
| `FileEntry` | `application.dto` | 유지 | 이번 범위 제외 | file tree, commit, content API 전반에 쓰이는 read model이다. 한 번에 옮기면 blast radius가 커진다. |
| `FileTreeLoadUseCase` | `application.port.in` | 유지 | 이번 범위 제외 | file/content 기능의 context 소유권이 아직 정리되지 않았다. |
| `GitRepositoryAccessUseCase` | `application.port.in` | 유지 | 이번 범위 제외 | git smart http authorizer, filter, validator에서도 사용한다. |
| `CurrentUserPort` | `application.port.out` | 유지 | 공통 포트 | 인증 사용자 식별은 Repository Context 전용 책임이 아니다. |

### UseCase 간 의존성 해소 기준
- `RepositoryOverviewUseCase`는 화면 개요 조회를 대표하는 inbound use case다.
- 이 use case의 구현체인 `RepositoryOverviewService`가 `RepositoryLoadUseCase`, `BranchLoadUseCase`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase` 같은 다른 inbound use case를 직접 주입하면 application layer 내부에서 use case가 use case를 호출하는 구조가 된다.
- 이 구조는 동작할 수 있으나, 각 use case의 독립 진입점 책임과 orchestration 책임이 섞인다.
- 따라서 TO-BE 구현에서는 다음 기준을 적용한다.
  - Repository metadata 조회는 `RepositoryQueryPort`를 직접 사용한다.
  - Branch 목록 조회는 `BranchQueryPort`를 직접 사용한다.
  - File tree 조회는 inbound `FileTreeLoadUseCase`가 아니라 outbound `FileGitPort`를 직접 사용한다.
  - 권한 계산은 inbound `GitRepositoryAccessUseCase` facade가 아니라 `repository.application.support.GitRepositoryAccessService`를 직접 사용한다.
  - 현재 사용자 식별은 cross-cutting port인 `CurrentUserPort`를 유지한다.
- 이 기준은 `RepositoryOverviewService`가 하나의 화면 조회 orchestration service로서 필요한 query/out port를 조합하게 만들고, inbound use case끼리 얽히는 구조를 막는다.

#### UseCase 의존성 AS-IS / TO-BE
```java
// AS-IS: inbound UseCase가 다른 inbound UseCase를 의존한다.
@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final BranchLoadUseCase branchLoadUseCase;
    private final FileTreeLoadUseCase fileTreeLoadUseCase;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;
}
```

```java
// TO-BE: orchestration service가 query/out port와 support collaborator를 조합한다.
@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private final RepositoryQueryPort repositoryQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final FileGitPort fileGitPort;
    private final CurrentUserPort currentUserPort;
    private final GitRepositoryAccessService gitRepositoryAccessService;
}
```

### 목표 패키지 구조
```text
server/src/main/java/io/jgitkins/server/repository
├── application
│   ├── contract
│   │   └── result
│   │       └── RepositoryOverviewResult.java
│   ├── port
│   │   └── in
│   │       └── RepositoryOverviewUseCase.java
│   └── service
│       └── RepositoryOverviewService.java
└── presentation
    └── api
        ├── rest
        │   └── RepositoryManagementController.java
        └── web
            └── WebRepositoryController.java
```

### 단계별 이관 계획

#### 단계 1: `RepositoryOverviewResult`를 Repository Context contract로 이동한다.
- 기존 파일을 다음 위치로 이동한다.
```text
FROM server/src/main/java/io/jgitkins/server/application/dto/result/RepositoryOverviewResult.java
TO   server/src/main/java/io/jgitkins/server/repository/application/contract/result/RepositoryOverviewResult.java
```
- AS-IS 코드는 다음과 같다.
```java
package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import java.util.List;

public record RepositoryOverviewResult(
        RepositoryResult repository,
        List<BranchSearchResult> branches,
        List<FileEntry> tree,
        String selectedBranch,
        String role,
        boolean writable
) {
}
```
- TO-BE 코드는 다음과 같다.
```java
package io.jgitkins.server.repository.application.contract.result;

import io.jgitkins.server.application.dto.FileEntry;
import java.util.List;

public record RepositoryOverviewResult(
        RepositoryResult repository,
        List<BranchSearchResult> branches,
        List<FileEntry> tree,
        String selectedBranch,
        String role,
        boolean writable
) {
}
```
- `FileEntry`는 이번 단계에서 그대로 top-level application dto를 import한다.
- 이유는 `FileEntry`의 사용처가 repository overview보다 넓고, file/content read model 경계가 아직 분리되지 않았기 때문이다.

#### 단계 2: `RepositoryOverviewUseCase`를 Repository Context port로 이동한다.
- 기존 파일을 다음 위치로 이동한다.
```text
FROM server/src/main/java/io/jgitkins/server/application/port/in/RepositoryOverviewUseCase.java
TO   server/src/main/java/io/jgitkins/server/repository/application/port/in/RepositoryOverviewUseCase.java
```
- AS-IS 코드는 다음과 같다.
```java
package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
```
- 1차 TO-BE 코드는 다음과 같다.
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
```
- 2차 개선 후보는 다음과 같다.
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);

    RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch);
}
```
- 2차 개선 후보는 컨트롤러의 path 기반 repository 조회 조합 로직을 application use case로 옮기기 위한 것이다.
- 1차 구현에서는 이동과 import 정리에 집중하고, 2차 구현에서 `getOverviewByPath`를 추가하는 순서가 안전하다.

#### 단계 3: `RepositoryOverviewService`를 Repository Context service로 이동한다.
- 기존 파일을 다음 위치로 이동한다.
```text
FROM server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java
TO   server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryOverviewService.java
```
- AS-IS 핵심 코드는 다음과 같다.
```java
package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private static final String ROOT_PATH = "";

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final BranchLoadUseCase branchLoadUseCase;
    private final FileTreeLoadUseCase fileTreeLoadUseCase;
    private final CurrentUserPort currentUserPersistencePort;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    @Override
    public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {
        RepositoryResult repository = repositoryLoadUseCase.loadRepository(repositoryId);
        RepositoryKey key = resolveRepositoryKey(repository);
        List<BranchSearchResult> branches = branchLoadUseCase.loadBranches(repositoryId);
        String selectedBranch = resolveBranch(branch, branches);
        List<FileEntry> tree = fileTreeLoadUseCase.getTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);
        Long userId = currentUserPersistencePort.resolveCurrentUserId().orElse(null);
        RepositoryPermission permission = gitRepositoryAccessUseCase.resolvePermission(
                null,
                key != null ? key.namespace() : null,
                key != null ? key.repoName() : null,
                userId);

        return new RepositoryOverviewResult(
                repository,
                branches,
                tree,
                selectedBranch,
                permission.role(),
                permission.writable());
    }
}
```
- 1차 TO-BE 코드는 다음과 같다.
```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

    private static final String ROOT_PATH = "";

    private final RepositoryQueryPort repositoryQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final FileGitPort fileGitPort;
    private final CurrentUserPort currentUserPort;
    private final GitRepositoryAccessService gitRepositoryAccessService;

    @Override
    public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {
        RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        return getOverview(repository, branch);
    }

    private RepositoryOverviewResult getOverview(RepositoryResult repository, String branch) {
        RepositoryKey key = resolveRepositoryKey(repository);
        List<BranchSearchResult> branches = branchQueryPort.findAllByRepositoryId(repository.id());
        String selectedBranch = resolveBranch(branch, branches);
        List<FileEntry> tree = fileGitPort.listTree(
                key.namespace(),
                key.repoName(),
                selectedBranch,
                ROOT_PATH);
        Long userId = currentUserPort.resolveCurrentUserId().orElse(null);
        RepositoryPermission permission = gitRepositoryAccessService.resolvePermission(
                null,
                key.namespace(),
                key.repoName(),
                userId);

        return new RepositoryOverviewResult(
                repository,
                branches,
                tree,
                selectedBranch,
                permission.role(),
                permission.writable());
    }

    private String resolveBranch(String branch, List<BranchSearchResult> branches) {
        if (StringUtils.hasText(branch)) {
            return branch;
        }

        return branches.stream()
                .filter(BranchSearchResult::defaultBranch)
                .findFirst()
                .map(BranchSearchResult::name)
                .orElseGet(() -> branches.isEmpty() ? null : branches.get(0).name());
    }

    private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
        RepositoryKey key = RepositoryKey.fromPath(repository.clonePath());
        return key != null ? key : RepositoryKey.fromPath(repository.path());
    }
}
```
- 위 1차 TO-BE는 이동과 import 정렬을 우선한다.
- 단, 기존 코드에는 `key`가 null일 때 `fileTreeLoadUseCase.getTree(key.namespace(), ...)`에서 NPE가 발생할 수 있는 구조가 있다.
- 구현 단계에서는 이 문제를 다음 중 하나로 처리한다.
```java
private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
    RepositoryKey key = RepositoryKey.fromPath(repository.clonePath());
    if (key != null) {
        return key;
    }

    key = RepositoryKey.fromPath(repository.path());
    if (key != null) {
        return key;
    }

    throw new RepositoryPathUnresolvableException(repository.id());
}
```
- 위 예외를 새로 만들 경우 application exception 정책과 GlobalExceptionHandler 매핑까지 확인해야 한다.
- 범위 통제를 위해 1차 구현에서는 기존 동작을 유지하고, null path 방어는 테스트에서 현재 위험으로 기록한 뒤 후속 작업으로 분리할 수 있다.

#### 단계 4: `WebRepositoryController` 위치와 import를 정리한다.
- `WebRepositoryController`는 다음 위치로 이동한다.
```text
FROM server/src/main/java/io/jgitkins/server/repository/presentation/api/web/WebRepositoryController.java
TO   server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java
```
- `WebRepositoryController` AS-IS는 다음과 같았다.
```java
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```
- 1차 TO-BE는 다음과 같다.
```java
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```
- 반환 타입의 풀 패키지명은 제거한다.
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
- 2차 TO-BE 후보는 다음과 같다.
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
- 2차 후보의 장점은 컨트롤러가 `loadRepositoryByPath`와 `getOverview`를 조합하지 않는다는 점이다.
- 2차 후보의 단점은 use case 계약 변경과 테스트 수정 범위가 늘어난다는 점이다.
- 이번 구현은 1차 TO-BE를 우선하고, 2차 후보는 선택 개선안으로 둔다.

#### 단계 5: `RepositoryManagementController` import를 정리한다.
- AS-IS는 다음과 같다.
```java
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```
- TO-BE는 다음과 같다.
```java
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```
- endpoint와 response wrapper는 유지한다.
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

#### 단계 6: 테스트 패키지와 import를 정리한다.
- `RepositoryOverviewServiceTest`는 다음 위치로 이동한다.
```text
FROM server/src/test/java/io/jgitkins/server/application/service/RepositoryOverviewServiceTest.java
TO   server/src/test/java/io/jgitkins/server/repository/application/service/RepositoryOverviewServiceTest.java
```
- 테스트 package는 다음과 같이 바꾼다.
```java
package io.jgitkins.server.repository.application.service;
```
- 테스트 import는 다음과 같이 바꾼다.
```java
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
```
- `RepositoryManagementControllerTest` import는 다음과 같이 바꾼다.
```java
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```
- `ArchitecturePackageConventionTest`는 `RepositoryOverviewService`를 repository service 목록으로 옮긴다.
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

### 데이터 흐름
```text
AS-IS

WebRepositoryController
  ├─ repository.application.port.in.RepositoryLoadUseCase
  └─ application.port.in.RepositoryOverviewUseCase
        └─ application.service.RepositoryOverviewService
              ├─ repository.application.port.in.RepositoryLoadUseCase
              ├─ repository.application.port.in.BranchLoadUseCase
              ├─ application.port.in.FileTreeLoadUseCase
              ├─ application.port.in.GitRepositoryAccessUseCase
              └─ application.port.out.CurrentUserPort

TO-BE 1차

WebRepositoryController
  ├─ repository.application.port.in.RepositoryLoadUseCase
  └─ repository.application.port.in.RepositoryOverviewUseCase
        └─ repository.application.service.RepositoryOverviewService
              ├─ repository.application.port.out.RepositoryQueryPort
              ├─ repository.application.port.out.BranchQueryPort
              ├─ application.port.out.FileGitPort
              ├─ repository.application.support.GitRepositoryAccessService
              └─ application.port.out.CurrentUserPort
```

### 실패 모드 및 방어 전략
| 실패 모드 | 원인 | 영향 | 방어 전략 |
|---|---|---|---|
| 컴파일 실패 | import 이동 누락 | server build 실패 | `rg "RepositoryOverviewUseCase|RepositoryOverviewResult|RepositoryOverviewService"`로 잔여 import를 확인한다. |
| Spring bean 주입 실패 | old interface와 new interface 혼재 | controller bean 생성 실패 | interface와 service import를 한 번에 이동하고 old file을 삭제한다. |
| UseCase 간 재의존 발생 | overview service가 `RepositoryLoadUseCase` 같은 inbound port를 계속 주입 | application orchestration 경계가 다시 흐려짐 | `RepositoryOverviewService`의 필드 타입을 query/out port와 support collaborator로 제한한다. |
| 테스트 package convention 실패 | `ArchitecturePackageConventionTest` 목록 미갱신 | test 실패 | `RepositoryOverviewService`를 repository service 목록으로 옮긴다. |
| API schema 변경 | result field 이름 변경 | web 화면 회귀 | record component 이름을 유지한다. |
| path overview 중복 유지 | 컨트롤러가 여전히 두 use case를 조합 | 구조 개선이 절반만 완료됨 | 2차 단계에서 `getOverviewByPath` 추가를 검토한다. |
| `RepositoryKey` null 문제 | clone path와 path 모두 파싱 실패 | overview 조회 NPE | 후속 작업으로 명시 예외 또는 path resolver를 도입한다. |

### 검증 전략
- 1차 컴파일 검증은 다음 명령으로 수행한다.
```bash
./gradlew :server:compileJava
```
- 1차 테스트 검증은 다음 명령으로 수행한다.
```bash
./gradlew :server:test
```
- 변경 직후 import 잔여 확인은 다음 명령으로 수행한다.
```bash
rg "io\\.jgitkins\\.server\\.application\\.(port\\.in\\.RepositoryOverviewUseCase|dto\\.result\\.RepositoryOverviewResult|service\\.RepositoryOverviewService)" server/src
```
- 신규 목표 package 존재 확인은 다음 명령으로 수행한다.
```bash
rg "RepositoryOverviewUseCase|RepositoryOverviewService|RepositoryOverviewResult" server/src/main/java/io/jgitkins/server/repository
```
- `RepositoryOverviewService`가 다른 inbound use case를 다시 주입하지 않는지는 다음 명령으로 확인한다.
```bash
rg "RepositoryLoadUseCase|BranchLoadUseCase|FileTreeLoadUseCase|GitRepositoryAccessUseCase" \
  server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryOverviewService.java
```
- 위 명령은 결과가 없어야 한다.
- controller endpoint 동작은 기존 테스트가 있다면 다음 기준으로 보강한다.
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

### 구현 순서 체크리스트
- [ ] `RepositoryOverviewResult` 파일을 `repository.application.contract.result`로 이동한다.
- [ ] `RepositoryOverviewUseCase` 파일을 `repository.application.port.in`으로 이동한다.
- [ ] `RepositoryOverviewService` 파일을 `repository.application.service`로 이동한다.
- [ ] `RepositoryOverviewService`의 inbound use case 의존성을 `RepositoryQueryPort`, `BranchQueryPort`, `FileGitPort`, `GitRepositoryAccessService`로 교체한다.
- [ ] `WebRepositoryController`의 import와 반환 타입을 정리한다.
- [ ] `RepositoryManagementController`의 import를 정리한다.
- [ ] `RepositoryOverviewServiceTest`를 repository test package로 이동한다.
- [ ] `RepositoryManagementControllerTest`의 mock import를 정리한다.
- [ ] `ArchitecturePackageConventionTest`에서 service package 기대값을 조정한다.
- [ ] `rg`로 old package 잔여 import를 제거한다.
- [ ] `./gradlew :server:compileJava`를 실행한다.
- [ ] `./gradlew :server:test`를 실행한다.

### 개선 사항 점검
- **개선안 1: `getOverviewByPath(namespace, repoName, branch)`를 `RepositoryOverviewUseCase`에 추가한다.**
  - 컨트롤러가 repository lookup과 overview 조회를 직접 조합하지 않게 된다.
  - path 기반 조회가 하나의 application flow로 응집된다.
  - 단점은 use case interface와 service test 범위가 늘어난다는 점이다.
- **개선안 2: `RepositoryKey`를 `RepositoryPathResolver` 또는 `RepositoryPathParser`로 분리한다.**
  - clone path와 repository path 파싱 정책을 한 곳에서 관리한다.
  - `RepositoryContentController`와 `RepositoryOverviewService`의 중복 path parsing을 줄일 수 있다.
  - 단점은 이번 작업보다 넓은 blast radius가 생긴다는 점이다.
- **개선안 3: `FileEntry`와 file tree use case의 context 소유권을 별도 task로 정리한다.**
  - file tree 조회가 Repository Context에 속하는지, Git content context로 분리할지 판단할 수 있다.
  - 단점은 현재 task의 완료 조건을 흐릴 수 있다.
- **선택 개선안은 개선안 1이다.**
- 이유는 `WebRepositoryController`가 동떨어져 보이는 원인을 가장 직접적으로 줄이고, 변경 범위가 `RepositoryOverviewUseCase`, `RepositoryOverviewService`, `WebRepositoryController`, service test 정도로 제한되기 때문이다.
- 다만 구현은 두 커밋 또는 두 단계로 나눈다.
  - 1단계는 package 이동과 import 정리다.
  - 2단계는 `WebRepositoryController`를 top-level web adapter로 이동하고 `getOverviewByPath` 추가와 컨트롤러 조합 제거를 적용한다.

### 선택 개선안 코드 스니펫
- `RepositoryOverviewUseCase`는 다음 형태가 된다.
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);

    RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch);
}
```
- `RepositoryOverviewService`는 path 기반 메서드를 다음처럼 추가한다.
```java
@Override
public RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch) {
    RepositoryResult repository = repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
            .orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
    return buildOverview(repository, branch);
}

@Override
public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {
    RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
            .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    return buildOverview(repository, branch);
}

private RepositoryOverviewResult buildOverview(RepositoryResult repository, String branch) {
    RepositoryKey key = resolveRepositoryKey(repository);
    List<BranchSearchResult> branches = branchQueryPort.findAllByRepositoryId(repository.id());
    String selectedBranch = resolveBranch(branch, branches);
    List<FileEntry> tree = fileGitPort.listTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);
    Long userId = currentUserPort.resolveCurrentUserId().orElse(null);
    RepositoryPermission permission = gitRepositoryAccessService.resolvePermission(
            null,
            key.namespace(),
            key.repoName(),
            userId);

    return new RepositoryOverviewResult(
            repository,
            branches,
            tree,
            selectedBranch,
            permission.role(),
            permission.writable());
}
```
- `WebRepositoryController`는 다음처럼 얇아진다.
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

### Autoplan Self-Review

#### CEO 관점
- 전제 1은 `Repository Overview`가 Repository Context 소유 기능이라는 판단이다.
- 이 전제는 타당하다. 결과 모델이 `RepositoryResult`, `BranchSearchResult`, `RepositoryPermission`을 중심으로 구성되어 있고 화면은 저장소 개요를 보여준다.
- 전제 2는 file tree와 current user 포트가 남아 있어도 1차 이관 가치가 있다는 판단이다.
- 이 전제도 타당하다. 모든 연관 객체를 한 번에 옮기는 것은 blast radius를 불필요하게 키운다.
- 12개월 후 후회할 지점은 `FileEntry`, `FileTreeLoadUseCase`, `RepositoryKey`를 계속 top-level application에 방치하는 것이다.
- 따라서 이번 작업은 overview 3종을 먼저 이관하고, file/content read model 정리는 별도 후속 task로 분리한다.

#### Engineering 관점
- 구조상 핵심 위험은 old interface와 new interface가 동시에 남아 Spring bean wiring이 깨지는 경우다.
- 이 위험은 파일 이동 후 old package import를 `rg`로 강제 확인하면 통제 가능하다.
- 테스트상 핵심 위험은 `ArchitecturePackageConventionTest`의 기대 목록을 수정하지 않는 경우다.
- 이 위험은 `RepositoryOverviewService`를 repository service 목록으로 옮기는 테스트 스니펫으로 해결한다.
- API 호환성 위험은 낮다. record field 이름과 endpoint path를 유지하기 때문이다.

#### DX 관점
- 이 작업은 외부 개발자 API가 아니라 내부 코드 구조와 테스트 진입점을 바꾸는 작업이다.
- 개발자 경험 측면에서 중요한 점은 검색 가능한 package convention을 만드는 것이다.
- 구현자가 `repository.application.port.in`에서 repository 관련 use case를 찾을 수 있어야 한다.
- 따라서 계획 문서에는 old import 제거 명령과 목표 package 확인 명령을 포함한다.

#### Design 관점
- UI 변경 범위는 없다.
- 화면 상태, 레이아웃, interaction 변경도 없다.
- 다만 web 화면 데이터 contract인 `RepositoryOverviewResult`의 JSON field 이름은 유지되어야 한다.

### Decision Audit Trail
| # | Phase | Decision | Classification | Principle | Rationale | Rejected |
|---|---|---|---|---|---|---|
| 1 | CEO | `RepositoryOverview*` 3종을 Repository Context로 이관한다. | Mechanical | Choose completeness | 현재 어색함의 직접 원인이며 blast radius가 작다. | top-level application 유지 |
| 2 | Eng | `FileEntry`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase`는 패키지 유지하되 `RepositoryOverviewService`의 직접 의존 대상에서는 제거한다. | Mechanical | Explicit over clever | 타입 소유권 이관과 orchestration 의존성 해소를 분리해야 변경 범위가 통제된다. | 모든 연관 객체 동시 이관 |
| 3 | Eng | `getOverviewByPath`는 2차 개선안으로 제시한다. | Taste | Pragmatic | 컨트롤러를 더 얇게 만들지만 1차 package 이동과 분리하면 검증이 쉽다. | 한 번에 계약 확장 |
| 4 | DX | old import 제거용 `rg` 명령을 검증 기준에 포함한다. | Mechanical | Bias toward action | 구조 리팩토링은 잔여 import가 가장 흔한 실패다. | 수동 확인 |

### NOT in scope
- `RepositoryFileService`의 Repository Context 이관은 이번 범위가 아니다.
- `FileEntry`를 `repository.application.contract.result`로 이동하는 작업은 이번 범위가 아니다.
- `GitRepositoryAccessUseCase`를 `repository.application.port.in`으로 이동하는 작업은 이번 범위가 아니다.
- `RepositoryKey`를 shared utility 또는 repository internal contract로 이동하는 작업은 이번 범위가 아니다.
- web frontend 코드 수정은 이번 범위가 아니다.

### 후속 task 후보
- `[server] Repository path parsing utility 중앙화`
- `[server] File tree read model 및 FileEntry context 소유권 정리`
- `[server] GitRepositoryAccessUseCase top-level application 의존 제거`
- `[server] Repository overview path 기반 use case 계약 추가 구현`

### 결론
- `WebRepositoryController`가 동떨어져 보이는 핵심 원인은 web internal/BFF adapter인데도 `repository.presentation.api.web`에 단독으로 남아 있었고, 동시에 top-level `RepositoryOverviewUseCase`와 `RepositoryOverviewResult`를 사용했기 때문이다.
- 1차 리팩토링은 `RepositoryOverviewUseCase`, `RepositoryOverviewService`, `RepositoryOverviewResult`를 `repository.application` 하위로 이관하는 것이 가장 적합하다.
- `RepositoryKey`, `FileEntry`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase`는 현재 사용처가 넓으므로 이번 작업에서 무리하게 함께 옮기지 않는다.
- 단, `RepositoryOverviewService`는 해당 inbound use case들을 직접 주입하지 않고 query/out port와 support collaborator를 사용해야 한다.
- 최종 구현에서는 `WebRepositoryController`를 `server.presentation.api.web`로 이동하고 `getOverviewByPath(namespace, repoName, branch)`를 추가해 조합 책임까지 제거한다.
