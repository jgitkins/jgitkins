package io.jgitkins.server.identity.access.application.support;

import io.jgitkins.server.identity.access.application.contract.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
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
         * <p>Unverified is not a rejection of the person, but it does end this request. A second
         * account for the same address is what the shape of this method suggests and it is not
         * reachable: {@code USER.EMAIL} carries {@code UK_USERS_EMAIL}, so the insert fails on the
         * unique index and the caller gets a 500 from the persistence layer. This method used to do
         * exactly that -- the rule was right and the outcome was an opaque server error on a request
         * the server understood perfectly well.
         *
         * <p>So it answers 409 and says what to do instead. Nothing is leaked by that status which
         * the 500 did not already leak: both distinguish "this address has an account" from "it does
         * not", and reaching either requires a provider willing to mint a token asserting an address
         * it did not verify.
         */
        private User findOrCreateUserForIdentity(String email,
                        boolean emailVerified,
                        String name,
                        String avatarUrl,
                        String providerName,
                        String providerSub) {
                if (emailVerified) {
                        Optional<User> linkTarget = findExistingUserByEmail(email);
                        if (linkTarget.isPresent()) {
                                return linkTarget.get();
                        }
                } else {
                        rejectWhenAddressIsAlreadyTaken(email);
                }
                return createPendingUser(email, name, avatarUrl, providerName, providerSub);
        }

        /**
         * The lookup that decides nothing on its own.
         *
         * <p>Split from the {@code emailVerified} branch so the two questions stay separate: "may
         * this identity attach to that account" is answered by the caller, and this only finds the
         * row. Folding the flag back in here is what made the unverified path fall through to an
         * insert that could not succeed.
         */
        private Optional<User> findExistingUserByEmail(String email) {
                if (email == null || email.isBlank()) {
                        return Optional.empty();
                }
                return userRepository.findByEmail(email.trim());
        }

        private void rejectWhenAddressIsAlreadyTaken(String email) {
                if (findExistingUserByEmail(email).isEmpty()) {
                        return;
                }
                throw new ApplicationException(
                                ApplicationProblemSpec.OAUTH_EMAIL_NOT_VERIFIED_FOR_LINK,
                                "That email address already belongs to an account. Sign in with the "
                                                + "provider that account uses, or verify the address with this "
                                                + "provider first.");
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
