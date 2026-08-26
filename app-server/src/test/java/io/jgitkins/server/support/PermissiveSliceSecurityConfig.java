package io.jgitkins.server.support;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A permit-all, CSRF-disabled filter chain for {@code @WebMvcTest} slices that need the security
 * filter chain alive.
 *
 * <p>Deliberately not annotated {@code @Configuration}: it is meant to be pulled in explicitly with
 * {@code @Import}, never picked up by a component scan.
 *
 * <p>Why a slice would need this at all. The application's own {@code SecurityConfig} is a plain
 * {@code @Configuration}, so a slice excludes it and Spring Boot's default chain applies instead -
 * which enables CSRF and requires authentication. Most controller slices here sidestep that with
 * {@code @AutoConfigureMockMvc(addFilters = false)}, but that is not available to a test that varies
 * the security context per request through {@code securityContext(...)} or {@code with(user(...))}:
 * those post-processors rely on the security filter chain to install the context, so switching the
 * filters off makes every request see whatever {@code SecurityContextHolder} happened to hold.
 *
 * <p>This mirrors what {@code SecurityConfig} does for the application - {@code csrf().disable()}
 * and {@code anyRequest().permitAll()} - so authorization stays out of the way while the context
 * plumbing keeps working. Real security behaviour is covered by the HTTP compatibility tests that
 * boot the actual application.
 */
public class PermissiveSliceSecurityConfig {

    @Bean
    SecurityFilterChain permissiveSliceFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
