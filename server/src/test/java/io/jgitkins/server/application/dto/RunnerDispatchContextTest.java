package io.jgitkins.server.application.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

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
