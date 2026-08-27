package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.RunnerPersistenceAdapter;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.infrastructure.config.ExecutionRunnerPersistenceSelectorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** The rollback half of the execution-runner cutover contract. */
class RunnerJpaRollbackTest {

    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "execution-runner-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RunnerJpaSelectorTest.Stubs.class,
                    ExecutionRunnerPersistenceSelectorConfiguration.class);

    @Test
    void rollsBackRunnerToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(RunnerRepository.class))
                        .isInstanceOf(RunnerJpaPersistenceAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RunnerRepository.class)).isInstanceOf(RunnerPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(RunnerJpaPersistenceAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RunnerRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(RunnerPersistenceAdapter.class);
        });
    }
}
