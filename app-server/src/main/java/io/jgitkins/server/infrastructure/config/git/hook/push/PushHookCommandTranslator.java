package io.jgitkins.server.infrastructure.config.git.hook.push;

import io.jgitkins.server.application.dto.command.PushHookRequest;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.springframework.stereotype.Component;

@Component
public class PushHookCommandTranslator {

    public Optional<PushHookRequest> translate(String gitDirPath, Long triggeredBy, Collection<ReceiveCommand> commands) {
        Optional<ReceiveCommand> lastCommand = commands.stream()
                .filter(cmd -> cmd.getRefName().startsWith(Constants.R_HEADS))
                .reduce((first, second) -> second);

        if (lastCommand.isEmpty()) {
            return Optional.empty();
        }

        ReceiveCommand command = lastCommand.get();
        String branchName = extractBranchName(command.getRefName()).orElse(null);
        if (branchName == null) {
            return Optional.empty();
        }

        return Optional.of(new PushHookRequest(
                gitDirPath,
                triggeredBy,
                branchName,
                command.getType() == ReceiveCommand.Type.CREATE,
                command.getType() == ReceiveCommand.Type.DELETE,
                command.getNewId() != null ? command.getNewId().getName() : null
        ));
    }

    private Optional<String> extractBranchName(String refName) {
        if (refName == null || !refName.startsWith(Constants.R_HEADS)) {
            return Optional.empty();
        }
        return Optional.of(refName.substring(Constants.R_HEADS.length()));
    }
}
