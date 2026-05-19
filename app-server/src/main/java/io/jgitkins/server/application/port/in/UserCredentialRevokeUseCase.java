package io.jgitkins.server.identity.access.application.port.in;

public interface UserCredentialRevokeUseCase {
    void removeCredential(Long credentialId);
}
