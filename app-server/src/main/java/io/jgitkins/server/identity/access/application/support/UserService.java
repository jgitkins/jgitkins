package io.jgitkins.server.identity.access.application.support;

import io.jgitkins.server.identity.access.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserService {

        private final UserRepository userRepository;
        private final UserIdentityPersistencePort userIdentityPort;
        private final UsernameAllocator usernameAllocator;
        private final UserProfileUpdater userProfileUpdater;

        /**
         * Signing up writes a USER row and a USER_IDENTITY row. Either both land or neither does:
         * a committed USER with no identity is unreachable by any login and permanently consumes
         * its username, since {@link UsernameAllocator} will not issue it again.
         *
         * <p>The boundary sits here rather than on the calling use case so that OAuth token
         * verification, which can reach the identity provider over the network, runs outside it.
         */
        @Transactional
        public User loginOrSignUp(UserLoginOrSignUpCommand command) {

                LocalDateTime loginAt = LocalDateTime.now();

                // Every field on this command was read out of an id token whose signature, issuer,
                // audience and expiry OidcIdTokenVerifierAdapter checked. The TODO that stood here
                // asked for entry-point validation of these strings; validating a claim the caller
                // supplied was never going to be enough, so the claim is gone instead.
                return userIdentityPort.findByProvider(command.providerName(), command.providerSub())
                                .map(identity -> signin(identity, command, loginAt))
                                .orElseGet(() -> signinWithSignUp(command, loginAt));
        }

        private User signin(UserIdentity identity,
                        UserLoginOrSignUpCommand command,
                        LocalDateTime loginAt) {

                User user = userRepository.findById(identity.getUserId())
                                .orElseThrow(() -> new IllegalStateException("User not found for identity"));

                User persistedUser = persistUserWithUpdates(user, command.email(), command.name(),
                                command.avatarUrl(),
                                loginAt);
                UserIdentity updatedIdentity = userProfileUpdater.updateIdentityIfChanged(identity, command.email(),
                                command.emailVerified(), command.name(), command.avatarUrl());
                if (updatedIdentity != identity) {
                        userIdentityPort.save(updatedIdentity);
                }

                return persistedUser;
        }

        private User signinWithSignUp(UserLoginOrSignUpCommand command,
                        LocalDateTime loginAt) {

                User user = findOrCreateUserForIdentity(command.email(), command.emailVerified(),
                                command.name(), command.avatarUrl(),
                                command.providerName(), command.providerSub());
                User persisted = persistUserWithUpdates(user, command.email(), command.name(),
                                command.avatarUrl(),
                                loginAt);

                UserIdentity identity = UserIdentity.create(
                                persisted.getId(),
                                command.providerName(),
                                command.providerSub(),
                                command.email(),
                                command.emailVerified(),
                                command.name(),
                                command.avatarUrl());
                userIdentityPort.save(identity);

                return persisted;
        }

        /**
         * Attaches this provider identity to an existing account only when the provider says it
         * verified the address.
         *
         * <p>The lookup by email is how a person who signed in with one provider keeps one account
         * when they later sign in with another. It is also how an account is taken over, if the
         * address behind it was never checked: whoever can assert {@code someone@example.com} at any
         * configured provider becomes that account. {@code emailVerified} arrived on the command
         * from the beginning and was never read, so the check cost nothing to add and the absence of
         * it cost everything.
         *
         * <p>Unverified means a new account, not a rejection. The person may well be who they say
         * they are; we simply have nothing that says so, and two accounts are recoverable where a
         * wrong merge is not.
         */
        private User findOrCreateUserForIdentity(String email,
                        boolean emailVerified,
                        String name,
                        String avatarUrl,
                        String providerName,
                        String providerSub) {
                return findExistingUserByVerifiedEmail(email, emailVerified)
                                .orElseGet(() -> createPendingUser(email, name, avatarUrl, providerName, providerSub));
        }

        private Optional<User> findExistingUserByVerifiedEmail(String email, boolean emailVerified) {
                if (!emailVerified) {
                        return Optional.empty();
                }
                if (email == null || email.isBlank()) {
                        return Optional.empty();
                }
                return userRepository.findByEmail(email.trim());
        }

        private User createPendingUser(String email,
                        String name,
                        String avatarUrl,
                        String providerName,
                        String providerSub) {

                String baseUsername = usernameAllocator.deriveBaseUsername(email, providerName, providerSub);
                String username = usernameAllocator.allocateUniqueUsername(baseUsername, providerSub);
                return User.createWithStatus(username, email, name, avatarUrl, UserStatus.PENDING);
        }

        private User persistUserWithUpdates(User user,
                        String email,
                        String name,
                        String avatarUrl,
                        LocalDateTime loginAt) {
                User updatedUser = userProfileUpdater.applyUserUpdates(user, email, name, avatarUrl, loginAt);
                return userRepository.save(updatedUser);
        }

}
