package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
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

    private final UserQueryPort userQueryPort;
    private final UserCredentialPersistencePort userCredentialPort;
    private final PasswordEncoder passwordEncoder;

    public Authentication authenticate(String username, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Missing token");
        }
        if (!rawToken.startsWith(PAT_PREFIX)) {
            throw new BadCredentialsException("Invalid token format");
        }

        Long userId = userQueryPort.findUserIdByUsername(username)
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
        // AuthenticatedUser, not the id as a String. @CurrentUser resolves by assignability with
        // errorOnInvalidType left at false, and GitSmartHttpAuthorizer reads the principal through
        // instanceof -- so a String principal makes an authenticated PAT request read as anonymous,
        // silently, on every route. Nothing reaches this method today (no AuthenticationManager
        // wires the provider that calls it), which is exactly why the type is fixed now: task
        // 2.127-B connects it, and a trap that only springs on the day someone else wires the
        // plumbing is the worst kind to leave behind.
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_GIT"))
        );
    }
}
