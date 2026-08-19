package io.jgitkins.server.execution.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ExecutionValueObjectsTest {
    @Test
    void idsRejectNullAndNonPositiveValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> ExecutionActorId.of(null));
        assertThatIllegalArgumentException().isThrownBy(() -> ExecutionActorId.of(0L));
        assertThatIllegalArgumentException().isThrownBy(() -> ExecutionRepositoryId.of(-1L));
    }

    @Test
    void systemActorHasStableSystemSingletonEquality() {
        assertThat(ExecutionSystemActor.of("SYSTEM")).isEqualTo(ExecutionSystemActor.SYSTEM);
        assertThatIllegalArgumentException().isThrownBy(() -> ExecutionSystemActor.of(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> ExecutionSystemActor.of(null));
    }
}
