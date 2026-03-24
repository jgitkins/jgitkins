package io.jgitkins.server.infrastructure.config.git.hook.fetch;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.AdvertiseRefsHook;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;

/**
 * AdvertiseRefsHook that logs key request information when a fetch happens.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAdvertiseRefsHook implements AdvertiseRefsHook {

    private final HttpServletRequest request;

    // when fetch
    @Override
    public void advertiseRefs(UploadPack uploadPack) {
        String repositoryPath = uploadPack.getRepository().getDirectory().getPath();
        log.info("$$$ AdvertiseRefsHook[Fetch] ! clientSID: [{}], repositoryPath: [{}]", uploadPack.getClientSID(), repositoryPath);
        log.info("$$$ AdvertiseRefsHook[Fetch] ! discovery!: client: [{}], repo: [{}]", request.getRemoteAddr(), request.getRequestURI());
    }


    // when push
    @Override
    public void advertiseRefs(ReceivePack receivePack) {
        log.info("$$$ AdvertiseRefsHook[Push] ! receivePack: {}", receivePack);
    }
}
