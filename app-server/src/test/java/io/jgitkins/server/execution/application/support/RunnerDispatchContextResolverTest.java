package io.jgitkins.server.execution.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunnerDispatchContextResolverTest {
    @Mock RunnerRepository repository;
    @Test void resolve_rejectsBlankTokenWithoutRepositoryLookup() {
        assertThat(new RunnerDispatchContextResolver(repository).resolve(" ")).isEmpty();
    }
    @Test void resolve_mapsRunnerScope() {
        Runner runner = Runner.create("worker", RunnerScopeType.REPOSITORY, 10L).withId(7L);
        when(repository.findByToken(runner.getToken())).thenReturn(Optional.of(runner));
        var context = new RunnerDispatchContextResolver(repository).resolve(runner.getToken());
        assertThat(context).get().extracting("runnerId", "scopeTargetId").containsExactly(7L, 10L);
    }
}
