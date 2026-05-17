package io.jgitkins.server.execution.application.port.out;

public interface PipelineFileLookupPort {
    boolean exists(String namespace, String repoName, String commitHash, String path);
}
