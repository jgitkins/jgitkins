package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.JobRepositoryAdapter;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobPersistenceSelectorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The rollback half of the execution-job cutover contract.
 *
 * <p>This is the slice where rollback matters most: the write path carries the compare-and-append that
 * keeps a job from being dispatched twice. An operator who sees double dispatch in production needs the
 * reset to work on the first try and by either route — setting the property back, or deleting it.
 */
class ExecutionJpaRollbackTest {

    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "execution-job-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ExecutionJpaSelectorTest.Stubs.class,
                    ExecutionJobPersistenceSelectorConfiguration.class);

    @Test
    void rollsBackExecutionToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(JobRepository.class)).isInstanceOf(JobJpaRepositoryAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobRepository.class)).isInstanceOf(JobRepositoryAdapter.class);
            assertThat(context).doesNotHaveBean(JobJpaRepositoryAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(JobRepositoryAdapter.class);
        });
    }
}
