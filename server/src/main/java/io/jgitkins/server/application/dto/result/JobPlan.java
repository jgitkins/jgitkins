package io.jgitkins.server.application.dto.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JobPlan {

    private final boolean creatable;
    private final String pipelineFilePath;
    private final PipelineSkipReason skipReason;

    public static JobPlan create(String pipelineFilePath) {
        return new JobPlan(true, pipelineFilePath, null);
    }

    public static JobPlan skip(PipelineSkipReason skipReason) {
        return new JobPlan(false, null, skipReason);
    }

    public boolean isSkipped() {
        return !creatable;
    }
}
