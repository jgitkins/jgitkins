package io.jgitkins.server.infrastructure.config.git;

import io.jgitkins.server.infrastructure.config.git.hook.fetch.CustomAdvertiseRefsHook;
import io.jgitkins.server.infrastructure.config.git.hook.fetch.CustomPostUploadHook;
import io.jgitkins.server.infrastructure.config.git.hook.fetch.CustomPreUploadHook;
import io.jgitkins.server.infrastructure.config.git.hook.fetch.RefLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.transport.resolver.UploadPackFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FetchEventUploadPackFactory implements UploadPackFactory<HttpServletRequest> {

    private final GitSmartHttpAuthorizer gitSmartHttpAuthorizer;

    /***
     * Fetch Event Listener
     */
    @Override
    public UploadPack create(HttpServletRequest req, Repository db) throws ServiceNotEnabledException, ServiceNotAuthorizedException {
        gitSmartHttpAuthorizer.authorizeRead(req);

        UploadPack up = new UploadPack(db);

        up.setAdvertiseRefsHook(new CustomAdvertiseRefsHook(req)); // 클라이언트 요청 로깅 request ip, which repo ...
        up.setRefFilter(new RefLogger()); // 서버가 클라이언트에 제공할 정보 로깅 (Refs)
        up.setPreUploadHook(new CustomPreUploadHook()); // 사용자의 Fetching Packfile 요청에 대한 로깅 (사용자가 요청한 wants, haves || 그래서 어떤 Packfile 구성이 도출되었는지)
        up.setPostUploadHook(new CustomPostUploadHook());

        return up;
    }
}
