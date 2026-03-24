package io.jgitkins.server.application.support;

import io.jgitkins.server.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserIdentity;
import io.jgitkins.server.domain.model.UserStatus;
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

                return userIdentityPort.findByProvider(command.getProviderName(), command.getProviderSub())
                                .map(identity -> signin(identity, command, loginAt))
                                .orElseGet(() -> signinWithSignUp(command, loginAt));
        }

        private User signin(UserIdentity identity,
                        UserLoginOrSignUpCommand command,
                        LocalDateTime loginAt) {

                User user = userPort.findById(identity.getUserId())
                                .orElseThrow(() -> new IllegalStateException("User not found for identity"));

                User persistedUser = persistUserWithUpdates(user, command.getEmail(), command.getName(),
                                command.getAvatarUrl(),
                                loginAt);
                UserIdentity updatedIdentity = userProfileUpdater.updateIdentityIfChanged(identity, command.getEmail(),
                                command.isEmailVerified(), command.getName(), command.getAvatarUrl());
                if (updatedIdentity != identity) {
                        userIdentityPort.save(updatedIdentity);
                }

                return persistedUser;
        }

        private User signinWithSignUp(UserLoginOrSignUpCommand command,
                        LocalDateTime loginAt) {

                User user = findOrCreateUserForIdentity(command.getEmail(), command.getName(), command.getAvatarUrl(),
                                command.getProviderName(), command.getProviderSub());
                User persisted = persistUserWithUpdates(user, command.getEmail(), command.getName(),
                                command.getAvatarUrl(),
                                loginAt);

                UserIdentity identity = UserIdentity.create(
                                persisted.getId(),
                                command.getProviderName(),
                                command.getProviderSub(),
                                command.getEmail(),
                                command.isEmailVerified(),
                                command.getName(),
                                command.getAvatarUrl());
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
