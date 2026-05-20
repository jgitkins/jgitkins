package io.jgitkins.server.common.infrastructure.config.git;

import io.jgitkins.server.common.infrastructure.config.git.hook.fetch.CustomAdvertiseRefsHook;
import io.jgitkins.server.common.infrastructure.config.git.hook.fetch.CustomPostUploadHook;
import io.jgitkins.server.common.infrastructure.config.git.hook.fetch.CustomPreUploadHook;
import io.jgitkins.server.common.infrastructure.config.git.hook.fetch.RefLogger;
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

    @Override
    public UploadPack create(HttpServletRequest req, Repository db)
            throws ServiceNotEnabledException, ServiceNotAuthorizedException {
        gitSmartHttpAuthorizer.authorizeRead(req);

        UploadPack up = new UploadPack(db);
        up.setAdvertiseRefsHook(new CustomAdvertiseRefsHook(req));
        up.setRefFilter(new RefLogger());
        up.setPreUploadHook(new CustomPreUploadHook());
        up.setPostUploadHook(new CustomPostUploadHook());
        return up;
    }
}
