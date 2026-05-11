# Task 2.30 리팩토링 계획

## 목표

`repository.infrastructure.adapter.git`가 `repository.application.exception`을 직접 참조하지 않도록 정리한다.

현재 `BranchGitAdapter`, `RepositoryGitCommitAdapter`는 Git 상태를 확인한 뒤 `BranchAlreadyExistsException`, `BranchNotFoundException`, `SourceBranchNotFoundException`, `CommitNotFoundException` 같은 application exception을 직접 던진다. Adapter가 사용자/API 의미를 알게 되므로 포트-어댑터 경계가 흐려진다.

목표 구조는 다음과 같다.

```text
JGit / filesystem failure
        |
        v
Git adapter
        |
        |-- semantic Git state failure -> Git port contract exception
        |-- technical failure           -> InfrastructureException
        v
Application service / factory
        |
        v
ApplicationException으로 번역
```

## 선택 방안

채택안은 **PortException 기반 번역**이다.

검토한 대안:

- `null`, `false`, `Optional.empty()`로 실패 표현: 기술 실패와 의미적 실패가 섞이고 분기 누락 위험이 크다.
- `InfrastructureException`을 application에서 catch: application 계층이 infrastructure 패키지를 알게 되어 기존 아키텍처 테스트와 충돌한다.
- sealed result/error carrier 반환: 가장 명시적이지만 이번 작업 범위 대비 변경량이 크다.

따라서 이번 작업에서는 Git outbound port가 소유하는 계약 예외를 추가한다. 이 예외는 application 패키지 안에 위치하지만 `ApplicationException`은 아니다. 사용자 응답용 예외가 아니라 adapter와 application 사이의 내부 계약 신호다.

## 예외 분류

```text
JgitkinsException
├── ApplicationException      // 사용자/API 의미. 400/404/409 등으로 응답 가능
├── DomainException           // 도메인 규칙 위반
└── InfrastructureException   // 기술 실패. 기본 500

RuntimeException
└── GitPortException          // outbound Git port 계약 실패. presentation까지 올라가면 안 됨
    ├── GitSourceBranchRefMissingException
    ├── GitBranchRefAlreadyExistsException
    ├── GitBranchRefMissingException
    └── GitCommitObjectMissingException
```

권장 패키지:

```text
server/src/main/java/io/jgitkins/server/repository/application/port/out/exception/
```

분류 기준:

- `GitPortException`: 외부 Git 저장소의 의미적 상태. 예: ref 없음, commit object 없음, 이미 같은 ref 존재.
- `InfrastructureException`: JGit API 실패, IOException, 파일시스템 접근 실패, ref update 실패 같은 기술 실패.
- `ApplicationException`: 최종 사용자/API 의미. 예: branch not found, source branch not found, commit not found.
- JGit 예외라도 `RefNotFoundException`처럼 ref/object missing을 의미하는 경우는 기술 실패로 500 처리하지 않고 가능한 한 Git port contract exception으로 변환한다.

중요 규칙:

- `GitPortException`은 `JgitkinsException`을 상속하지 않는다.
- `GitPortException`은 `ApplicationException`도 `InfrastructureException`도 아니다.
- `GitPortException`은 service/factory에서 반드시 application exception으로 번역한다.
- 번역 누락으로 `GitPortException`이 presentation까지 전파되면 버그다.

## 변경 범위

주요 변경 파일:

- `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git/BranchGitAdapter.java`
- `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git/RepositoryGitCommitAdapter.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/BranchGitPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/port/out/CommitGitPort.java`
- `server/src/main/java/io/jgitkins/server/repository/application/support/branch/BranchFactory.java`
- `server/src/main/java/io/jgitkins/server/repository/application/service/BranchManagementService.java`
- `server/src/main/java/io/jgitkins/server/application/service/CommitService.java`
- `server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestMergeabilityResolver.java`
- 관련 application / infrastructure 테스트

신규 추가 후보:

- `GitPortException`
- `GitSourceBranchRefMissingException`
- `GitBranchRefAlreadyExistsException`
- `GitBranchRefMissingException`
- `GitCommitObjectMissingException`
- 필요 시 `GitBranchHeadResolveFailedException`

## 책임 재배치

### Adapter

