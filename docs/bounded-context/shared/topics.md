## Shared / Cross-Cutting Topics

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념](#핵심-개념)
- [Namespace](#namespace)
- [Repository Lookup](#repository-lookup)
- [Repository Access Resolution](#repository-access-resolution)
- [Mergeability Assessment](#mergeability-assessment)
- [Pipeline Policy](#pipeline-policy)
- [분류](#분류)
- [Shared Value Object / Policy](#shared-value-object--policy)
- [Read-Side Result](#read-side-result)
- [Application-Level Resolver / Policy](#application-level-resolver--policy)
- [주요 시나리오](#주요-시나리오)
- [1. Namespace 변환](#1-namespace-변환)
- [2. Repository 경로 조회](#2-repository-경로-조회)
- [3. Git 접근 권한 계산](#3-git-접근-권한-계산)
- [4. Mergeability 계산](#4-mergeability-계산)
- [5. Push 기반 Pipeline Policy 판정](#5-push-기반-pipeline-policy-판정)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [미확정 쟁점](#미확정-쟁점)

### 문제 정의

이 문서는 여러 context에서 함께 사용하는 해석 규칙과 계산 모델을 정리한다. 현재 코드 기준 핵심 대상은 namespace, repository lookup, access resolution, mergeability, pipeline policy다.

### 책임 범위

이 문서의 범위는 다음과 같다.

- owner namespace 문자열 변환
- repository 경로 조회
- Git 접근 권한 계산
- mergeability 계산 결과 조합
- push 이후 pipeline 실행 여부 판정

이 문서는 별도 aggregate를 확정하지 않는다.

### 핵심 개념

#### Namespace

repository owner를 표현하는 문자열이다.

#### Repository Lookup

`namespace + repoName`으로 repository를 찾는 규칙이다.

#### Repository Access Resolution

repository visibility, repository member, organization member를 조합해 read/write 가능 여부를 계산하는 규칙이다.

#### Mergeability Assessment

merge preview 결과를 domain-friendly한 상태로 조합한 읽기 모델이다.

#### Pipeline Policy

push event에 대해 job 생성 가능 여부와 어떤 pipeline file을 실행할지 판정하는 애플리케이션 레벨 정책이다.

### 분류

#### Shared Value Object / Policy

현재 shared value object / policy에 가까운 대상은 다음과 같다.

- namespace
- owner -> namespace 변환 규칙
- repository path lookup 규칙

#### Read-Side Result

현재 read-side result에 가까운 대상은 다음과 같다.

- `MergeabilityAssessment`
- `MergeTopologySummary`
- `RepositoryPermission`

#### Application-Level Resolver / Policy

현재 application-level resolver / policy에 가까운 대상은 다음과 같다.

- `RepositoryNamespaceResolver`
- `RepositoryLookupService`
- `GitRepositoryAccessService`
- `MergeabilityAssessmentAssembler`
- `PushJobCreationPolicy`
- `EventPolicyResolver`

`Pipeline Policy`는 `Execution Context`가 사용하지만, 현재 구현에서는 aggregate나 entity가 아니라 application-level policy로 존재한다.

### 주요 시나리오

#### 1. Namespace 변환

현재 흐름은 다음과 같다.

1. `RepositoryNamespaceResolver.resolve(repository)`가 `ownerType`, `ownerId`를 읽는다.
2. owner type이 organization이면 organization 이름을 조회한다.
3. owner type이 user면 username을 조회한다.
4. 조회 실패 시 예외를 던진다.

#### 2. Repository 경로 조회

현재 흐름은 다음과 같다.

1. `RepositoryLookupService.findByPath(namespace, repoName)`가 clone path로 먼저 조회한다.
2. 없으면 user namespace로 조회한다.
3. organize namespace로도 조회한다.
4. 둘 다 있으면 user-owned repository를 우선한다.

#### 3. Git 접근 권한 계산

현재 흐름은 다음과 같다.

1. `GitRepositoryAccessService`가 repository를 해석한다.
2. public repository면 read를 허용한다.
3. owner면 read/write를 허용한다.
4. repository member role을 확인한다.
5. 없으면 organization member role을 확인한다.
6. 최종 `RepositoryPermission`을 계산한다.

#### 4. Mergeability 계산

현재 흐름은 다음과 같다.

1. `MergeGitPort.previewMergeability(...)`가 `MergeResult`를 반환한다.
2. `MergeabilityAssessmentAssembler`가 이를 `MergeabilityAssessment`로 변환한다.
3. status, topology, conflicts, reason을 조합한다.

#### 5. Push 기반 Pipeline Policy 판정

현재 흐름은 다음과 같다.

1. `EventPolicyResolver`가 push plan 판정을 위임한다.
2. `PushJobCreationPolicy`가 commit 기준 pipeline config를 읽는다.
3. branch와 일치하는 rule을 찾는다.
4. pipeline file 존재 여부를 확인한다.
5. 실행할 file path 또는 skip reason을 반환한다.

### 외부 시스템과의 경계

외부 경계는 다음과 같다.

- `OrganizeQueryPort`
- `UserPersistencePort`
- `RepositoryPersistencePort`
- `OrganizeMemberPersistencePort`
- `RepositoryMemberPersistencePort`
- `MergeGitPort`
- `PipelineConfigPort`
- `FileGitPort`

원칙:

- 이 문서의 개념은 aggregate보다 계산 규칙, resolver, read model에 가깝다.
- mergeability와 permission은 저장 상태보다 계산 결과다.
- namespace 규칙은 user, organization, repository path에 공통으로 사용된다.

### 다른 Context와의 연결

- `Identity & Access Context`
  - user, current user, token과 연결된다.
- `Collaboration Context`
  - organization namespace와 organization member가 접근 계산에 사용된다.
- `Repository Context`
  - repository path와 owner namespace 변환에 사용된다.
- `Change & Review Context`
  - mergeability 계산에 사용된다.
- `Execution Context`
  - pipeline policy 결과가 job 생성 여부와 `pipelineFilePath`를 결정한다.

### 미확정 쟁점

1. namespace 전역 유일성을 어디서 강제할지
   - 현재 lookup 계열과 access 계열의 충돌 처리 규칙이 다르다.
2. ambiguous namespace 처리 방식을 통일할지
   - `RepositoryLookupService`, `GitRepositoryAccessService`의 동작이 다르다.
3. `RepositoryPermission`을 별도 명시 모델로 끌어올릴지
   - 현재는 서비스 내부 계산 결과다.
