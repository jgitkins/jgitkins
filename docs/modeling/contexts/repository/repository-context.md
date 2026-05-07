## Repository Context

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념과 유비쿼터스 언어](#핵심-개념과-유비쿼터스-언어)
- [Repository](#repository)
- [Owner](#owner)
- [Repository Initialization](#repository-initialization)
- [Branch](#branch)
- [Repository Membership](#repository-membership)
- [Aggregate / Entity / Value Object 경계](#aggregate--entity--value-object-경계)
- [Aggregate Root: Repository](#aggregate-root-repository)
- [Entity Candidate: Branch](#entity-candidate-branch)
- [Relation Model: Repository Member](#relation-model-repository-member)
- [주요 Value Objects](#주요-value-objects)
- [Repository State](#repository-state)
- [불변식](#불변식)
- [주요 시나리오](#주요-시나리오)
- [1. Repository 생성](#1-repository-생성)
- [2. Branch 생성](#2-branch-생성)
- [3. Repository Membership 관리](#3-repository-membership-관리)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [설계 결정과 후속 고려사항](#설계-결정과-후속-고려사항)

### 문제 정의

`Repository Context`는 저장소 작업 공간의 경계를 정의한다. 사용자는 owner 아래에 저장소를 만들고, 기본 브랜치를 기준으로 파일과 커밋을 다루며, 저장소 단위 멤버십을 부여한다. Pull Request와 CI도 이 저장소를 기준으로 연결된다.

이 문서의 목적은 다음 질문에 답하는 것이다.

- Repository는 어떤 상태를 직접 소유하는가
- Branch는 Repository 내부 Entity인가, 외부 Git 상태의 projection인가
- Repository Member는 Aggregate 내부 Entity인가, 별도 관계 모델인가
- Git 저장소 초기화와 첫 commit 반영은 어느 책임 경계에서 처리되는가

### 책임 범위

이 context의 책임은 다음과 같다.

- 저장소 생성, 조회, 삭제
- 저장소 owner와 경로 식별
- 기본 브랜치와 초기화 상태 관리
- 저장소 단위 멤버십 관리
- 저장소 초기 provisioning과 bare repository 생성

직접 소유하지 않는 책임은 다음과 같다.

- Pull Request 상태 전이
- mergeability 계산
- CI 정책 해석
- Job/Runner 실행
- 파일 트리, 파일 내용, commit 자체의 영속 소유

### 핵심 개념과 유비쿼터스 언어

#### Repository

사용자 또는 조직이 소유하는 Git 저장소와 그 메타데이터를 함께 다루는 Aggregate Root다.

#### Owner

Repository 소유 주체다. 현재는 `User` 또는 `Organization`이며 `OwnerType + OwnerId`로 표현한다.

#### Repository Initialization

저장소 생성 후 bare repository 준비, 초기 commit, HEAD 갱신까지 포함한 초기화 과정이다.

#### Branch

Repository 안의 변경 흐름을 식별하는 브랜치 메타데이터다.

#### Repository Membership

사용자와 Repository 사이의 저장소 단위 권한 관계다.

관련 구조와 흐름 다이어그램은 [Repository Context Diagrams](./repository-context-diagrams.md) 문서를 참고한다.

### Aggregate / Entity / Value Object 경계

#### Aggregate Root: Repository

`Repository`는 이 context의 root다.

- 코드 근거: `server/domain/aggregate/Repository.java`
- 유스케이스 근거: `RepositoryCreateUseCase`, `RepositoryLoadUseCase`, `RepositoryDeleteUseCase`
- 서비스 근거: `RepositoryManagementService`, `RepositoryLoadService`

Repository가 직접 소유하거나 결정하는 값은 다음과 같다.

- `RepositoryId`
- `OwnerType`
- `OwnerId`
- `RepositoryName`
- `RepositoryPath`
- `BranchName` as default branch
- `RepositoryVisibility`
- `clonePath`
- `credentialId`
- `requiresInitialContent`
- `initialized`

Repository는 저장소 메타데이터와 초기화 상태를 소유한다. Git object graph는 소유하지 않는다.

#### Entity Candidate: Branch

`Branch`는 Repository에 종속된 Entity로 본다.

- 코드 근거: `server/repository/domain/entity/Branch.java`
- 식별 근거: `repositoryId + branch name`
- 규칙 근거: 기본 브랜치 삭제 금지, 중복 브랜치명 금지

Branch head는 Git에 있고 DB에는 메타데이터만 있다. 따라서 Branch는 내부 Entity이지만 실제 Git 상태와 동기화된다.

- Branch는 `Repository Context` 안에서 다룬다.
- Branch는 독립 Aggregate로 승격하지 않는다.
- Branch head 자체는 Repository Aggregate 내부 값으로 강하게 포함하지 않는다.

#### Relation Model: Repository Member

`Repository Member`는 Repository 내부 Entity보다 `Repository`와 `User` 사이의 관계 모델에 가깝다.

- 코드 근거: `server/domain/model/RepositoryMember.java`
- 서비스 근거: `RepositoryMemberService`
- 특징: `repositoryId + userId + role + addedAt` 중심으로 관리되고, Repository aggregate를 직접 로드하지 않고 추가/삭제된다.

- Repository Member는 `Repository Context` 안에서 다룬다.
- Repository 내부 lifecycle entity가 아니라 권한 관계 모델로 본다.
- 후속 권한 체계 통합 시 `Organization Member`와 공통 `Membership` 추상화 후보가 된다.

권장 패키지 예시는 다음과 같다.

```text
server/domain/repository/
  aggregate/
    Repository.java
  model/
    RepositoryMember.java
  vo/
    RepositoryId.java
    RepositoryMemberRole.java
```

이 구조는 `RepositoryMember`를 Context 안에 두되 `Repository`와 같은 root로 올리지 않는다는 뜻이다.

#### 주요 Value Objects

이 context의 주요 Value Object는 다음과 같다.

- `RepositoryId`
- `RepositoryName`
- `RepositoryPath`
- `BranchName`
- `OwnerType`
- `OwnerId`
- `RepositoryVisibility`

#### Repository State

초기화 단계는 boolean보다 상태 모델로 해석하는 편이 명확하다.

현재 문서에서 권장하는 상태는 다음 3개다.

- `PROVISIONED`
  - bare repository는 생성되었지만, 아직 초기 commit은 반영되지 않은 상태
- `INITIALIZED`
  - 초기 commit과 HEAD 갱신까지 끝나서 실제 사용 가능한 active 상태
- `DELETED`
  - 삭제가 완료된 상태

현재 구현은 `initialized` boolean 중심이므로, 문서에서는 상태 모델을 기준으로 설명하고 구현은 후속 리팩터링에서 `RepositoryState` enum으로 맞춘다.

### 불변식

현재 기준 불변식은 다음과 같다.

1. 같은 owner 범위 안에서 repository name은 중복될 수 없다.
2. Repository 생성 시 owner, name, default branch, visibility는 반드시 정해져야 한다.
3. bare repository만 생성되고 초기 commit이 반영되지 않은 상태에서는 `initialized=false`다.
4. `initialized=false`인 저장소에서는 새 브랜치를 만들 수 없다.
5. 기본 브랜치는 삭제할 수 없다.
6. 같은 Repository 안에서 branch name은 중복될 수 없다.
7. Repository Member는 같은 `repositoryId + userId` 조합으로 중복 생성하지 않는다.

### 주요 시나리오

#### 1. Repository 생성

`RepositoryManagementService` 흐름은 다음과 같다.

1. owner와 repository name을 검증한다.
2. namespace를 계산한다.
3. `Repository.create(...)`로 Aggregate를 만든다.
4. persistence에 저장한다.
5. `RepositoryProvisioner`를 호출해 bare Git repository를 초기화한다.
6. 기본 브랜치 row를 만든다.
7. 초기 commit 옵션이 있으면 README 등 초기 파일을 commit하고 HEAD를 갱신한다.
8. 초기 commit까지 성공하면 `markInit()`으로 저장소를 initialized 상태로 바꾼다.

상태 모델로 보면 다음과 같다.

1. bare repository 생성 완료: `PROVISIONED`
2. 초기 commit 및 HEAD 갱신 완료: `INITIALIZED`
3. 저장소 삭제 완료: `DELETED`

#### 2. Branch 생성

`BranchManagementService` 흐름은 다음과 같다.

1. Repository를 로드한다.
2. 저장소가 initialized 상태인지 확인한다.
3. 같은 이름의 branch가 이미 있는지 확인한다.
4. source branch를 결정한다.
5. DB에 branch 메타데이터를 저장한다.
6. Git adapter를 통해 실제 branch를 만든다.

#### 3. Repository Membership 관리

`RepositoryMemberService` 흐름은 다음과 같다.

1. 입력값을 검증한다.
2. 이미 member인지 확인한다.
3. 기본 role은 `READER`다.
4. persistence에 관계 row를 저장하거나 삭제한다.

### 외부 시스템과의 경계

이 context는 Git과 연결되지만 Git 자체는 소유하지 않는다.

외부 경계는 다음과 같다.

- `RepositoryGitPort`
  - bare repository 생성/삭제
- `CommitGitPort`
  - 초기 commit 반영
- `BranchGitPort`
  - branch 생성/삭제
- `RepositoryNamespaceResolver`
  - owner를 namespace로 해석

원칙:

- Repository Aggregate는 Git object graph를 직접 담지 않는다.
- Git 반영은 adapter/port를 통해 수행한다.
- Git tree, file content, commit은 입력/출력 또는 외부 개념으로 다룬다.

### 다른 Context와의 연결

- `Change & Review Context`
  - Pull Request는 특정 Repository와 Branch를 기준으로 열린다.
- `Execution Context`
  - Job은 특정 Repository / Branch / Commit을 실행 대상으로 가진다.
- `Identity & Access Context`
  - Repository owner가 User일 수 있고, User credential이 clone/push/pull 경계와 연결된다.
- `Collaboration Context`
  - Repository owner가 Organization일 수 있고, 조직 멤버십이 저장소 접근 모델에 영향을 준다.

### 설계 결정과 후속 고려사항

이 섹션은 현재 채택한 설계와 후속 고려사항을 정리한다.

1. Branch를 Repository 내부 Entity로 확정하고, Git 상태 read model 성격은 보조 해석으로 둔다.
   - branch는 단독 의미가 없고 식별도 `repositoryId + branchName`에 의존한다.
   - 기본 브랜치 삭제 금지 같은 규칙도 Repository 문맥에서만 해석된다.
   - branch head와 commit graph는 Git 외부 상태다.

2. Repository Member는 Repository 내부 Entity로 올리지 않고, 별도 관계 모델로 유지한다.
   - 현재 코드는 `RepositoryMemberService`가 Repository를 로드하지 않고 멤버십 row를 독립 관리한다.
   - 따라서 멤버십은 aggregate 내부 상태보다 권한 관계 모델로 보는 편이 구현과 맞다.

3. Namespace 충돌 정책은 Repository Context 내부 규칙으로 두지 않고, Shared / Cross-Cutting 규칙으로 분리한다.
   - namespace는 repository만의 문제가 아니라 `User`, `Organization`, URL 경로, clone path, lookup 정책을 함께 관통한다.
   - 이 문서는 namespace 사용까지만 다루고, 선점 규칙은 별도 문서에서 정의한다.

4. Repository initialization은 목표 모델로 상태 머신을 채택하고, 현재 구현은 boolean 기반으로 유지한다.
   - 목표 상태는 `PROVISIONED -> INITIALIZED -> DELETED`다.
   - `requiresInitialContent`는 초기화 전략을 설명하는 보조 값으로 둔다.
   - 현재 구현은 boolean을 유지하고 목표 모델만 상태 머신으로 둔다.

5. owner가 Organization일 때의 멤버십 상속 규칙은 Repository Context 본문에 깊게 넣지 않고, 권한 문서로 분리한다.
   - 이 문서는 저장소 단위 멤버십 존재와 Organization ownership 영향까지만 다룬다.
   - 상속, override, 충돌 해소 규칙은 권한 문서에서 정의한다.

### 현재 문서 결정

- `Repository`는 Aggregate Root로 본다.
- `Branch`는 Repository에 종속된 내부 Entity로 본다. 단, branch head와 commit graph는 외부 Git 상태다.
- `Repository Member`는 Repository Context 안의 관계 모델로 본다.
- Repository 상태는 장기적으로 `RepositoryState(PROVISIONED, INITIALIZED, DELETED)`로 표현하는 방향을 권장한다.
- Git object graph는 외부 시스템 경계로 둔다.
