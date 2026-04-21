# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.18 Shared / Cross-Cutting Topics 리팩토링 계획서

### 참조 문서
- `docs/modeling/contexts/shared-cross-cutting/shared-cross-cutting-topics.md`

### 목적
- Shared / Cross-Cutting Topics에 정의된 개념을 코드에 일관된 형태로 반영한다.
- 한 번에 전부 바꾸지 않고, 개념 하나씩 순차적으로 정리한다.
- 각 개념마다 책임, 경계, 호출부, 반환 모델을 명확히 한다.

### 작업 원칙
- 이번 계획은 구현 순서를 명시한다.
- 개념 하나가 끝나기 전에는 다음 개념 구현에 들어가지 않는다.
- 각 단계는 기존 동작 보존이 우선이다.
- 이름 변경만으로 끝나지 않고, 책임 분리가 실제 호출부에 반영되어야 한다.

### 우선순위
1. Namespace
2. Repository Lookup
3. Repository Access Resolution
4. Mergeability Assessment
5. Pipeline Policy

### 현재 코드 기준 개념별 진단

#### 1. Namespace
- 현재 `RepositoryNamespaceResolver`는 두 방향의 책임을 함께 가진다.
- `resolve(String namespace)`는 `namespace -> owner` 해석이다.
- `resolve(Repository)`와 `resolve(OwnerType, OwnerId)`는 `owner -> namespace` 변환이다.
- 같은 `resolve` 이름에 서로 다른 방향이 섞여 있다.

#### 2. Repository Lookup
- 현재 `RepositoryLookupService`는 조회와 visibility 판단을 같이 가진다.
- `findByPath(...)`는 경로 해석 책임이다.
- `isVisibleToRequester(...)`, `isVisibleToUserOwner(...)`는 접근 판단 책임이다.
- 조회 서비스 안에 접근 정책이 같이 들어가 있다.

#### 3. Repository Access Resolution
- 현재 `GitRepositoryAccessService`는 repository 해석, visibility 조회, permission 계산을 모두 수행한다.
- `ownerType == null`이면 namespace 기반 조회로 분기한다.
- namespace 충돌 시 `RepositoryLookupService`와 다르게 빈 결과를 반환한다.
- `RepositoryPermission`이 `use case` 내부 record로 존재해 재사용 경계가 약하다.

#### 4. Mergeability Assessment
- 현재 `MergeabilityAssessmentAssembler`는 역할이 비교적 단순하다.
- 다만 `reason` 문자열이 application 계층에서 직접 조합된다.
- `UNKNOWN`과 topology unknown 처리 규칙은 assembler 안에 묶여 있다.

#### 5. Pipeline Policy
- 현재 `EventPolicyResolver`는 push plan 위임만 담당한다.
- 실제 규칙은 `PushJobCreationPolicy`에 있다.
- `plan(...)` 안에 config 조회, rule 결정, file 존재 확인, 예외 fallback이 함께 들어가 있다.
- 향후 policy를 더 늘리면 같은 구조가 반복될 가능성이 높다.

### 방법 검토

#### 방안 1. 개념별 서비스 이름만 정리
- 장점: 수정량이 가장 적다.
- 단점: 책임 혼재는 유지된다.
- 단점: 후속 리팩토링 때 다시 흔들린다.

#### 방안 2. 개념별 책임 경계를 먼저 문서화하고, 각 단계마다 최소 코드 이동과 이름 정리를 수행
- 장점: 점진적 리팩토링이 가능하다.
- 장점: 기존 동작을 유지하기 쉽다.
- 장점: 테스트 범위를 개념별로 고정할 수 있다.

#### 방안 3. Shared 패키지를 새로 만들고 전부 한 번에 재배치
- 장점: 최종 구조를 빠르게 맞출 수 있다.
- 단점: 영향 범위가 크다.
- 단점: 회귀 위험이 높다.

### 선택 방안
- 방안 2를 선택한다.
- 이유는 현재 코드 기준으로 개념 간 결합이 남아 있으므로, 한 번에 재배치하면 회귀 위험이 크기 때문이다.
- 먼저 개념별 경계를 분리하고, 이후 패키지 재배치는 별도 단계로 진행하는 편이 안전하다.

