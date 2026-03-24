package io.jgitkins.server.infrastructure.config.security.auth;

import io.jgitkins.server.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.UserCredential;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatTokenAuthenticationService {

    private static final String PAT_PREFIX = "jkpat_";
    private static final String PROVIDER_PAT = "PAT";

    private final UserPersistencePort userPort;
    private final UserCredentialPersistencePort userCredentialPort;
    private final PasswordEncoder passwordEncoder;

    public Authentication authenticate(String username, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Missing token");
        }
        if (!rawToken.startsWith(PAT_PREFIX)) {
            throw new BadCredentialsException("Invalid token format");
        }

        Long userId = userPort.findUserIdByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<UserCredential> credentials = userCredentialPort.findAllByUserIdAndProvider(userId, PROVIDER_PAT);
        if (credentials.isEmpty()) {
            throw new BadCredentialsException("Token not registered");
        }

        boolean matched = credentials.stream()
                .anyMatch(credential -> passwordEncoder.matches(rawToken, credential.getPasswordHash()));
        if (!matched) {
            throw new BadCredentialsException("Invalid token");
        }

        log.info("Authenticated user: [{}]", username);
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(userId),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_GIT"))
        );
    }
}