- `BranchGitAdapter`
  - source ref 없음 -> `GitSourceBranchRefMissingException`
  - target branch ref 이미 존재 -> `GitBranchRefAlreadyExistsException`
  - delete/head 대상 ref 없음 -> `GitBranchRefMissingException`
  - `RefNotFoundException`처럼 ref missing 의미가 명확한 JGit 예외 -> 해당 Git port contract exception
  - JGit/IO 실패 -> 기존 `BranchCreateFailedException`, `BranchDeleteFailedException` 또는 head 조회 전용 infrastructure exception

- `RepositoryGitCommitAdapter`
  - commit object 없음 -> `GitCommitObjectMissingException`
  - commit load 중 IO 실패 -> `CommitLoadFailedException`

### Application

- `BranchFactory`
  - `GitSourceBranchRefMissingException` -> `SourceBranchNotFoundException`
  - `GitBranchRefAlreadyExistsException` -> `BranchAlreadyExistsException`

- `BranchManagementService`
  - delete 중 `GitBranchRefMissingException` -> `BranchNotFoundException`

- `CommitService`
  - `GitCommitObjectMissingException` -> `CommitNotFoundException`

- `PullRequestService`, `PullRequestMergeabilityResolver`
  - `getHeadCommitHash`에서 `GitBranchRefMissingException`이 발생하면 새 PR 전용 예외를 만들지 않고 `BranchNotFoundException(branchName)`으로 번역한다.

## 구현 순서

1. `repository.application.port.out.exception` 패키지에 Git port contract exception을 추가한다.
2. `BranchGitPort.getHeadCommitHash(...) throws IOException`을 제거한다.
3. `BranchGitAdapter`, `RepositoryGitCommitAdapter`에서 `repository.application.exception` import를 제거한다.
4. Adapter의 semantic Git 상태 실패를 Git port contract exception으로 교체한다.
5. Application service/factory에서 Git port contract exception을 기존 application exception으로 번역한다.
6. PR 관련 head lookup 호출부에서 checked `IOException` 전파를 제거하고 새 계약 기준으로 정리한다.
7. 테스트를 adapter contract 테스트와 application translation 테스트로 분리해 보강한다.
8. 아키텍처 테스트에 adapter가 `repository.application.exception`을 import하지 않는 규칙을 추가한다.

## 테스트 계획

Adapter 테스트:

- source branch ref 없음 -> `GitSourceBranchRefMissingException`
- duplicate branch ref -> `GitBranchRefAlreadyExistsException`
- delete 대상 branch ref 없음 -> `GitBranchRefMissingException`
- head 조회 대상 branch ref 없음 -> `GitBranchRefMissingException`
- commit object 없음 -> `GitCommitObjectMissingException`
- JGit/IO 실패 -> 기존 infrastructure exception 유지

Application translation 테스트:

- `BranchFactory`가 `GitSourceBranchRefMissingException`을 `SourceBranchNotFoundException`으로 번역한다.
- `BranchFactory`가 `GitBranchRefAlreadyExistsException`을 `BranchAlreadyExistsException`으로 번역한다.
- `BranchManagementService`가 delete 중 `GitBranchRefMissingException`을 `BranchNotFoundException`으로 번역한다.
- `CommitService`가 `GitCommitObjectMissingException`을 `CommitNotFoundException`으로 번역한다.
- PR head lookup 경로에서 missing branch가 `GitPortException`으로 노출되지 않는다.

Architecture regression 테스트:

- `server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git` 아래 소스는 `io.jgitkins.server.repository.application.exception`을 import하지 않는다.
- 기존 규칙대로 application 계층은 `io.jgitkins.server.infrastructure.*`를 import하지 않는다.

권장 검증 명령:

```bash
./gradlew test
```

또는 서버 모듈 단독 검증이 필요하면:

```bash
./gradlew :server:test
```

## 완료 기준

- Git adapter에서 application exception import가 제거된다.
- Application 계층에서 infrastructure exception import를 추가하지 않는다.
- `BranchGitPort.getHeadCommitHash`의 checked `IOException` 계약이 제거된다.
- branch 생성/삭제, commit 조회, PR head lookup의 기존 API 의미가 유지된다.
- PortException은 presentation까지 노출되지 않는다.
- 관련 adapter/application/architecture 테스트가 통과한다.

## 범위 밖

- 포트 반환 타입을 sealed result로 바꾸는 구조 개편.
- 전체 application exception 체계 재설계.
- 모든 infrastructure exception의 세분화.
- merge 관련 Git port의 checked `IOException` 정리. 별도 작업으로 다룬다.
