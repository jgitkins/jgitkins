package io.jgitkins.server.repository.infrastructure.adapter.acl;

import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserNamespaceAclAdapter implements UserNamespacePort {
    private final UserQueryPort delegate;
    @Override public Optional<Long> findUserIdByUsername(String username) { return delegate.findUserIdByUsername(username); }
}