### 개념별 상세 계획

## 1. Namespace

### 목표
- namespace 해석과 namespace 문자열 변환을 구분한다.
- `resolve`는 해석 책임만 남긴다.
- `owner -> namespace` 변환은 별도 이름으로 분리한다.

### 현재 문제
- `RepositoryNamespaceResolver`에서 서로 다른 방향의 책임이 같은 메서드명으로 노출된다.
- 호출부가 이름만 보고 해석인지 변환인지 구분하기 어렵다.

### 변경 방향
- `resolve(String namespace)`만 유지한다.
- `resolve(Repository)`는 `toNamespace(Repository)`로 변경한다.
- `resolve(OwnerType, OwnerId)`는 `toNamespace(OwnerType, OwnerId)`로 변경한다.
- 구 메서드는 호출부 변경이 끝난 뒤 제거한다.

### 예상 영향 범위
- `PullRequestService`
- `RepositoryManagementService`
- `BranchManagementService`
- `RepositoryProvisioner`
- `PullRequestMergeabilityResolver`
- 관련 단위 테스트
- 메인 코드와 테스트 호출부가 함께 변경 대상이다.

### 예시 코드

```java
@Component
@RequiredArgsConstructor
public class RepositoryNamespaceResolver {

    private final OrganizePersistencePort organizePort;
    private final UserPersistencePort userPort;

    public NamespaceInfo resolve(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new InvalidNamespaceException("Namespace cannot be empty");
        }

        String target = namespace.trim();

        NamespaceInfo organizeInfo = resolveAsOrganize(target);
        if (organizeInfo != null) {
            return organizeInfo;
        }

        NamespaceInfo userInfo = resolveAsUser(target);
        if (userInfo != null) {
            return userInfo;
        }

        throw new InvalidNamespaceException(
                "Could not resolve namespace to organize or user: " + target);
    }

    public String toNamespace(Repository repository) {
        return toNamespace(repository.getOwnerType(), repository.getOwnerId());
    }

    public String toNamespace(OwnerType ownerType, OwnerId ownerId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return organizePort.findById(OrganizeId.of(ownerId.getValue()))
                    .map(org -> org.getName().getValue())
                    .orElseThrow(OrganizeNotFoundException::new);
        }

        User user = userPort.findById(ownerId.getValue())
                .orElseThrow(UserNotFoundException::new);
        return user.getUsername();
    }

    private NamespaceInfo resolveAsOrganize(String namespace) {
        return organizePort.findByName(OrganizeName.from(namespace))
                .map(org -> new NamespaceInfo(
                        OwnerType.ORGANIZATION,
                        OwnerId.of(org.getId().getValue())))
                .orElse(null);
    }

    private NamespaceInfo resolveAsUser(String namespace) {
        return userPort.findUserIdByUsername(namespace)
                .map(userId -> new NamespaceInfo(OwnerType.USER, OwnerId.of(userId)))
                .orElse(null);
    }
}
```

### 구현 단계
1. `toNamespace(...)` 메서드를 추가한다.
2. 기존 `resolve(Repository)`, `resolve(OwnerType, OwnerId)` 호출부를 메인 코드와 테스트에서 전부 교체한다.
3. 테스트를 함께 수정한다.
4. 구 메서드를 제거한다.

### 검증 기준
- `resolve(String)`만 해석 책임으로 남아야 한다.
- `Repository`, `OwnerType + OwnerId` 기반 호출은 전부 `toNamespace(...)`를 사용해야 한다.
- 동작은 바뀌지 않아야 한다.

## 2. Repository Lookup

### 목표
- repository 조회 책임과 visibility 판단 책임을 분리한다.
- lookup 서비스는 경로 해석과 repository 조회만 담당하게 정리한다.

### 현재 문제
- `RepositoryLookupService`에 조회와 접근 판단이 같이 있다.
- `isVisibleToRequester(...)`, `isVisibleToUserOwner(...)`는 lookup 개념보다 access resolution에 가깝다.
- namespace 충돌 시 user-owned repository 우선 규칙이 lookup 서비스에 하드코딩되어 있다.

