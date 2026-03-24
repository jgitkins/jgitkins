package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.application.dto.result.UserCredentialIssueResult;
import io.jgitkins.server.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.application.mapper.UserCredentialApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.in.UserCredentialIssueUseCase;
import io.jgitkins.server.application.port.in.UserCredentialQueryUseCase;
import io.jgitkins.server.application.port.in.UserCredentialRevokeUseCase;
import io.jgitkins.server.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.model.UserCredential;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCredentialService implements UserCredentialIssueUseCase,
        UserCredentialQueryUseCase,
        UserCredentialRevokeUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final CurrentUserPort currentUserPersistencePort;
    private final UserCredentialPersistencePort userCredentialPort;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialApplicationMapper userCredentialApplicationMapper;

    @Override
    public UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command) {
        Long userId = currentUserId();

        String token = generateToken();

        String hash = passwordEncoder.encode(token);

        UserCredential credential = UserCredential.issue(
                userId,
                command.getName(),
                command.getDescription(),
                hash);

        UserCredential saved = userCredentialPort.save(credential);

        return new UserCredentialIssueResult(saved.getId(), token);
    }

    @Override
    public List<UserCredentialSummary> getCredentials() {
        Long userId = currentUserId();
        return userCredentialPort.findAllByUserIdAndProvider(userId, "PAT")
                .stream()
                .map(userCredentialApplicationMapper::toSummary)
                .toList();
    }

    @Override
    public void removeCredential(Long credentialId) {
        Long userId = currentUserId();
        userCredentialPort.deleteByIdAndUserId(credentialId, userId);
    }

    private Long currentUserId() {
        return currentUserPersistencePort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.UNAUTHENTICATED,
                        "Unauthenticated"));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "jkpat_" + encoded;
    }
}
