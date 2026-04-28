# Pipeline Policy

## 핵심
- pipeline 실행 여부 판정 단계를 명확히 분리한다.
- push 이벤트 정책을 후속 확장 가능한 구조로 정리한다.

## 정리 방향
- `PushJobCreationPolicy` 내부 단계를 `config resolve`, `rule resolve`, `file validate`로 분리한다.
- `EventPolicyResolver`는 event type별 정책 조합 지점으로 확장 가능하게 둔다.
- policy 오류 시 skip 처리 규칙은 유지한다.
- `PushEventHandleService`를 application 진입점으로 유지하고, webhook/request 전체를 domain object로 승격하지 않는다.
- 대신 policy 판단에 필요한 최소 의미 집합만 별도 domain-oriented value 또는 policy input model로 추출하는 방향을 우선 검토한다.

## DDD 정리 방향
- `PushEventHandleService`는 application service로서 진입점과 orchestration 책임을 가진다.
- 외부 입력인 `PushEventCommand`는 transport/application command로 유지한다.
- `PushJobCreationPolicy` 내부로 전체 request를 밀어 넣는 대신, branch/rule/file validation에 필요한 최소 의미 집합을 별도 모델로 축소한다.
- 예시 후보:
  `PipelineTriggerContext`
  `PushPolicyInput`
  `BranchUpdateContext`
- 즉, `request 자체를 domain model로 바꾸는 것`보다 `정책 판단에 필요한 의미만 분리해 domain-oriented input으로 만드는 것`이 더 적절하다.

## 예시 코드
```java
package io.jgitkins.server.shared.application.policy;

import static io.jgitkins.server.application.dto.result.PipelineSkipReason.SKIPPED_NO_RULE;
import static io.jgitkins.server.application.dto.result.PipelineSkipReason.SKIPPED_PIPELINE_NOT_FOUND;
import static io.jgitkins.server.application.dto.result.PipelineSkipReason.SKIPPED_POLICY_ERROR;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;
import io.jgitkins.server.application.dto.pipeline.PipelineRule;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.application.port.out.PipelineConfigPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushJobCreationPolicy {

    private static final String PIPELINE_ROOT = ".jgitkins/";

    private final PipelineConfigPort configPort;
    private final FileGitPort fileGitPort;

    public JobPlan plan(PushJobPlanRequest request) {
        try {
            PipelineConfig config = loadConfig(request);
            PipelineRule rule = selectRule(config, request.branchName());
            if (rule == null) {
                return JobPlan.skip(SKIPPED_NO_RULE);
            }

            String pipelineFilePath = normalizePipelineFile(rule.getFile());
            if (!pipelineFileExists(request, pipelineFilePath)) {
                return JobPlan.skip(SKIPPED_PIPELINE_NOT_FOUND);
            }

            return JobPlan.create(pipelineFilePath);
        } catch (RuntimeException ex) {
            log.warn(
                    "push event job planning skipped due to policy error. repo=[{}] branch=[{}] commit=[{}]",
                    request.repoName(),
                    request.branchName(),
                    request.commitHash(),
                    ex);
            return JobPlan.skip(SKIPPED_POLICY_ERROR);
        }
    }

    private PipelineConfig loadConfig(PushJobPlanRequest request) {
        return configPort.read(
                request.namespace(),
                request.repoName(),
                request.commitHash());
    }

    private PipelineRule selectRule(PipelineConfig config, String branchName) {
        if (config == null) {
            return null;
        }
        return config.findRule(branchName).orElse(null);
    }

    private String normalizePipelineFile(String file) {
        if (file.startsWith(PIPELINE_ROOT)) {
            return file;
        }
        return PIPELINE_ROOT + file;
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

```java
package io.jgitkins.server.shared.application.policy;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPolicyResolver {

    private final PushJobCreationPolicy pushJobCreationPolicy;

    public JobPlan resolvePushPlan(PushEventCommand command) {
        return pushJobCreationPolicy.plan(PushJobPlanRequest.from(command));
    }
}
```

## 아키텍처 메모
- 현재 `EventPolicyResolver`는 얇지만 불필요한 것은 아니다.
- 정책이 push 외 webhook, merge, schedule로 늘어나면 이 레이어가 event-to-policy 매핑 지점이 된다.
- `PushJobCreationPolicy` 내부 단계를 쪼개두면 테스트가 branch rule, file existence, fallback policy 단위로 고정된다.
- `PushJobCreationPolicy`, `EventPolicyResolver`는 1차 shared 이관 대상으로 적합하다.
- 목표 위치는 `io.jgitkins.server.shared.application.policy`다.
- `PushEventHandleService`는 진입점이므로 application 계층에 남기고, policy는 domain rule에 가까운 입력만 받아 계산하도록 좁히는 편이 DDD 관점에서 더 안정적이다.

## 검증 기준
- branch rule 매칭 결과는 기존과 같아야 한다.
- pipeline file 존재 검증 결과는 기존과 같아야 한다.
- policy 오류 시 skip 처리 규칙은 유지되어야 한다.
