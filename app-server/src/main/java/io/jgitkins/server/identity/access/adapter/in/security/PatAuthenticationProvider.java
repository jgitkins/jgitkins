package io.jgitkins.server.identity.access.adapter.in.security;

import io.jgitkins.server.identity.access.adapter.out.security.PatTokenAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatAuthenticationProvider implements AuthenticationProvider {

    private final PatTokenAuthenticationService patTokenAuthenticationService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawToken = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        return patTokenAuthenticationService.authenticate(username, rawToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