### 변경 방향
- `findByPath(...)`만 lookup 핵심 API로 유지한다.
- visibility 관련 메서드는 별도 access 판단 컴포넌트로 이동한다.
- namespace 충돌 우선순위는 lookup 정책으로 문서화하고 테스트로 고정한다.

### 예시 코드

```java
@Component
@RequiredArgsConstructor
public class RepositoryLookupService {

    private final RepositoryPersistencePort repositoryPort;
    private final UserPersistencePort userPort;
    private final OrganizePersistencePort organizePort;

    public Optional<Repository> findByPath(String namespace, String repoName) {
        String normalizedNamespace = normalize(namespace);
        String normalizedRepoName = normalize(repoName);

        String clonePath = RepositoryPathHelper.buildClonePath(
                normalizedNamespace,
                normalizedRepoName);

        Optional<Repository> byClonePath = repositoryPort.findByClonePath(clonePath);
        if (byClonePath.isPresent()) {
            return byClonePath;
        }

        Optional<Repository> userOwned = findUserOwned(normalizedNamespace, normalizedRepoName);
        Optional<Repository> organizationOwned = findOrganizationOwned(
                normalizedNamespace,
                normalizedRepoName);

        if (userOwned.isPresent() && organizationOwned.isPresent()) {
            return userOwned;
        }

        return userOwned.isPresent() ? userOwned : organizationOwned;
    }

    private Optional<Repository> findUserOwned(String namespace, String repoName) {
        return userPort.findByUsername(namespace)
                .flatMap(user -> repositoryPort.findByOwnerAndName(
                        OwnerType.USER,
                        OwnerId.of(user.getId()),
                        RepositoryName.from(repoName)));
    }

    private Optional<Repository> findOrganizationOwned(String namespace, String repoName) {
        return organizePort.findByName(OrganizeName.from(namespace))
                .flatMap(org -> repositoryPort.findByOwnerAndPath(
                        OwnerType.ORGANIZATION,
                        OwnerId.of(org.getId().getValue()),
                        RepositoryPath.from(repoName)));
    }

    private String normalize(String value) {
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
```

### 구현 단계
1. `RepositoryLookupService`에서 visibility 관련 메서드를 분리 대상으로 표시한다.
2. 조회 메서드만 남기도록 새 접근 판단 컴포넌트를 만든다.
3. 기존 호출부를 새 컴포넌트로 이관한다.
4. namespace 충돌 시 user 우선 규칙을 테스트로 고정한다.

### 검증 기준
- lookup 서비스는 repository 조회만 담당해야 한다.
- visibility 계산은 lookup 서비스 밖으로 이동해야 한다.
- clone path 우선 조회 동작은 유지되어야 한다.

## 3. Repository Access Resolution

### 목표
- repository 해석과 permission 계산을 분리한다.
- namespace 충돌 처리 규칙을 명시한다.
- `RepositoryPermission`의 위치를 재검토한다.

### 현재 문제
- `GitRepositoryAccessService`가 repository 해석, visibility, permission 계산을 동시에 수행한다.
- `ownerType == null`일 때 namespace 모드로 동작하지만, 충돌 시 empty를 반환한다.
- 같은 namespace 충돌에 대해 lookup과 access의 정책이 다르다.

### 변경 방향
- repository 식별은 lookup/resolver에 위임한다.
- access 서비스는 `Repository`를 입력받아 permission만 계산하는 방향으로 축소한다.
- `RepositoryPermission`은 application 공용 DTO 또는 support result로 분리 여부를 결정한다.

### 예시 코드

