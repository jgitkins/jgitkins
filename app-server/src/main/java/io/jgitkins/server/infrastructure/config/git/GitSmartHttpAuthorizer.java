package io.jgitkins.server.infrastructure.config.git;

import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.infrastructure.config.filter.GitSmartHttpAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitSmartHttpAuthorizer {

    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;
    private final GitRequestAuthSupport requestAuthSupport;

    // 요청에 대해 해당 요청이 특정 저장소에 대해 읽기가 가능한지 확인
    public GitSmartHttpEvent authorizeRead(HttpServletRequest request) throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = parseRepositoryOrThrow(request, "fetch");
        Object publicAttr = request.getAttribute(GitSmartHttpAuthFilter.REPO_PUBLIC_ATTR);
        if (Boolean.TRUE.equals(publicAttr)) {
            log.info("git fetch allowed (public repository): owner=[{}] repo=[{}]",
                    event.ownerName(), event.repositoryName());
            return event;
        }

        Long userId = resolveUserIdOrThrow(request, "fetch");
        boolean allowed = gitRepositoryAccessUseCase.canRead(
                null, event.ownerName(), event.repositoryName(), userId);
        if (!allowed) {
            log.warn("git fetch denied: owner=[{}] repo=[{}] userId=[{}]",
                    event.ownerName(), event.repositoryName(), userId);
            throw new ServiceNotAuthorizedException("Access denied");
        }

        log.info("git fetch allowed: owner=[{}] repo=[{}] userId=[{}]",
                event.ownerName(), event.repositoryName(), userId);
        return event;
    }

    // 요청에 대해 해당 요청이 특정 저장소에 대해 쓰기가 가능한지 확인
    public GitSmartHttpEvent authorizeWrite(HttpServletRequest request) throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = parseRepositoryOrThrow(request, "push");
        Long userId = resolveUserIdOrThrow(request, "push");

        boolean allowed = gitRepositoryAccessUseCase.canWrite(
                null, event.ownerName(), event.repositoryName(), userId);
        if (!allowed) {
            log.warn("git push denied: owner=[{}] repo=[{}] userId=[{}]",
                    event.ownerName(), event.repositoryName(), userId);
            throw new ServiceNotAuthorizedException("Access denied");
        }

        log.info("git push allowed: owner=[{}] repo=[{}] userId=[{}]",
                event.ownerName(), event.repositoryName(), userId);
        return event;
    }

    private GitSmartHttpEvent parseRepositoryOrThrow(HttpServletRequest request,
                                                     String action) throws ServiceNotAuthorizedException {
        GitSmartHttpEvent event = GitSmartHttpEventParser.parse(request);
        if (event == null) {
            log.warn("git {} denied: invalid repository path uri=[{}]", action, request.getRequestURI());
            throw new ServiceNotAuthorizedException("Invalid repository path");
        }
        return event;
    }

    private Long resolveUserIdOrThrow(HttpServletRequest request,
                                      String action) throws ServiceNotAuthorizedException {
        log.debug("git {} auth check. uri=[{}] query=[{}]",
                action, request.getRequestURI(), request.getQueryString());
        Long userId = requestAuthSupport.resolveUserId(request);
        if (userId == null) {
            log.warn("git {} denied: unauthenticated request uri=[{}] query=[{}]",
                    action, request.getRequestURI(), request.getQueryString());
            throw new ServiceNotAuthorizedException("Unauthenticated");
        }
        return userId;
    }
}
