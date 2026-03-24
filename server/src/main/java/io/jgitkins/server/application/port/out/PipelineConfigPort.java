package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;

public interface PipelineConfigPort {

    PipelineConfig read(String namespace, String repoName, String commitHash);
}
