package io.jgitkins.server.common.infrastructure.config.git;

import io.jgitkins.server.common.infrastructure.config.git.hook.push.PushHook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushEventReceivePackFactory implements ReceivePackFactory<HttpServletRequest> {

    private final PushHook pushHook;
    private final GitSmartHttpAuthorizer gitSmartHttpAuthorizer;

    @Override
    public ReceivePack create(HttpServletRequest req, Repository db)
            throws ServiceNotEnabledException, ServiceNotAuthorizedException {
        gitSmartHttpAuthorizer.authorizeWrite(req);
        ReceivePack rp = new ReceivePack(db);
        rp.setPostReceiveHook(pushHook);
        return rp;
    }
}
