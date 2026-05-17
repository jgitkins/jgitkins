package io.jgitkins.server.infrastructure.config.filter;

import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.infrastructure.config.git.GitSmartHttpEvent;
import io.jgitkins.server.infrastructure.config.git.GitSmartHttpEventParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitSmartHttpAuthFilter extends OncePerRequestFilter {

    public static final String REPO_PUBLIC_ATTR = "jgitkins.repo.public";

    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    /***
     * smart http 요청 filter
     * git fetch, push 이벤트에 동작
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        // resolve repository info from fetch or push event
        GitSmartHttpEvent repoRequest = GitSmartHttpEventParser.parse(request);
        if (repoRequest == null) {
            // Non-git or malformed paths should be handled by downstream handlers.
            filterChain.doFilter(request, response);
            return;
        }

        // check visibility
        Optional<Boolean> isPublic = gitRepositoryAccessUseCase.resolveVisibility(null,
                                                                                  repoRequest.ownerName(),
                                                                                  repoRequest.repositoryName());

        // Unknown repo: let GitServlet resolve to 404 without leaking auth details.
        if (isPublic.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public repositories can be read without credentials.
        // But push negotiation(receive-pack) still requires credentials.
        request.setAttribute(REPO_PUBLIC_ATTR, isPublic.get());
        boolean challengeRequired = !isPublic.get() || isReceivePackRequest(request);

        String authorization = request.getHeader("Authorization");
        if (challengeRequired && (authorization == null || authorization.isBlank())) {
            log.debug("git auth challenge: missing credentials uri=[{}] public=[{}] query=[{}]",
                    request.getRequestURI(),
                    isPublic.get(),
                    request.getQueryString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"JGITKINS\"");
            return;
        }

        // Authorization exists: allow downstream handlers to authenticate and authorize.
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
