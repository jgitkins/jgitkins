# Task 2.32 Detail 5: 검증 전략과 후속 작업

### 목적
- 이 문서는 Task 2.32 구현 완료 여부를 확인하는 명령과 실패 모드를 정리한다.
- 또한 이번 범위에서 제외한 후속 작업을 명확히 남긴다.

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

TO-BE

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
- TO-BE의 `WebRepositoryController`는 `server.presentation.api.web`에 위치한다.

### 구현 순서 체크리스트
- [ ] `RepositoryOverviewResult` 파일을 `repository.application.contract.result`로 이동한다.
- [ ] `RepositoryOverviewUseCase` 파일을 `repository.application.port.in`으로 이동한다.
- [ ] `RepositoryOverviewService` 파일을 `repository.application.service`로 이동한다.
- [ ] `RepositoryOverviewService`의 inbound use case 의존성을 `RepositoryQueryPort`, `BranchQueryPort`, `FileGitPort`, `GitRepositoryAccessService`로 교체한다.
- [ ] `WebRepositoryController` 파일을 `server.presentation.api.web`로 이동한다.
- [ ] `WebRepositoryController`의 import와 반환 타입을 정리한다.
- [ ] `RepositoryManagementController`의 import를 정리한다.
- [ ] `RepositoryOverviewServiceTest`를 repository test package로 이동한다.
- [ ] `RepositoryOverviewServiceTest`의 mock 대상을 query/out port 중심으로 교체한다.
- [ ] `RepositoryManagementControllerTest`의 mock import를 정리한다.
- [ ] `ArchitecturePackageConventionTest`에서 service package 기대값을 조정한다.
- [ ] `rg`로 old package 잔여 import를 제거한다.
- [ ] `./gradlew :server:compileJava`를 실행한다.
- [ ] `./gradlew :server:test`를 실행한다.

### 컴파일 검증
```bash
./gradlew :server:compileJava
```

### 테스트 검증
```bash
./gradlew :server:test
```

### old package 잔여 import 확인
```bash
rg "io\\.jgitkins\\.server\\.application\\.(port\\.in\\.RepositoryOverviewUseCase|dto\\.result\\.RepositoryOverviewResult|service\\.RepositoryOverviewService)" server/src
```

### 목표 package 확인
```bash
rg "RepositoryOverviewUseCase|RepositoryOverviewService|RepositoryOverviewResult" \
  server/src/main/java/io/jgitkins/server/repository
```

### inbound UseCase 재의존 확인
```bash
rg "RepositoryLoadUseCase|BranchLoadUseCase|FileTreeLoadUseCase|GitRepositoryAccessUseCase" \
  server/src/main/java/io/jgitkins/server/repository/application/service/RepositoryOverviewService.java
```
- 위 명령은 결과가 없어야 한다.

### 실패 모드
| 실패 모드 | 원인 | 영향 | 방어 전략 |
|---|---|---|---|
| 컴파일 실패 | import 이동 누락 | server build 실패 | old package 잔여 import를 `rg`로 확인한다. |
| Spring bean 주입 실패 | old interface와 new interface 혼재 | controller bean 생성 실패 | old file을 삭제하고 모든 controller/test mock import를 교체한다. |
| UseCase 간 재의존 발생 | service가 inbound port를 계속 주입 | application orchestration 경계가 흐려진다. | service field 타입을 query/out port와 support collaborator로 제한한다. |
| package convention test 실패 | service 목록 미갱신 | test 실패 | `RepositoryOverviewService`를 repository service 목록으로 옮긴다. |
| API schema 변경 | record component 이름 변경 | web 화면 회귀 | record component 이름을 유지한다. |
| path overview 중복 유지 | controller가 repository lookup과 overview를 계속 조합 | 구조 개선이 절반만 완료된다. | 2차 단계에서 `getOverviewByPath`를 추가한다. |
| `RepositoryKey` null 문제 | clone path와 path 모두 파싱 실패 | overview 조회 NPE | 후속 작업으로 명시 예외 또는 path resolver를 도입한다. |

### 선택 개선안
- `getOverviewByPath(namespace, repoName, branch)`를 `RepositoryOverviewUseCase`에 추가한다.
- 이 개선은 `WebRepositoryController`의 조합 책임을 제거한다.
- 단, 1차 package 이동과 분리해 적용하는 편이 검증이 쉽다.

```java
public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);

    RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch);
}
```

### 후속 task 후보
- `[server] Repository path parsing utility 중앙화`
- `[server] File tree read model 및 FileEntry context 소유권 정리`
- `[server] GitRepositoryAccessUseCase top-level application 의존 제거`
- `[server] Repository overview path 기반 use case 계약 추가 구현`

### 완료 판정
- old `application.port.in.RepositoryOverviewUseCase` import가 없다.
- old `application.dto.result.RepositoryOverviewResult` import가 없다.
- old `application.service.RepositoryOverviewService` import가 없다.
- `RepositoryOverviewService`는 Repository Context service package에 있다.
- `RepositoryOverviewService`는 inbound UseCase를 주입하지 않는다.
- `WebRepositoryController`는 `server.presentation.api.web` package에 있다.
- `:server:compileJava`가 통과한다.
- `:server:test`가 통과한다.
