package io.jgitkins.server.infrastructure.config.git;

import io.jgitkins.server.infrastructure.config.git.hook.push.PushHook;
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

        // 브랜치 신규 생성 Listener (브랜치 관리 가능)
        rp.setPostReceiveHook(pushHook);

        return rp;

    }
}
