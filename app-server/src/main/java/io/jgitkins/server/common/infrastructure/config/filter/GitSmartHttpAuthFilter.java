package io.jgitkins.server.common.infrastructure.config.filter;

import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.common.infrastructure.config.git.GitSmartHttpEvent;
import io.jgitkins.server.common.infrastructure.config.git.GitSmartHttpEventParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Challenges git smart-HTTP requests that need credentials.
 *
 * <p>Not a {@code @Component}. {@link io.jgitkins.server.common.infrastructure.config.security.SecurityConfig}
 * constructs it and places it inside the git security chain with
 * {@code addFilterBefore(..., BasicAuthenticationFilter.class)}, scoped to the git URL patterns.
 * Component-annotating it made Spring Boot additionally auto-register it as a servlet filter on
 * {@code /*}, so it was mapped twice: once where it belongs and once across every request in the
 * application. {@code OncePerRequestFilter} hid the duplicate at runtime, and the stray mapping was
 * a no-op on non-git URLs, which is why nothing failed.
 */
@RequiredArgsConstructor
@Slf4j
public class GitSmartHttpAuthFilter extends OncePerRequestFilter {

    public static final String REPO_PUBLIC_ATTR = "jgitkins.repo.public";

    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        GitSmartHttpEvent repoRequest = GitSmartHttpEventParser.parse(request);
        if (repoRequest == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Boolean> isPublic = gitRepositoryAccessUseCase.resolveVisibility(
                null, repoRequest.ownerName(), repoRequest.repositoryName());

        if (isPublic.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        request.setAttribute(REPO_PUBLIC_ATTR, isPublic.get());
        boolean challengeRequired = !isPublic.get() || isReceivePackRequest(request);

        String authorization = request.getHeader("Authorization");
        if (challengeRequired && (authorization == null || authorization.isBlank())) {
            log.debug("git auth challenge: missing credentials uri=[{}] public=[{}] query=[{}]",
                    request.getRequestURI(), isPublic.get(), request.getQueryString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"JGITKINS\"");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isReceivePackRequest(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query != null && query.contains("service=git-receive-pack")) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/git-receive-pack");
    }
}
