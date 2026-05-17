package io.jgitkins.server.application.port.out;

import java.util.List;

public interface TokenIssuerPort {
    String issueToken(Long userId, List<String> roles);
}