```java
@Service
@RequiredArgsConstructor
public class GitRepositoryAccessService implements GitRepositoryAccessUseCase {

    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final OrganizeMemberPersistencePort organizeMemberPort;

    public RepositoryPermission resolvePermission(
            OwnerType ownerType,
            String ownerName,
            String repositoryName,
            Long userId) {

        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) {
            return RepositoryPermission.none();
        }

        return resolvePermission(repository.get(), userId);
    }

    @Override
    public RepositoryPermission resolvePermission(Repository repository, Long userId) {
        if (repository == null) {
            return RepositoryPermission.none();
        }
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC && userId == null) {
            return RepositoryPermission.publicReadOnly();
        }
        if (userId == null) {
            return RepositoryPermission.anonymous();
        }

        UserId requesterId = UserId.of(userId);
        if (isOwner(repository, requesterId)) {
            return RepositoryPermission.owner();
        }

        Optional<RepositoryPermission> repositoryMemberPermission =
                resolveRepositoryMemberPermission(repository, requesterId);
        if (repositoryMemberPermission.isPresent()) {
            return repositoryMemberPermission.get();
        }

        Optional<RepositoryPermission> organizationPermission =
                resolveOrganizationPermission(repository, requesterId);
        return organizationPermission.orElseGet(RepositoryPermission::none);
    }

    private Optional<Repository> resolveRepository(
            OwnerType ownerType,
            String ownerName,
            String repositoryName) {
        if (ownerType == null) {
            return repositoryLookupService.findByPath(ownerName, repositoryName);
        }

        return repositoryLookupService.findByOwner(ownerType, ownerName, repositoryName);
    }
}
```

### 구현 단계
1. repository 식별 로직을 lookup/resolver로 분리한다.
2. `GitRepositoryAccessService`는 permission 계산 중심으로 축소한다.
3. namespace 충돌 정책을 하나로 통일한다.
4. `RepositoryPermission` 위치를 정리한다.

### 검증 기준
- access 서비스는 계산 책임 중심이어야 한다.
- namespace 충돌 동작이 lookup과 access에서 같아야 한다.
- public, owner, repository member, organization member 우선순위가 테스트로 고정되어야 한다.

## 4. Mergeability Assessment

### 목표
- merge preview 결과 변환 규칙을 명시 모델로 유지한다.
- `reason` 생성 규칙을 단순화한다.
- 읽기 모델과 변환 책임의 경계를 고정한다.

### 현재 문제
- assembler는 단순하지만, 문자열 reason이 길고 application 계층에 직접 박혀 있다.
- 상태와 설명 규칙이 늘어나면 assembler가 문자열 정책까지 떠안게 된다.

### 변경 방향
- `MergeabilityAssessmentAssembler`는 상태/토폴로지 변환만 담당하게 유지한다.
- 사람이 읽는 `reason`은 최소 문장으로 축소한다.
- 필요하면 `reason code`를 추가하고 문장 생성은 API 응답 계층으로 미룬다.

### 예시 코드

```java
@Component
public class MergeabilityAssessmentAssembler {

    public MergeabilityAssessment toAssessment(MergeResult result) {
        if (result == null || result.getStatus() == null) {
            return new MergeabilityAssessment(
                    MergeabilityStatus.UNKNOWN,
                    MergeTopologySummary.unknown(),
                    List.of(),
                    "UNKNOWN");
        }

        MergeabilityStatus status = switch (result.getStatus()) {
            case MERGEABLE, MERGED, ALREADY_UP_TO_DATE -> MergeabilityStatus.MERGEABLE;
            case CONFLICTS -> MergeabilityStatus.CONFLICTING;
            case NO_COMMON_ANCESTOR -> MergeabilityStatus.NO_COMMON_ANCESTOR;
        };

        MergeTopologySummary topology =
                result.getFastForwardPossible() == null || result.getMergeCommitRequired() == null
                        ? MergeTopologySummary.unknown()
                        : MergeTopologySummary.known(
                                result.getFastForwardPossible(),
                                result.getMergeCommitRequired());

        return new MergeabilityAssessment(
                status,
                topology,
                result.getConflicts(),
                status.name());
    }
}
```

### 구현 단계
1. assembler 책임을 상태 변환으로 한정한다.
2. `reason`을 최소화하거나 reason code로 대체한다.
3. 응답 계층에서 사람이 읽는 설명이 필요한지 재검토한다.

### 검증 기준
- 상태 변환 결과는 기존과 같아야 한다.
- unknown 처리 규칙은 유지되어야 한다.
- conflict 목록 복사와 null 방어는 유지되어야 한다.

## 5. Pipeline Policy

