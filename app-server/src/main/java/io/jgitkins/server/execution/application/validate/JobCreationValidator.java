package io.jgitkins.server.execution.application.validate;

import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.internal.JobCreationDecision;
import org.springframework.stereotype.Component;

@Component
public class JobCreationValidator {

    public JobCreationDecision validate(PushEventCommand command) {
        if (command.getRepositoryId() == null) {
            return JobCreationDecision.skip("missing repository id");
        }

        if (command.isBranchDeleted()) {
            return JobCreationDecision.skip("branch deleted");
        }

        if (command.getCommitHash() == null || command.getCommitHash().isBlank()) {
            return JobCreationDecision.skip("missing commit hash");
        }

        if (command.getTriggeredBy() == null) {
            return JobCreationDecision.skip("missing triggered by");
        }

        return JobCreationDecision.create();
    }
}
