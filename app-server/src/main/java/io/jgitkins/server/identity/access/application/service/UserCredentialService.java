package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialIssueResult;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.identity.access.application.mapper.UserCredentialApplicationMapper;

import io.jgitkins.server.identity.access.application.port.in.UserCredentialIssueUseCase;
import io.jgitkins.server.identity.access.application.port.in.UserCredentialQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.UserCredentialRevokeUseCase;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;

import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCredentialService implements UserCredentialIssueUseCase,
        UserCredentialQueryUseCase,
        UserCredentialRevokeUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final io.jgitkins.server.identity.access.application.port.out.ActiveAccountPolicyPort activeAccountPolicyPort;
    private final UserCredentialPersistencePort userCredentialPort;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialApplicationMapper userCredentialApplicationMapper;

    @Override
    @Transactional
    public UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command) {
        Long userId = activeAccountPolicyPort.requireActiveUserId();

        String token = generateToken();

        String hash = passwordEncoder.encode(token);

        UserCredential credential = UserCredential.issue(
                userId,
                command.name(),
                command.description(),
                hash);

        UserCredential saved = userCredentialPort.save(credential);

        return new UserCredentialIssueResult(saved.getId(), token);
    }

    @Override
    @Transactional
    public List<UserCredentialSummary> getCredentials() {
        Long userId = activeAccountPolicyPort.requireActiveUserId();
        return userCredentialPort.findAllByUserIdAndProvider(userId, "PAT")
                .stream()
                .map(userCredentialApplicationMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public void removeCredential(Long credentialId) {
        Long userId = activeAccountPolicyPort.requireActiveUserId();
        userCredentialPort.deleteByIdAndUserId(credentialId, userId);
    }


    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "jkpat_" + encoded;
    }
}
