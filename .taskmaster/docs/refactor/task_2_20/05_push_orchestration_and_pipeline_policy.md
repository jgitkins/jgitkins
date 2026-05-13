# 05. Push Orchestration / Pipeline Policy 상세 계획

## 목적

Push event 이후 Job 생성 흐름에서 cross-context orchestration과 Execution Context 책임을 분리한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/application/service/PushEventHandleService.java
server/src/main/java/io/jgitkins/server/application/support/execution/ExecutionRequestService.java
server/src/main/java/io/jgitkins/server/shared/application/policy/EventPolicyResolver.java
server/src/main/java/io/jgitkins/server/shared/application/policy/PushJobCreationPolicy.java
server/src/main/java/io/jgitkins/server/application/validate/JobCreationValidator.java
server/src/main/java/io/jgitkins/server/application/support/change/BranchChangeRecorder.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/application/service/PushEventHandleService.java
    // 1차 유지. cross-context orchestration.

server/src/main/java/io/jgitkins/server/execution/application/support/ExecutionRequestService.java
server/src/main/java/io/jgitkins/server/execution/application/policy/PushJobCreationPolicy.java
server/src/main/java/io/jgitkins/server/execution/application/policy/EventPolicyResolver.java
server/src/main/java/io/jgitkins/server/execution/application/validate/JobCreationValidator.java
```

## 결정 사항

- `PushEventHandleService`는 지금 단계에서 execution 내부로 이동하지 않는다.
- `PushEventHandleService`는 repository branch change 기록과 execution request 생성을 조율하는 cross-context application service다.
- `ExecutionRequestService`는 execution application support로 이동한다.
- `PushJobCreationPolicy`는 execution application policy다.
- `PipelineConfigPort`는 execution application port다.
- raw file lookup은 execution application이 정의한 `PipelineFileLookupPort`로 처리한다.
- 1차 구현은 기존 `FileGitPort`를 adapter 내부에서 감싸서 위임한다. 이후 Repository Context에 file-read seam이 정리되면 위임 대상을 교체한다.
- `PipelineFileLookupPort` 덕분에 execution policy는 기존 `FileGitPort`나 repository 구현체를 직접 알지 않는다.
- execution policy가 repository infrastructure adapter를 직접 알면 안 된다.

## 현재 흐름

```text
PushEventCommand
    |
    v
PushEventHandleService
    |
    +-- BranchChangeRecorder.record(command)         // Repository/change recording side
    +-- JobCreationValidator.validate(command)       // Execution creation pre-check
    +-- EventPolicyResolver.resolvePushPlan(command) // Execution pipeline policy
    +-- ExecutionRequestService.requestPushExecution(command, plan)
```

## PushEventHandleService 스니펫

```java
package io.jgitkins.server.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushEventHandleService implements PushEventHandleUseCase {

    private final BranchChangeRecorder branchChangeRecorder;
    private final JobCreationValidator jobCreationValidator;
    private final EventPolicyResolver eventPolicyResolver;
    private final ExecutionRequestService executionRequestService;

    @Override
    @Transactional
    public void handle(PushEventCommand command) {
        branchChangeRecorder.record(command);

        JobCreationDecision decision = jobCreationValidator.validate(command);
        if (decision.isSkipped()) {
            log.info("push event job skipped: reason={}", decision.reason());
            return;
        }

        JobPlan jobPlan = eventPolicyResolver.resolvePushPlan(command);
        if (jobPlan.isSkipped()) {
            log.info("push event job skipped: reason={}", jobPlan.getSkipReason());
            return;
        }

        executionRequestService.requestPushExecution(command, jobPlan);
    }
}
```

`PushEventHandleService`는 1차 유지한다. 나중에 cross-context coordinator 이름이 필요하면 `PushEventOrchestrationService`로 별도 작업에서 변경한다.

## ExecutionRequestService 스니펫

```java
package io.jgitkins.server.execution.application.support;

@Component
@RequiredArgsConstructor
public class ExecutionRequestService {

    private final JobCreateUseCase jobCreateUseCase;

    public void requestPushExecution(PushEventCommand command, JobPlan plan) {
        jobCreateUseCase.create(new JobCreateCommand(
                command.getRepoName(),
                command.getRepositoryId(),
                command.getCommitHash(),
                command.getBranchName(),
                plan.getPipelineFilePath(),
                command.getTriggeredBy()));
    }
}
```

## Pipeline Policy 스니펫

```java
package io.jgitkins.server.execution.application.policy;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushJobCreationPolicy {

    private static final String PIPELINE_ROOT = ".jgitkins/";

    private final PipelineConfigPort pipelineConfigPort;
    private final PipelineFileLookupPort pipelineFileLookupPort;

    public JobPlan plan(PushJobPlanRequest request) {
        try {
            PipelineConfig config = pipelineConfigPort.read(request.namespace(), request.repoName(), request.commitHash());
            PipelineRule rule = resolveRule(config, request.branchName());
            if (rule == null) {
                return JobPlan.skip(PipelineSkipReason.SKIPPED_NO_RULE);
            }

            String pipelineFilePath = toPipelineFilePath(rule.getFile());
            if (!pipelineFileLookupPort.exists(request.namespace(), request.repoName(), request.commitHash(), pipelineFilePath)) {
                return JobPlan.skip(PipelineSkipReason.SKIPPED_PIPELINE_NOT_FOUND);
            }

            return JobPlan.create(pipelineFilePath);
        } catch (RuntimeException ex) {
            log.warn("push event job planning skipped due to policy error. repo=[{}] branch=[{}] commit=[{}]",
                    request.repoName(), request.branchName(), request.commitHash(), ex);
            return JobPlan.skip(PipelineSkipReason.SKIPPED_POLICY_ERROR);
        }
    }
}
```

`PipelineFileLookupPort`는 execution 소유 port다. 구현체는 현재 코드 기준 기존 `FileGitPort.exists(...)`에 위임하고, 이후 repository/read seam이 생기면 adapter 내부 delegate만 교체한다.

## 구현 순서

1. `ExecutionRequestService`를 execution application support로 이동한다.
2. `PushJobCreationPolicy`, `EventPolicyResolver`, `JobCreationValidator`를 execution application policy/validate로 이동한다.
3. `PipelineConfigPort`를 execution application port로 이동한다.
4. `PipelineFileLookupPort`를 execution application port로 추가하고 adapter에서 기존 `FileGitPort.exists(...)`에 위임한다.
5. `PushEventHandleService` import만 수정하고 위치는 유지한다.
6. `PushEventHandleServiceTest`, integration test, policy test import를 수정한다.

## 테스트 기준

- branch recording은 계속 호출된다.
- job creation validator skip이면 policy와 execution request를 호출하지 않는다.
- policy skip이면 execution request를 호출하지 않는다.
- policy success면 execution request가 호출된다.
- policy error는 exception이 아니라 skip decision으로 남는다.
- pipeline config read는 execution-owned port로 수행된다.
- raw file lookup은 repository infrastructure 직접 의존 없이 수행된다.

## 완료 기준

- execution request/policy code가 execution package에 있다.
- `PushEventHandleService`는 cross-context orchestration으로 남아 있다.
- `PushJobCreationPolicyTest`, `PushEventHandleServiceTest`가 통과한다.
