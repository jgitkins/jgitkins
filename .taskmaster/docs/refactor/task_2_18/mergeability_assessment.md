# Mergeability Assessment

## 핵심
- merge preview 결과 변환 책임만 유지하고, 설명 문자열 정책은 축소한다.

## 정리 방향
- `MergeabilityAssessmentAssembler`는 상태/토폴로지 변환에 집중한다.
- `reason`은 최소화하거나 reason code로 대체한다.
- 사람이 읽는 설명 생성은 필요 시 응답 계층으로 미룬다.

## 예시 코드
```java
package io.jgitkins.server.shared.application.change;

import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.domain.model.changegraph.MergeTopologySummary;
import java.util.List;
import org.springframework.stereotype.Component;

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

        MergeabilityStatus status = toStatus(result.getStatus());
        MergeTopologySummary topology = toTopology(result);

        return new MergeabilityAssessment(
                status,
                topology,
                result.getConflicts() == null ? List.of() : List.copyOf(result.getConflicts()),
                status.name());
    }

    private MergeabilityStatus toStatus(MergeResult.Status status) {
        return switch (status) {
            case MERGEABLE, MERGED, ALREADY_UP_TO_DATE -> MergeabilityStatus.MERGEABLE;
            case CONFLICTS -> MergeabilityStatus.CONFLICTING;
            case NO_COMMON_ANCESTOR -> MergeabilityStatus.NO_COMMON_ANCESTOR;
        };
    }

    private MergeTopologySummary toTopology(MergeResult result) {
        if (result.getFastForwardPossible() == null || result.getMergeCommitRequired() == null) {
            return MergeTopologySummary.unknown();
        }
        return MergeTopologySummary.known(
                result.getFastForwardPossible(),
                result.getMergeCommitRequired());
    }
}
```

## 아키텍처 메모
- 장문 `reason`을 assembler에서 만들면 presentation concern이 application support로 스며든다.
- assembler는 domain 읽기 모델을 안정적으로 만드는 역할만 맡는 편이 낫다.
- `reason`이 꼭 필요하면 `reasonCode`를 두고, 사용자 메시지 조합은 API 응답 계층이나 presenter로 미룬다.
- `Task 2.18` 1차 shared 이관 대상으로 적합하다.
- 목표 위치는 `io.jgitkins.server.shared.application.change`다.

## 검증 기준
- 상태 변환 결과는 기존과 같아야 한다.
- unknown 처리 규칙은 유지되어야 한다.
- null 방어와 conflict 목록 전달 규칙은 유지되어야 한다.
