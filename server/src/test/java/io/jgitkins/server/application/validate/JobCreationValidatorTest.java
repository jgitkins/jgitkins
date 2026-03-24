package io.jgitkins.server.application.validate;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobCreationDecision;
import org.junit.jupiter.api.Test;

class JobCreationValidatorTest {

    private final JobCreationValidator validator = new JobCreationValidator();

    @Test
    void validate_returnsSkipWhenBranchDeleted() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .branchDeleted(true)
                .build();

        JobCreationDecision result = validator.validate(command);

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.reason()).isEqualTo("branch deleted");
    }

    @Test
    void validate_returnsSkipWhenCommitHashMissing() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .branchName("main")
                .triggeredBy(1L)
                .build();

        JobCreationDecision result = validator.validate(command);

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.reason()).isEqualTo("missing commit hash");
    }

    @Test
    void validate_returnsCreateWhenCommandIsValid() {
        PushEventCommand command = PushEventCommand.builder()
                .repositoryId(9L)
                .branchName("main")
                .commitHash("abc")
                .triggeredBy(1L)
                .build();

        JobCreationDecision result = validator.validate(command);

        assertThat(result.isSkipped()).isFalse();
        assertThat(result.reason()).isNull();
    }
}
