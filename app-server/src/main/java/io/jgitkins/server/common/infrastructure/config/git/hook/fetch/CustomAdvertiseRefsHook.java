package io.jgitkins.server.common.infrastructure.config.git.hook.fetch;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.AdvertiseRefsHook;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;

@Slf4j
@RequiredArgsConstructor
public class CustomAdvertiseRefsHook implements AdvertiseRefsHook {

    private final HttpServletRequest request;

    @Override
    public void advertiseRefs(UploadPack uploadPack) {
        String repositoryPath = uploadPack.getRepository().getDirectory().getPath();
        log.info("$$$ AdvertiseRefsHook[Fetch] ! clientSID: [{}], repositoryPath: [{}]",
                uploadPack.getClientSID(), repositoryPath);
        log.info("$$$ AdvertiseRefsHook[Fetch] ! discovery!: client: [{}], repo: [{}]",
                request.getRemoteAddr(), request.getRequestURI());
    }

    @Override
    public void advertiseRefs(ReceivePack receivePack) {
        log.info("$$$ AdvertiseRefsHook[Push] ! receivePack: {}", receivePack);
    }
}
