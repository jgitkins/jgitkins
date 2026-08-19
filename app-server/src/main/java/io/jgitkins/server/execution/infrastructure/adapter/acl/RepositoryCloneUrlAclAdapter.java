package io.jgitkins.server.execution.infrastructure.adapter.acl;

import io.jgitkins.server.execution.application.port.out.CloneUrlPort;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryCloneUrlAclAdapter implements CloneUrlPort {
    private final CloneUrlBuilder cloneUrlBuilder;

    @Override
    public String build(String repositoryClonePath) {
        return cloneUrlBuilder.build(repositoryClonePath);
    }
}
