package io.jgitkins.server.repository.adapter.out.acl;

import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryActorAclAdapter implements RepositoryActorPort {
    private final CurrentUserPort delegate;
    @Override public Optional<Long> resolveCurrentUserId() { return delegate.resolveCurrentUserId(); }
}
