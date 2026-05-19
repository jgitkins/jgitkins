package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.application.dto.result.UserCredentialSummary;
import java.util.List;

public interface UserCredentialQueryUseCase {
    List<UserCredentialSummary> getCredentials();
}