### 목표
- pipeline 실행 여부 판정 규칙을 명확히 분리한다.
- push 이벤트 정책과 pipeline 파일 검증을 분해한다.
- 후속 이벤트 확장에 대비한 구조를 만든다.

### 현재 문제
- `PushJobCreationPolicy`에 config 조회, rule 선택, file 검증, 예외 처리까지 몰려 있다.
- `EventPolicyResolver`는 사실상 단순 위임이라 확장 포인트가 약하다.

### 변경 방향
- `PushJobCreationPolicy`는 push 전용 규칙으로 유지한다.
- 내부 단계를 `config resolve`, `rule resolve`, `file validate`로 명시적으로 분리한다.
- 이벤트가 늘어나면 `EventPolicyResolver`가 event type별 정책을 조합하도록 확장한다.

### 예시 코드

```java
@Component
@RequiredArgsConstructor
public class PushJobCreationPolicy {

    private static final String PIPELINE_ROOT = ".jgitkins/";

    private final PipelineConfigPort configPort;
    private final FileGitPort fileGitPort;

    public JobPlan plan(PushJobPlanRequest request) {
        try {
            PipelineConfig config = loadConfig(request);
            PipelineRule rule = selectRule(config, request.branchName());
            if (rule == null) {
                return JobPlan.skip(PipelineSkipReason.SKIPPED_NO_RULE);
            }

            String pipelineFilePath = normalizePipelineFile(rule.getFile());
            if (!pipelineFileExists(request, pipelineFilePath)) {
                return JobPlan.skip(PipelineSkipReason.SKIPPED_PIPELINE_NOT_FOUND);
            }

            return JobPlan.create(pipelineFilePath);
        } catch (RuntimeException ex) {
            return JobPlan.skip(PipelineSkipReason.SKIPPED_POLICY_ERROR);
        }
    }

    private PipelineConfig loadConfig(PushJobPlanRequest request) {
        return configPort.read(request.namespace(), request.repoName(), request.commitHash());
    }

    private PipelineRule selectRule(PipelineConfig config, String branchName) {
        if (config == null) {
            return null;
        }
        return config.findRule(branchName).orElse(null);
    }

    private String normalizePipelineFile(String file) {
        return file.startsWith(PIPELINE_ROOT) ? file : PIPELINE_ROOT + file;
    }

    private boolean pipelineFileExists(PushJobPlanRequest request, String pipelineFilePath) {
        return fileGitPort.exists(
                request.namespace(),
                request.repoName(),
                request.commitHash(),
                pipelineFilePath);
    }
}
```

### 구현 단계
1. policy 내부 단계를 private 메서드로 먼저 분리한다.
2. 예외 fallback 범위를 검토한다.
3. 후속 이벤트 정책이 필요하면 `EventPolicyResolver`를 확장한다.

### 검증 기준
- branch rule 매칭 결과는 기존과 같아야 한다.
- pipeline file 존재 검증 결과는 기존과 같아야 한다.
- policy 오류 시 skip 처리 규칙은 유지되어야 한다.

### 단계별 수행 순서
1. Namespace 리팩토링 수행
2. Namespace 관련 테스트와 호출부 정리
3. Repository Lookup 리팩토링 수행
4. Access Resolution 리팩토링 수행
5. Mergeability Assessment 정리
6. Pipeline Policy 정리

### 개념별 산출물
- 코드 변경
- 관련 테스트 보강
- 계획 문서 업데이트
- 완료 후 다음 개념으로 이동 여부 판단

### 이번 계획의 핵심 판단
- `2.18`은 단일 클래스 수정이 아니라 shared concept 정리의 시작점으로 본다.
- 다만 구현은 반드시 개념 하나씩 닫으면서 진행한다.
- 첫 구현 대상은 `Namespace`다.

### 후속 메모
- `RepositoryLookupService`와 `GitRepositoryAccessService`의 namespace 충돌 정책은 반드시 같은 규칙으로 수렴시켜야 한다.
- `RepositoryPermission`의 위치는 access resolution 단계에서 같이 정리한다.
- `Pipeline Policy`는 이번 문서에 포함하지만, 구현 우선순위는 가장 뒤로 둔다.
