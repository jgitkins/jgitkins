# 04. Pull Request Presentation and Tests

## 목적

`Change & Review Context`의 API와 테스트 경계를 정리한다.

이 문서는 PR 생성/상세 조회용 presentation endpoint를 새로 두고, merge controller는 repository-level command로 유지하는 계획을 담는다.

## 핵심 결론

- PR 전용 controller를 추가한다.
- controller는 use case 호출만 하고, 계산 로직은 갖지 않는다.
- `MergeController`는 repository-level merge flow로 유지한다.
- PR 상태 전이는 이번 단계에서 merge endpoint에 직접 얹지 않는다.

## 대상 파일

- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/MergeController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/PullRequestController.java` 추가 예정
- `app-server/src/main/java/io/jgitkins/server/presentation/dto/...` 추가 예정
- `app-server/src/main/java/io/jgitkins/server/presentation/mapper/...` 추가 예정

## controller 설계

### 1. PullRequestController 신규 도입

권장 base path:

```text
/repositories/{namespace}/{repoName}/pull-requests
```

기본 endpoint:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/repositories/{namespace}/{repoName}/pull-requests")
public class PullRequestController {

    private final CreatePullRequestUseCase createPullRequestUseCase;
    private final GetPullRequestDetailUseCase getPullRequestDetailUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<PullRequestResult>> createPullRequest(
            @PathVariable String namespace,
            @PathVariable String repoName,
            @RequestBody PullRequestCreateRequest request) {
        PullRequestCreateCommand command = new PullRequestCreateCommand(
                namespace,
                repoName,
                request.sourceBranch(),
                request.targetBranch());
        PullRequestResult result = createPullRequestUseCase.createPullRequest(command);
        return ApiResponse.created(result.getId(), result);
    }

    @GetMapping("/{pullRequestId}")
    public ResponseEntity<ApiResponse<PullRequestDetailResult>> getPullRequestDetail(
            @PathVariable Long pullRequestId) throws IOException {
        return ApiResponse.ok(getPullRequestDetailUseCase.getPullRequestDetail(PullRequestId.of(pullRequestId)));
    }
}
```

### 2. MergeController 유지

`MergeController`는 현재처럼 repository-level merge를 유지한다.

```java
@GetMapping("/repositories/{namespace}/{repoName}/merge/check")
@PostMapping("/repositories/{namespace}/{repoName}/merge")
```

이번 단계에서 하지 않는 것:

- PR id를 `MergeRequest`에 끼워 넣지 않는다.
- merge 완료 시 `PullRequest.markMerged()`를 controller에서 직접 호출하지 않는다.
- repository-level merge endpoint를 PR status transition endpoint로 바꾸지 않는다.

## test 설계

### 1. PullRequestControllerTest

검증 대상:

- `POST /repositories/{namespace}/{repoName}/pull-requests`
- `GET /repositories/{namespace}/{repoName}/pull-requests/{pullRequestId}`
- request -> command mapping
- response body via `ApiResponse`

예시:

```java
when(createPullRequestUseCase.createPullRequest(any(PullRequestCreateCommand.class)))
        .thenReturn(PullRequestResult.builder()...build());
```

### 2. service tests

- `PullRequestCreateServiceTest`
- `PullRequestQueryServiceTest`
- `MergeServiceTest`

각 테스트는 다음을 확인한다.

- create service는 current Git head만 저장하고 mergeability를 저장하지 않는다.
- query service는 current head + target drift + mergeability를 조립한다.
- merge service는 mergeability 계산과 실제 merge command를 분리해서 유지한다.

### 3. architecture guardrail

`ArchitecturePackageConventionTest`에 아래 기준을 추가한다.

- presentation은 application port만 의존한다.
- presentation은 infrastructure mapper/adapter를 직접 참조하지 않는다.
- PR controller는 `PullRequestController`가 생기더라도 use case 중심 계약을 유지한다.

## 권장 파일 추가

- `app-server/src/main/java/io/jgitkins/server/presentation/api/rest/PullRequestController.java`
- `app-server/src/main/java/io/jgitkins/server/presentation/dto/PullRequestCreateRequest.java`
- `app-server/src/test/java/io/jgitkins/server/presentation/api/rest/PullRequestControllerTest.java`

필요하면 request mapper를 별도로 둔다.

## 완료 기준

- PR 생성/상세 조회 엔드포인트가 presentation에 명시된다.
- merge controller는 repository-level command로 남는다.
- controller는 thin adapter로 유지되고, 계산 로직은 service/support로만 존재한다.
- 테스트가 endpoint, service, architecture 경계를 각각 검증한다.
