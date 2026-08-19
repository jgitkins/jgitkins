package io.jgitkins.server.execution.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.execution.domain.exception.RunnerTokenMismatchException;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import org.junit.jupiter.api.Test;

class RunnerTest {
    @Test void create_generatesOfflineRunnerWithScope() {
        Runner runner = Runner.create(" worker ", RunnerScopeType.GLOBAL, null);
        assertThat(runner.getDescription()).isEqualTo("worker");
        assertThat(runner.getToken()).startsWith("RNR-");
        assertThat(runner.getStatus()).isEqualTo(RunnerStatus.OFFLINE);
    }
    @Test void activate_requiresMatchingTokenAndSetsOnline() {
        Runner runner = Runner.create("worker", RunnerScopeType.GLOBAL, null);
        assertThat(runner.activate(runner.getToken(), " 127.0.0.1 ").getStatus()).isEqualTo(RunnerStatus.ONLINE);
        assertThatThrownBy(() -> runner.activate("wrong", "127.0.0.1")).isInstanceOf(RunnerTokenMismatchException.class);
    }
}
