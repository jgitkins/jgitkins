package io.jgitkins.server.infrastructure.config.git.hook.push;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.command.PushHookRequest;
import io.jgitkins.server.application.port.in.PushEventHandleUseCase;
import io.jgitkins.server.application.port.out.PushEventRequestResolver;
import io.jgitkins.server.application.support.PushEventCommandResolver;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.springframework.stereotype.Component;

/**
 * Push-side hook that logs generic push info and branch creation events.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PushHook implements PostReceiveHook {

    private final PushEventHandleUseCase pushEventHandleUseCase;
    private final PushEventRequestResolver pushEventRequestResolver;
    private final PushHookCommandTranslator pushHookCommandTranslator;
    private final PushEventCommandResolver pushEventCommandResolver;

    @Override
    public void onPostReceive(ReceivePack receivePack, Collection<ReceiveCommand> commands) {
        String gitDirPath = receivePack.getRepository().getDirectory().getAbsolutePath();
        Long requesterId = pushEventRequestResolver.resolveRequesterId().orElse(null);
        log.debug("push event: user=[{}] bare repo path=[{}]", requesterId, gitDirPath);

        // 사실상 Presentation 계층의 DTO 가 맞음
        Optional<PushHookRequest> pushHookRequest = pushHookCommandTranslator.translate(gitDirPath, requesterId, commands);
        if (pushHookRequest.isEmpty()) {
            log.debug("push hook skipped: no applicable branch command found");
            return;
        }

        PushEventCommand pushEventCommand = pushEventCommandResolver.resolve(pushHookRequest.get());
        pushEventHandleUseCase.handle(pushEventCommand);
    }
}
