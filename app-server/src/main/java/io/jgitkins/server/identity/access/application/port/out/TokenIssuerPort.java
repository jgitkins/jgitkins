package io.jgitkins.server.identity.access.application.port.out;

import java.util.List;

public interface TokenIssuerPort {
    String issueToken(Long userId, List<String> roles);
}
