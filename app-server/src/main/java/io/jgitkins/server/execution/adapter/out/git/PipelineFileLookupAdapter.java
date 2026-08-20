package io.jgitkins.server.execution.adapter.out.git;

import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.execution.application.port.out.PipelineFileLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineFileLookupAdapter implements PipelineFileLookupPort {

    private final FileGitPort fileGitPort;

    @Override
    public boolean exists(String namespace, String repoName, String commitHash, String path) {
        return fileGitPort.exists(namespace, repoName, commitHash, path);
    }
}
