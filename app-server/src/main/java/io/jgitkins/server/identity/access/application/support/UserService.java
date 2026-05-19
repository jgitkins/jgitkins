package io.jgitkins.server.identity.access.application.support;

import io.jgitkins.server.identity.access.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserService {

        private final UserPersistencePort userPort;
        private final UserIdentityPersistencePort userIdentityPort;
        private final UsernameAllocator usernameAllocator;
        private final UserProfileUpdater userProfileUpdater;

        public User loginOrSignUp(UserLoginOrSignUpCommand command) {

                LocalDateTime loginAt = LocalDateTime.now();

                // TODO: Presentation 계층으로 이관 (Validator 통해 처리하기)
                // param validation must be enforced in the entry point.

                return userIdentityPort.findByProvider(command.providerName(), command.providerSub())
                                .map(identity -> signin(identity, command, loginAt))
                                .orElseGet(() -> signinWithSignUp(command, loginAt));
        }

        private User signin(UserIdentity identity,
                        UserLoginOrSignUpCommand command,
                        LocalDateTime loginAt) {

                User user = userPort.findById(identity.getUserId())
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

                User user = findOrCreateUserForIdentity(command.email(), command.name(), command.avatarUrl(),
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

        private User findOrCreateUserForIdentity(String email,
                        String name,
                        String avatarUrl,
                        String providerName,
                        String providerSub) {
                return findExistingUserByEmail(email)
                                .orElseGet(() -> createPendingUser(email, name, avatarUrl, providerName, providerSub));
        }

        private Optional<User> findExistingUserByEmail(String email) {
                if (email == null || email.isBlank()) {
                        return Optional.empty();
                }
                return userPort.findByEmail(email.trim());
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
                return userPort.save(updatedUser);
        }

}
