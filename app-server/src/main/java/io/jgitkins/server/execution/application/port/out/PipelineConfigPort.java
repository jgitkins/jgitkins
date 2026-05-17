package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.contract.pipeline.PipelineConfig;

public interface PipelineConfigPort {

    PipelineConfig read(String namespace, String repoName, String commitHash);
}
