# Task 2.32 Detail 1: Repository Overview 경계와 결정 기준

### 목적
- 이 문서는 `RepositoryOverview*` 연관 객체를 Repository Context로 옮겨야 하는 이유와 이관 범위를 고정한다.
- 구현 문서가 아니라 설계 판단 문서다.
- 후속 상세 문서들은 이 문서의 결정 기준을 전제로 한다.

### 초기 문제
- `WebRepositoryController`는 `io.jgitkins.server.repository.presentation.api.web`에 위치한다.
- 그러나 `RepositoryOverviewUseCase`는 `io.jgitkins.server.application.port.in`에 있다.
- `RepositoryOverviewService`는 `io.jgitkins.server.application.service`에 있다.
- `RepositoryOverviewResult`는 `io.jgitkins.server.application.dto.result`에 있다.
- 결과적으로 Repository Context의 presentation adapter가 top-level application 계층의 overview use case를 바라본다.
- 추가 검수 결과 `WebRepositoryController` 자체도 web internal/BFF adapter 성격이 강하므로 `WebOrganizeController`와 같은 `io.jgitkins.server.presentation.api.web`에 두는 편이 더 일관적이다.

### 최초 문제 코드
```java
package io.jgitkins.server.repository.presentation.api.web;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/repositories")
public class WebRepositoryController {

    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryOverviewUseCase repositoryOverviewUseCase;
}
```

### 최종 위치 결정
```java
package io.jgitkins.server.presentation.api.web;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```

- `WebRepositoryController`는 Repository REST resource adapter가 아니라 web module이 소비하는 internal/BFF adapter다.
- 따라서 `WebOrganizeController`와 같은 `io.jgitkins.server.presentation.api.web`에 둔다.
- Repository Overview 유스케이스와 결과 모델은 Repository Context 소유이므로 `repository.application` 아래에 둔다.

### 판단 기준
- Repository Overview는 저장소 화면에 필요한 read model을 구성한다.
- 반환 모델은 `RepositoryResult`, `BranchSearchResult`, `RepositoryPermission`, file tree를 포함한다.
- 핵심 언어는 repository, branch, repository permission이다.
- 따라서 `RepositoryOverviewUseCase`, `RepositoryOverviewService`, `RepositoryOverviewResult`의 소유권은 Repository Context에 두는 편이 자연스럽다.

### 방법 조사

#### 방안 1: `WebRepositoryController`만 top-level presentation으로 되돌린다.
- 장점은 `WebOrganizeController`와 web internal API adapter 위치가 맞아진다.
- 단점은 overview application contract의 위치 문제는 그대로 남는다.
- 평가 점수는 6/10이다.

#### 방안 2: top-level `RepositoryOverviewUseCase`를 유지하고 예외로 문서화한다.
- 장점은 Java 코드 변경이 없다.
- 단점은 구조적 어색함이 계속 남는다.
- 단점은 package convention test가 이 어색함을 잡지 못한다.
- 평가 점수는 3/10이다.

#### 방안 3: `RepositoryOverview*` 3종을 Repository Context application 하위로 이관하고 `WebRepositoryController`는 top-level web adapter로 둔다.
- 장점은 Repository Overview application contract의 소유 경계가 맞아진다.
- 장점은 web internal API adapter가 `server.presentation.api.web` 아래에 모여 동떨어진 위치 문제가 사라진다.
- 장점은 blast radius가 `RepositoryOverview*`, `WebRepositoryController`, controller import, service test, architecture test로 제한된다.
- 단점은 `FileEntry`, `FileTreeLoadUseCase`, `GitRepositoryAccessUseCase`의 경계가 아직 남는다.
- 평가 점수는 9/10이다.

### 선택
- 방안 3을 선택한다.
- 이유는 작은 변경으로 application 경계 불일치와 web adapter 위치 불일치를 함께 제거할 수 있기 때문이다.
- 단, 모든 연관 객체를 한 번에 옮기지 않는다.

### 대상 객체 분류
| 객체 | 현재 위치 | 목표 위치 | 분류 | 이유 |
|---|---|---|---|---|
| `RepositoryOverviewUseCase` | `application.port.in` | `repository.application.port.in` | 즉시 이관 | Repository overview는 Repository Context 화면 조회 유스케이스다. |
| `RepositoryOverviewService` | `application.service` | `repository.application.service` | 즉시 이관 | Repository 화면 조회 orchestration이다. |
| `RepositoryOverviewResult` | `application.dto.result` | `repository.application.contract.result` | 즉시 이관 | Repository contract read model이다. |
| `RepositoryKey` | `application.dto` | 유지 또는 후속 이관 | 후속 검토 | content controller와 file service도 사용한다. |
| `FileEntry` | `application.dto` | 유지 | 이번 범위 제외 | file tree와 commit 조회 등 사용처가 넓다. |
| `FileTreeLoadUseCase` | `application.port.in` | 유지 | 이번 범위 제외 | file/content context 소유권이 별도 검토 대상이다. |
| `GitRepositoryAccessUseCase` | `application.port.in` | 유지 | 이번 범위 제외 | smart-http auth, filter, validator 사용처가 있다. |
| `CurrentUserPort` | `application.port.out` | 유지 | 공통 포트 | 인증 사용자 식별은 Repository Context 전용 책임이 아니다. |

### UseCase 간 의존성 해소 기준
- `RepositoryOverviewUseCase` 구현체가 다른 inbound UseCase를 직접 호출하지 않도록 한다.
- `RepositoryOverviewService`는 화면 조회 orchestration service다.
- 따라서 내부에서는 query/out port와 support collaborator를 조합한다.

```java
// 피해야 하는 구조다.
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
// 목표 구조다.
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

### 결정 로그
| # | 결정 | 이유 |
|---|---|---|
| 1 | `RepositoryOverview*` 3종은 Repository Context로 이관한다. | presentation과 application 경계를 맞춘다. |
| 2 | file/content 객체 전체 이관은 제외한다. | 사용처가 넓어 blast radius가 커진다. |
| 3 | `RepositoryOverviewService`는 inbound UseCase를 주입하지 않는다. | UseCase 간 의존 구조를 막는다. |
| 4 | `WebRepositoryController`는 `server.presentation.api.web`로 이동한다. | web internal/BFF adapter는 `WebOrganizeController`와 같은 top-level web adapter 패키지에 두는 편이 일관적이다. |
