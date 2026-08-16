package io.jgitkins.server.collaboration.adapter.outbound.identity;

import io.jgitkins.server.collaboration.application.port.out.UserIdentityPort;
import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InProcessUserIdentityAdapter implements UserIdentityPort {

    private final CurrentUserPort currentUserPort;

    @Override
    public Optional<Long> resolveCurrentActiveUserId() {
        // CurrentUserPort currently exposes only authenticated-user identity.
        // Active-account validation remains an identity-context responsibility.
        return currentUserPort.resolveCurrentUserId();
    }
}
