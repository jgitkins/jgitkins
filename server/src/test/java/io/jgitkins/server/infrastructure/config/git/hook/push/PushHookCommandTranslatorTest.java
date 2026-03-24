package io.jgitkins.server.infrastructure.config.git.hook.push;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.application.dto.command.PushHookRequest;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.junit.jupiter.api.Test;

class PushHookCommandTranslatorTest {

    @Test
    void translate_buildsPushHookRequestFromReceiveCommand() {
        PushHookCommandTranslator translator = new PushHookCommandTranslator();
        ReceiveCommand command = new ReceiveCommand(
                ObjectId.zeroId(),
                ObjectId.fromString("0123456789012345678901234567890123456789"),
                "refs/heads/main"
        );

        Optional<PushHookRequest> result = translator.translate("/bare/users/alice/repo.git", 7L, List.of(command));

        assertThat(result).isPresent();
        assertThat(result.get().gitDirPath()).isEqualTo("/bare/users/alice/repo.git");
        assertThat(result.get().triggeredBy()).isEqualTo(7L);
        assertThat(result.get().branchName()).isEqualTo("main");
        assertThat(result.get().commitHash()).isEqualTo("0123456789012345678901234567890123456789");
        assertThat(result.get().branchCreated()).isTrue();
    }

    @Test
    void translate_returnsEmptyWhenNoBranchCommandExists() {
        PushHookCommandTranslator translator = new PushHookCommandTranslator();
        ReceiveCommand command = new ReceiveCommand(
                ObjectId.zeroId(),
                ObjectId.fromString("0123456789012345678901234567890123456789"),
                "refs/tags/v1.0.0"
        );

        Optional<PushHookRequest> result = translator.translate("/bare/users/alice/repo.git", 7L, List.of(command));

        assertThat(result).isEmpty();
    }
}
