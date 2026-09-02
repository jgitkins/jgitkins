package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.contract.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.internal.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.contract.result.OAuthLoginResult;
import io.jgitkins.server.identity.access.application.internal.VerifiedOAuthIdentity;
import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;
import io.jgitkins.server.identity.access.application.port.out.OAuthIdTokenVerifierPort;
import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import io.jgitkins.server.identity.access.application.support.UserService;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthLoginService implements OAuthLoginUseCase {

    private final OAuthIdTokenVerifierPort idTokenVerifier;
    private final UserService userService;
    private final TokenIssuerPort tokenIssuerPort;

    /**
     * Verify first, then act on what the verification returned.
     *
     * <p>Not transactional, deliberately. Verification can make a network round trip to the
     * provider for its signing keys, and a transaction opened here would hold a pool connection for
     * the length of that call. The atomicity that matters is narrower than this method: signup
     * writes a USER row and a USER_IDENTITY row and they must both exist or neither, which is
     * {@code UserService.loginOrSignUp}'s boundary and is where the annotation lives. Issuing the
     * token afterwards touches no database.
     *
     * <p>Before task 2.114 there was no transaction anywhere on this path, so the USER row was
     * committed and {@code UserIdentity.create} then rejected its arguments -- a dangling account
     * whose username {@code UsernameAllocator} would refuse to hand out again.
     */
    @Override
    public OAuthLoginResult login(OAuthLoginCommand command) {
        VerifiedOAuthIdentity identity = idTokenVerifier.verify(command.provider(), command.idToken());

        User user = userService.loginOrSignUp(new UserLoginOrSignUpCommand(
                identity.provider(),
                identity.subject(),
                identity.email(),
                identity.emailVerified(),
                identity.name(),
                identity.avatarUrl()
        ));
        String appToken = tokenIssuerPort.issueToken(user.getId(), List.of("ROLE_USER"));
        return new OAuthLoginResult(appToken, user, identity.provider());
    }
}
