# 02. Pull Request Application Services

## 목적

`PullRequestService`에 섞여 있는 생성/조회 책임을 분리하고, `MergeService`와의 경계도 명확히 한다.

이 문서는 application service를 create/query 중심으로 분리하는 계획이다.

## 핵심 결론

- PR 생성과 PR 상세 조회는 서로 다른 service로 분리한다.
- `MergeService`는 Git merge 계산/실행만 책임진다.
- PR 상태 전이는 이번 단계에서 merge API에 직접 묶지 않는다.

## 대상 파일

- `app-server/src/main/java/io/jgitkins/server/application/service/PullRequestService.java`
- `app-server/src/main/java/io/jgitkins/server/application/service/MergeService.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestMergeabilityResolver.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestDetailMapper.java`
- `app-server/src/main/java/io/jgitkins/server/application/support/pr/PullRequestResultMapper.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/in/CreatePullRequestUseCase.java`
- `app-server/src/main/java/io/jgitkins/server/application/port/in/GetPullRequestDetailUseCase.java`

## TO-BE 서비스 구조

권장 구조는 다음과 같다.

```text
app-server/application/service/
  PullRequestCreateService
  PullRequestQueryService
  MergeService
```

### 1. PullRequestCreateService

책임:

- repository lookup
- source/target branch head capture
- `PullRequest.create(...)`
- save
- result mapping

예시:

```java
@Service
@RequiredArgsConstructor
public class PullRequestCreateService implements CreatePullRequestUseCase {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchGitPort branchGitPort;
    private final PullRequestResultMapper resultMapper;

    @Override
    @Transactional
    public PullRequestResult createPullRequest(PullRequestCreateCommand command) {
        Repository repository = repositoryLookupService.resolveByPath(command.namespace(), command.repoName())
                .orElseThrow(() -> new RepositoryNotFoundException(command.namespace(), command.repoName()));
        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getPath().getValue();

        BranchHeadSnapshot source = currentHead(namespace, repoName, command.sourceBranch());
        BranchHeadSnapshot target = currentHead(namespace, repoName, command.targetBranch());

        PullRequest saved = pullRequestRepository.save(PullRequest.create(repository.getId(), source, target));
        return resultMapper.toResult(saved);
    }
}
```

### 2. PullRequestQueryService

책임:

- PR aggregate load
- repository load
- current source/target head load
- target drift observation
- mergeability evaluation
- detail mapping

예시:

```java
@Service
@RequiredArgsConstructor
public class PullRequestQueryService implements GetPullRequestDetailUseCase {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestMergeabilityResolver mergeabilityResolver;
    private final PullRequestDetailMapper detailMapper;

    @Override
    @Transactional(readOnly = true)
    public PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId) throws IOException {
        PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new PullRequestNotFoundException(pullRequestId));
        Repository repository = repositoryRepository.findById(pullRequest.getRepositoryId())
                .orElseThrow(() -> new RepositoryNotFoundException(pullRequest.getRepositoryId().getValue()));

        BranchHeadSnapshot currentSource = mergeabilityResolver.currentSourceHead(repository, pullRequest);
        BranchHeadSnapshot currentTarget = mergeabilityResolver.currentTargetHead(repository, pullRequest);
        PullRequest observed = pullRequest.markTargetDrifted(currentTarget);
        MergeabilityAssessment assessment = mergeabilityResolver.assess(repository, observed);

        return detailMapper.toDetail(observed, currentSource, currentTarget, assessment);
    }
}
```

### 3. MergeService 경계

`MergeService`는 그대로 유지하되, PR 상태 전이를 끌어오지 않는다.

```java
@Service
@RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeabilityEvaluationUseCase, MergeUseCase {
    private final MergeGitPort mergeGitPort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;
}
```

이번 단계에서는 다음을 하지 않는다.

- `performMerge(...)`에 PR id를 추가하지 않는다.
- merge 성공 후 `PullRequest.markMerged()`를 자동 호출하지 않는다.
- repository-level merge API를 PR state transition API로 바꾸지 않는다.

## 정리 포인트

- `PullRequestService`는 더 이상 create/query를 동시에 가지지 않도록 정리한다.
- create/query service의 의존성 집합이 분리되도록 한다.
- `PullRequestMergeabilityResolver`는 query service 전용 collaborator로 유지한다.
- merge 수행은 repository-level flow로 남기고, PR 상태 전이 seam은 다음 단계로 미룬다.

## 테스트 기준

- `PullRequestCreateServiceTest`
  - repository lookup 실패
  - source/target branch missing
  - successful save/result mapping
- `PullRequestQueryServiceTest`
  - PR not found
  - repository not found
  - current head observation and mergeability mapping
- `MergeServiceTest`
  - 현재 책임 유지 확인
  - mergeability preview/evaluation/perform merge 회귀 검증

## 완료 기준

- create/query 책임이 서로 다른 service로 분리된다.
- `PullRequestService`를 제거하거나 단일 facade로 남기지 않는 방향이 명확하다.
- `MergeService`가 PR 상태 전이와 섞이지 않는다.
