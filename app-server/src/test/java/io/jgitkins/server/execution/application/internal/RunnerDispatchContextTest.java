package io.jgitkins.server.execution.application.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.external.RunnerDispatchContext;

class RunnerDispatchContextTest {

    @Test
    void constructor_throws_whenDispatchScopeMissing() {
        assertThatThrownBy(() -> new RunnerDispatchContext(7L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dispatchScope is required");
    }

    @Test
    void constructor_throws_whenScopedDispatchHasNoTarget() {
        assertThatThrownBy(() -> new RunnerDispatchContext(7L, JobDispatchScope.REPOSITORY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scopeTargetId is required for scoped dispatch");
    }
}
