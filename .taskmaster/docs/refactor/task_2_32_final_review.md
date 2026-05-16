# Task 2.32 구현 전 최종검수 보고서

### 검수 대상
- `.taskmaster/docs/refactor/task_2_32_plan.md`
- `.taskmaster/docs/refactor/task_2_32_detail_1_boundary_and_decisions.md`
- `.taskmaster/docs/refactor/task_2_32_detail_2_contract_and_port_migration.md`
- `.taskmaster/docs/refactor/task_2_32_detail_3_service_orchestration.md`
- `.taskmaster/docs/refactor/task_2_32_detail_4_controller_and_tests.md`
- `.taskmaster/docs/refactor/task_2_32_detail_5_validation_and_followups.md`

### 검수 기준
- 구현자가 문서 순서대로 읽었을 때 목표 패키지와 이동 대상이 명확해야 한다.
- main plan과 detail 문서가 서로 다른 구현 방향을 말하지 않아야 한다.
- 코드 스니펫은 현재 코드의 record 생성자, package, port 이름과 맞아야 한다.
- `RepositoryOverviewService`는 inbound UseCase를 다시 주입하지 않는 방향으로 일관되어야 한다.
- 검증 명령은 old package 잔여, target package 생성, inbound UseCase 재의존을 잡을 수 있어야 한다.

### 문서별 검수 결과

#### 1. `task_2_32_plan.md`
- 상태는 구현 가능이다.
- 상세 문서 인덱스가 추가되어 있다.
- `RepositoryOverview*` 3종 이관, inbound UseCase 의존성 제거, controller/test 수정, 검증 전략이 한 문서에 연결되어 있다.
- `RepositoryResult.builder()` 사용을 실제 record 생성자 호출로 수정했다.
- TO-BE 스니펫의 `CurrentUserPort` 필드명을 `currentUserPort`로 정리했다.
- AS-IS 관찰 스니펫은 기존 코드의 `currentUserPersistencePort` 명칭을 유지한다.

#### 2. `task_2_32_detail_1_boundary_and_decisions.md`
- 상태는 구현 가능이다.
- Repository Overview의 소유권을 Repository Context로 보는 근거가 명확하다.
- 3가지 방안과 선택 이유가 정리되어 있다.
- `RepositoryOverviewService`가 inbound UseCase를 주입하지 않아야 한다는 기준이 별도 섹션으로 분리되어 있다.

#### 3. `task_2_32_detail_2_contract_and_port_migration.md`
- 상태는 구현 가능이다.
- `RepositoryOverviewResult`와 `RepositoryOverviewUseCase`의 이동 경로가 명확하다.
- 1차 이동과 2차 `getOverviewByPath` 계약 확장이 분리되어 있다.
- old package 잔여 import 확인 명령이 포함되어 있다.

#### 4. `task_2_32_detail_3_service_orchestration.md`
- 상태는 구현 가능이다.
- `RepositoryOverviewService`의 목표 collaborator가 `RepositoryQueryPort`, `BranchQueryPort`, `FileGitPort`, `CurrentUserPort`, `GitRepositoryAccessService`로 고정되어 있다.
- `getOverview`, `getOverviewByPath`, `buildOverview`, branch 선택 정책, repository path 해석 정책이 분리되어 있다.
- `RepositoryPathUnresolvableException`은 실제 구현 확정 항목이 아니라 null path 위험 설명용 후보로 남아 있다.

#### 5. `task_2_32_detail_4_controller_and_tests.md`
- 상태는 구현 가능이다.
- controller import와 endpoint 유지 기준이 정리되어 있다.
- service test의 mock 대상이 inbound UseCase에서 query/out port로 바뀌어 있다.
- `RepositoryResult` 테스트 스니펫은 실제 record 생성자 형태로 수정했다.
- `BranchSearchResult` 테스트 스니펫은 실제 record 생성자 형태로 수정했다.
- 테스트 import 목록에 `RepositoryResult`, `BranchSearchResult`, `RepositoryPermission`, `List`, `Optional`을 보강했다.

#### 6. `task_2_32_detail_5_validation_and_followups.md`
- 상태는 구현 가능이다.
- 데이터 흐름 AS-IS/TO-BE가 구현 방향과 일치한다.
- compile/test 명령, old package 잔여 확인, target package 확인, inbound UseCase 재의존 확인 명령이 있다.
- 후속 task 후보가 이번 범위와 분리되어 있다.

### 발견 및 수정한 문제
| # | 문제 | 수정 |
|---|---|---|
| 1 | `RepositoryResult.builder()` 예시가 실제 record 타입과 맞지 않았다. | `new RepositoryResult(...)` 생성자 호출로 수정했다. |
| 2 | `new BranchSearchResult("main", true)` 예시가 실제 record 시그니처와 맞지 않았다. | `new BranchSearchResult(1L, "main", false, true, true)`로 수정했다. |
| 3 | TO-BE 스니펫 일부에서 `CurrentUserPort` 필드명이 혼재되어 있었다. | TO-BE는 `currentUserPort`, AS-IS는 기존 명칭으로 분리했다. |
| 4 | service test import가 구현자가 바로 쓰기에는 부족했다. | 필요한 contract와 JDK import를 보강했다. |

### 잔여 리스크
- `FileGitPort`는 여전히 top-level `application.port.out`에 있다.
  - 이번 task에서는 inbound UseCase 재의존 제거를 우선하고, file/content context 소유권은 후속 task로 둔다.
- `RepositoryKey` null path 문제는 이번 task에서 정책 변경까지 강제하지 않는다.
  - 명시 예외를 도입하면 error code, handler mapping, 테스트 범위가 추가된다.
- `getOverviewByPath`는 2차 개선안이다.
  - 1차 package 이동과 같은 커밋에 넣어도 되지만, 실패 원인을 좁히려면 별도 단계가 낫다.

### 최종 판정
- 구현 착수 가능하다.
- 권장 구현 순서는 다음과 같다.
  - 1단계: `RepositoryOverviewResult`, `RepositoryOverviewUseCase`, `RepositoryOverviewService` 파일 이동
  - 2단계: `RepositoryOverviewService` inbound UseCase 의존성 제거
  - 3단계: controller import와 반환 타입 정리
  - 4단계: service/controller/package convention test 수정
  - 5단계: `compileJava`, `test`, `rg` 검증 수행
  - 6단계: 선택적으로 `getOverviewByPath` 계약 추가
