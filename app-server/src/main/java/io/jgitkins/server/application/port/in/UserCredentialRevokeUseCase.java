package io.jgitkins.server.application.port.in;

public interface UserCredentialRevokeUseCase {
    void removeCredential(Long credentialId);
}
