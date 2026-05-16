## Change & Review Context

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념과 유비쿼터스 언어](#핵심-개념과-유비쿼터스-언어)
- [Pull Request](#pull-request)
- [Branch Head Snapshot](#branch-head-snapshot)
- [Target Drift](#target-drift)
- [Mergeability Assessment](#mergeability-assessment)
- [Merge](#merge)
- [Aggregate / Entity / Value Object 경계](#aggregate--entity--value-object-경계)
- [Aggregate Root: Pull Request](#aggregate-root-pull-request)
- [Embedded Value Model: Branch Head Snapshot](#embedded-value-model-branch-head-snapshot)
- [Embedded Value Model: Target Drift](#embedded-value-model-target-drift)
- [Read-Side / Domain Service Result: Mergeability Assessment](#read-side--domain-service-result-mergeability-assessment)
- [주요 Value Objects](#주요-value-objects)
- [Pull Request State](#pull-request-state)
- [불변식](#불변식)
- [영속 데이터와 조회 데이터](#영속-데이터와-조회-데이터)
- [주요 시나리오](#주요-시나리오)
- [1. Pull Request 생성](#1-pull-request-생성)
- [2. Pull Request 상세 조회](#2-pull-request-상세-조회)
- [3. Mergeability 확인](#3-mergeability-확인)
- [4. Merge 수행](#4-merge-수행)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [미확정 쟁점](#미확정-쟁점)

### 문제 정의

`Change & Review Context`는 `Repository` 위의 변경 흐름을 검토와 병합 단위로 다루는 경계다. 현재 중심은 `PullRequest`이며 source/target snapshot, target drift, mergeability를 함께 해석한다.

이 문서의 목적은 다음 질문에 답하는 것이다.

- Pull Request는 어떤 상태를 직접 소유하는가
- source/target branch의 최신 HEAD와 PR 생성 시점 snapshot은 어떻게 구분되는가
- mergeability는 Aggregate 내부 영속 상태인가, 조회 시점 계산 결과인가
- target drift와 실제 merge 수행은 어느 책임 경계에서 처리되는가

### 책임 범위

이 context의 책임은 다음과 같다.

- Pull Request 생성, 조회, 상태 전이
- source / target branch snapshot 보존
- target branch drift 감지
- mergeability 평가 결과 조합
- 실제 merge command와 PR 상태 변경 사이의 연결 규칙 정의

직접 소유하지 않는 책임은 다음과 같다.

- Repository 생성/삭제와 branch lifecycle 자체의 영속 소유
- commit object, tree, blob 같은 Git object graph 소유
- CI job 실행과 runner orchestration
- 사용자/조직 권한 정책의 최종 결정
- 코드 리뷰 comment, approval rule, reviewer assignment의 상세 정책

### 핵심 개념과 유비쿼터스 언어

#### Pull Request

특정 Repository 안에서 source branch 변경을 target branch로 합치기 위한 검토 요청이다. 이 context의 Aggregate Root다.

#### Branch Head Snapshot

특정 branch 이름과 그 시점 HEAD commit hash를 함께 보존한 값이다.

#### Target Drift

PR 생성 시점 target HEAD와 현재 target HEAD 차이를 나타내는 값이다. 현재 구현에서는 조회 시 계산한다.

#### Mergeability Assessment

source branch와 target branch의 병합 가능성, 충돌 여부, fast-forward 가능성을 나타내는 계산 결과다.

#### Merge

source branch를 target branch로 실제 반영하는 command다. 현재는 `MergeService`가 수행한다.

관련 구조와 흐름 다이어그램은 [Change & Review Context Diagrams](./diagrams.md) 문서를 참고한다.

### Aggregate / Entity / Value Object 경계

#### Aggregate Root: Pull Request

`PullRequest`는 이 context의 root다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/pr/aggregate/PullRequest.java`
- 유스케이스 근거: `CreatePullRequestUseCase`, `GetPullRequestDetailUseCase`
- 서비스 근거: `PullRequestService`

Pull Request가 직접 소유하거나 결정하는 값은 다음과 같다.

- `PullRequestId`
- `RepositoryId`
- `BranchHeadSnapshot source`
- `BranchHeadSnapshot target`
- `PullRequestStatus`
- `TargetDrift`
- `lastAssessmentSnapshot`
- `createdAt`
- `updatedAt`

Pull Request는 identity, branch snapshot, 상태 전이를 소유한다. 최신 branch HEAD, mergeability, 충돌 파일 목록은 외부 Git 상태와 조회 시점 계산에 가깝다.

#### Embedded Value Model: Branch Head Snapshot

`BranchHeadSnapshot`은 Pull Request 내부 값 모델로 본다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/pr/model/BranchHeadSnapshot.java`
- 구성: `BranchName + CommitHash`
- 규칙 근거: branch 이름과 commit hash가 모두 있어야 snapshot이 성립한다.

독립 생명주기는 없다.

#### Embedded Value Model: Target Drift

`TargetDrift`는 Pull Request에 종속된 값 모델이다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/pr/model/TargetDrift.java`
- 구성: `drifted`, `previousTargetHead`, `currentTargetHead`
- 규칙 근거: drifted=true이면 이전/현재 target head가 모두 있어야 하고 서로 달라야 한다.

- `TargetDrift`는 Pull Request 내부 값으로 다룬다.
- drift는 조회 시점 관찰 결과지만 aggregate 내부 값으로 표현한다.
- 현재 서비스는 상세 조회 시 이를 계산하지만 저장하지 않는다.

#### Read-Side / Domain Service Result: Mergeability Assessment

`MergeabilityAssessment`는 Pull Request 내부 엔티티보다 도메인 서비스 계산 결과에 가깝다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/changegraph/MergeabilityAssessment.java`
- 서비스 근거: `PullRequestMergeabilityResolver`, `MergeService`, `MergeabilityAssessmentAssembler`
- 테스트 근거: `PullRequestServiceTest`
- `MergeabilityAssessment`는 `Change & Review Context` 안에서 다룬다.
- Pull Request가 항상 영속적으로 소유하는 내부 엔티티로 보지 않는다.
- 현재는 read-side 계산 결과 또는 domain service result로 기술한다.

권장 패키지 예시는 다음과 같다.

```text
server/domain/pr/
  aggregate/
    PullRequest.java
  model/
    BranchHeadSnapshot.java
    TargetDrift.java
    PullRequestStatus.java
  vo/
    PullRequestId.java

server/domain/model/changegraph/
  MergeabilityAssessment.java
  MergeabilityStatus.java
  MergeTopologySummary.java
```

이 구조는 Pull Request를 root로 두고 mergeability를 변경 그래프 해석 결과로 분리한다.

#### 주요 Value Objects

이 context의 주요 Value Object는 다음과 같다.

- `PullRequestId`
- `BranchName`
- `CommitHash`
- `RepositoryId`
- `PullRequestStatus`

#### Pull Request State

Pull Request 상태는 다음 3개다.

- `OPEN`
- `CLOSED`
- `MERGED`

- `OPEN` 상태에서만 source 갱신, assessment 기록, target drift 반영, close, merge를 수행할 수 있다.
- `CLOSED` 상태에서만 reopen이 가능하다.
- `MERGED`는 terminal state다.

현재 서비스는 `close`, `reopen`, `markMerged`, `updateSource`, `recordAssessmentSnapshot`를 모두 외부 API로 노출하지는 않는다.

### 불변식

현재 기준 불변식은 다음과 같다.

1. Pull Request 생성 시 `repositoryId`, `source`, `target`은 반드시 있어야 한다.
2. source branch와 target branch는 같을 수 없다.
3. `BranchHeadSnapshot`은 branch name과 commit hash가 모두 있어야 성립한다.
4. `TargetDrift`가 감지된 상태라면 이전 target head와 현재 target head는 반드시 존재해야 하며 서로 달라야 한다.
5. `OPEN` 상태에서만 source 변경, target drift 반영, mergeability snapshot 기록, close, merge 완료 표시가 가능하다.
6. `CLOSED` 상태에서만 reopen이 가능하다.
7. mergeability는 기본적으로 조회 시점 계산 결과이며, 현재 구현 기준 상세 조회만으로는 DB에 다시 저장되지 않는다.

### 영속 데이터와 조회 데이터

이 문서에서 구분하는 기준은 다음과 같다.

- 영속 데이터
  - `PullRequestRepository`를 통해 저장되는 값
  - PR 생성 시점 snapshot과 상태 전이에 속하는 값
- 조회 시 재조회/재계산 데이터
  - branch HEAD, target drift, mergeability처럼 Git 상태를 다시 읽거나 계산해 만드는 값

현재 기준 영속 데이터는 다음과 같다.

- `PullRequestId`
- `RepositoryId`
- `stored source snapshot`
  - source branch name
  - source head commit hash
- `stored target snapshot`
  - target branch name
  - target head commit hash
- `PullRequestStatus`
- `createdAt`
- `updatedAt`
- `TargetDrift`
  - 도메인 모델에는 포함되지만 현재 구현에서는 상세 조회 시 계산하고 저장하지 않는다
- `lastAssessmentSnapshot`
  - 도메인 모델에는 포함되지만 현재 구현에서는 상세 조회 시 저장하지 않는다

현재 기준 조회 시 재조회/재계산 데이터는 다음과 같다.

- `current source head`
- `current target head`
- `TargetDrift`
  - stored target head와 current target head 비교 결과
- `MergeabilityAssessment`
- 충돌 파일 목록
- fast-forward 가능 여부
- merge commit 필요 여부

### 주요 시나리오

#### 1. Pull Request 생성

`PullRequestService` 흐름은 다음과 같다.

1. `namespace + repoName`으로 Repository를 조회한다.
2. Repository에서 실제 namespace와 repo path를 해석한다.
3. source branch와 target branch의 현재 HEAD commit hash를 Git port로 읽는다.
4. `BranchHeadSnapshot` 두 개를 만든다.
5. `PullRequest.create(...)`로 Aggregate를 만든다.
6. persistence에 저장한다.
7. 결과 DTO로 변환해 반환한다.

영속화 데이터:

- `repositoryId`
- source branch name
- source head commit hash
- target branch name
- target head commit hash
- `status=OPEN`
- `createdAt`
- `updatedAt`

조회 시 재조회/재계산 데이터:

- 없음
- 생성 시 읽은 source/target HEAD는 조회용 계산값이 아니라 저장용 snapshot이 된다.

#### 2. Pull Request 상세 조회

`PullRequestService#getPullRequestDetail(...)` 흐름은 다음과 같다.

1. Pull Request를 로드한다.
2. `repositoryId`로 Repository를 다시 로드한다.
3. 현재 source branch HEAD를 계산한다.
4. 현재 target branch HEAD를 계산한다.
5. 현재 target HEAD를 바탕으로 target drift를 계산한다.
6. 현재 기준 mergeability를 재평가한다.
7. 저장된 snapshot과 현재 snapshot, drift, assessment를 함께 detail result로 반환한다.

- 상세 조회는 read-side observation 성격이 강하다.
- target drift와 mergeability는 계산하지만 persistence에 다시 저장하지 않는다.

영속화 데이터:

- 없음
- 현재 구현 기준 상세 조회만으로는 PR을 다시 저장하지 않는다.

조회 시 재조회/재계산 데이터:

- current source head
- current target head
- `TargetDrift`
- `MergeabilityAssessment`
- 충돌 파일 목록
- fast-forward 가능 여부
- merge commit 필요 여부

#### 3. Mergeability 확인

`MergeService`와 `PullRequestMergeabilityResolver` 흐름은 다음과 같다.

1. source/target branch를 기준으로 Git adapter에 preview 요청을 보낸다.
2. adapter의 `MergeResult`를 받아온다.
3. 이를 `MergeabilityAssessment`로 조합한다.
4. 충돌 여부, 공통 조상 존재 여부, fast-forward 가능성, merge commit 필요 여부를 해석한다.

영속화 데이터:

- 없음

조회 시 재조회/재계산 데이터:

- `MergeabilityAssessment`
- 충돌 파일 목록
- fast-forward 가능 여부
- merge commit 필요 여부

#### 4. Merge 수행

`MergeService` 흐름은 다음과 같다.

1. `namespace`, `repoName`, `sourceBranch`, `targetBranch`를 입력으로 받는다.
2. `MergeGitPort`를 통해 실제 Git merge를 수행한다.
3. `MergeResult`를 반환한다.

현재 구현은 merge command와 PR 상태 전이를 완전히 통합하지 않는다. 후속 작업에서는 merge 성공 시 `PullRequest.markMerged()`와 영속 반영을 연결해야 한다.

영속화 데이터:

- 현재 구현 기준 없음
- 후속 통합 유스케이스에서는 `PullRequestStatus=MERGED`, `updatedAt` 저장이 필요하다.

조회 시 재조회/재계산 데이터:

- `MergeResult`
- new commit id
- result tree id
- 충돌 파일 목록

### 외부 시스템과의 경계

이 context는 Git 상태 해석과 병합 계산에 연결되지만 Git object graph는 소유하지 않는다.

외부 경계는 다음과 같다.

- `BranchGitPort`
  - branch HEAD commit 조회
- `MergeGitPort`
  - mergeability preview와 실제 merge 수행
- `RepositoryLookupService`
  - `namespace + repoName`으로 repository 식별
- `RepositoryNamespaceResolver`
  - repository를 실제 namespace 문자열로 해석
- `PullRequestRepository`
  - Pull Request aggregate 영속화

원칙:

- Pull Request aggregate는 Git branch의 최신 HEAD를 직접 보유하지 않는다.
- branch의 현재 상태는 조회 시점에 port를 통해 확인한다.
- mergeability와 conflict 목록은 외부 Git 계산 결과를 조합한 값으로 다룬다.
- commit graph, merge algorithm, conflict path 계산은 외부 시스템 또는 adapter 책임이다.

### 다른 Context와의 연결

- `Repository Context`
  - Pull Request는 특정 `Repository`와 그 안의 `Branch`를 기준으로 열린다.
- `Execution Context`
  - Pull Request readiness는 Job, CI 결과, branch 상태와 결합될 수 있다.
- `Identity & Access Context`
  - 누가 PR을 생성, 조회, merge할 수 있는지는 사용자/credential/권한 모델과 연결된다.
- `Collaboration Context`
  - Organization ownership과 팀 구조는 reviewer 권한, merge 권한, 기본 정책에 영향을 준다.
- `Shared / Cross-Cutting Topics`
  - mergeability, readiness, review policy는 여러 context 결과를 조합하는 cross-cutting 해석 모델로 발전할 수 있다.

### 미확정 쟁점

1. mergeability snapshot을 Pull Request에 실제로 저장할지, 계속 조회 시점 계산만 유지할지
   - 현재 구현은 조회 시점 재계산 모델에 가깝다.
   - 감사 로그나 마지막 평가 시점이 필요하면 `lastAssessmentSnapshot` 저장을 추가할 수 있다.
2. merge 성공 이후 Pull Request 상태를 자동으로 `MERGED`로 전이할지
   - 도메인 일관성상 자동 전이가 맞다.
   - 현재 구현은 merge command와 PR 상태 전이가 분리되어 있다.
3. review comment, approval, reviewer assignment를 이 context의 내부 모델로 포함할지
   - 현재 문서는 PR과 mergeability까지만 다룬다.
   - review collaboration 모델은 후속 문서에서 확장한다.
4. source drift도 target drift처럼 1급 모델로 둘지
   - 현재는 current source와 stored source를 함께 반환하지만 drift 모델은 target에만 있다.
