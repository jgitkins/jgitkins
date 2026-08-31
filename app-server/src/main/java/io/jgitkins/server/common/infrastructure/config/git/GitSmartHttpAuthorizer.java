package io.jgitkins.server.common.infrastructure.config.git;

import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitSmartHttpAuthorizer {

    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    public GitSmartHttpEvent authorizeRead(HttpServletRequest request) throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = parseRepositoryOrThrow(request, "fetch");
        // Resolved here rather than read from a request attribute. GitSmartHttpAuthFilter was the only
        // writer of that attribute and it is deleted, so the read was always null and this branch --
        // anonymous fetch of a public repository -- was unreachable. Asking the use case directly keeps
        // the rule while removing a contract between two components that no longer has one end.
        Optional<Boolean> isPublic = gitRepositoryAccessUseCase.resolveVisibility(
                null, event.ownerName(), event.repositoryName());
        if (isPublic.filter(Boolean::booleanValue).isPresent()) {
            log.info("git fetch allowed (public repository): owner=[{}] repo=[{}]",
                    event.ownerName(), event.repositoryName());
            return event;
        }

        Long userId = resolveUserIdOrThrow(request, "fetch");
        boolean allowed = gitRepositoryAccessUseCase.canRead(null, event.ownerName(), event.repositoryName(), userId);
        if (!allowed) {
            log.warn("git fetch denied: owner=[{}] repo=[{}] userId=[{}]",
                    event.ownerName(), event.repositoryName(), userId);
            throw new ServiceNotAuthorizedException("Access denied");
        }

        log.info("git fetch allowed: owner=[{}] repo=[{}] userId=[{}]",
                event.ownerName(), event.repositoryName(), userId);
        return event;
    }

    public GitSmartHttpEvent authorizeWrite(HttpServletRequest request) throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = parseRepositoryOrThrow(request, "push");
        Long userId = resolveUserIdOrThrow(request, "push");

        boolean allowed = gitRepositoryAccessUseCase.canWrite(null, event.ownerName(), event.repositoryName(), userId);
        if (!allowed) {
            log.warn("git push denied: owner=[{}] repo=[{}] userId=[{}]",
                    event.ownerName(), event.repositoryName(), userId);
            throw new ServiceNotAuthorizedException("Access denied");
        }

        log.info("git push allowed: owner=[{}] repo=[{}] userId=[{}]",
                event.ownerName(), event.repositoryName(), userId);
        return event;
    }

    private static Long authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? user.userId() : null;
    }

    private GitSmartHttpEvent parseRepositoryOrThrow(HttpServletRequest request, String action)
            throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = GitSmartHttpEventParser.parse(request);
        if (event == null) {
            log.warn("git {} denied: invalid repository path uri=[{}]", action, request.getRequestURI());
            throw new ServiceNotAuthorizedException("Invalid repository path");
        }
        return event;
    }

    /**
     * The requester, from the security context.
     *
     * <p>It used to come from {@code GitRequestAuthSupport}, which read a client-supplied
     * {@code X-User-Id} header and returned whatever number was in it. No reverse proxy anywhere in
     * this repository sets that header, so the header was the client's own claim about who it was,
     * and it was the only identity git authorization ever had. That class is deleted.
     *
     * <p>Reads the same {@code AuthenticatedUser} the API chain establishes, so there is one identity
     * representation rather than two. Nothing populates it on this chain yet: the git chain denies
     * every request and no servlet serves its paths, so this method is unreached. It is written in the
     * correct shape anyway — the next person here should find a requester source that is right, not
     * one that compiles.
     */
    private Long resolveUserIdOrThrow(HttpServletRequest request, String action) throws ServiceNotAuthorizedException {
        log.debug("git {} auth check. uri=[{}] query=[{}]", action, request.getRequestURI(), request.getQueryString());
        Long userId = authenticatedUserId();
        if (userId == null) {
            log.warn("git {} denied: unauthenticated request uri=[{}] query=[{}]",
                    action, request.getRequestURI(), request.getQueryString());
            throw new ServiceNotAuthorizedException("Unauthenticated");
        }
        return userId;
    }
}
