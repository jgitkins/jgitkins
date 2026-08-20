package io.jgitkins.server.identity.access.adapter.out.policy;

import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.identity.access.application.port.out.ActiveAccountPolicyPort;
import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveAccountPolicyAdapter implements ActiveAccountPolicyPort {
    private final CurrentUserPort currentUserPort;
    private final UserRepository userRepository;

    @Override
    public Long requireActiveUserId() {
        Long userId = currentUserPort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated"));
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "Access denied");
        }
        return userId;
    }
}
