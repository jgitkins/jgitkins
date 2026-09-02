package io.jgitkins.server.execution.application.port.out;

import io.jgitkins.server.execution.application.internal.PipelineConfig;

public interface PipelineConfigPort {

    PipelineConfig read(String namespace, String repoName, String commitHash);
}
