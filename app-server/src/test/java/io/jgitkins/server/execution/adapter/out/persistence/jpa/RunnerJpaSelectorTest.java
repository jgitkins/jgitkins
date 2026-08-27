package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import io.jgitkins.server.execution.adapter.out.persistence.RunnerPersistenceAdapter;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.infrastructure.config.ExecutionRunnerPersistenceSelectorConfiguration;
import io.jgitkins.server.execution.infrastructure.config.ExecutionRunnerPersistenceSelectorConfiguration.ExecutionRunnerPersistenceSelection;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerAssignmentDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.mapper.RunnerAssignmentEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.mapper.RunnerEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Selection behaviour for the execution-runner capability.
 *
 * <p>Deliberately a different property from the execution-job selector, even though both live in the
 * execution context. Runners and jobs share no table, so a runner-registration regression must be
 * rollback-able without also reverting the job write path that carries the dispatch lock. The test
 * asserts the independence: setting the runner selector must not disturb anything else.
 */
class RunnerJpaSelectorTest {

    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "execution-runner-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, ExecutionRunnerPersistenceSelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExecutionRunnerPersistenceSelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(RunnerRepository.class)).isInstanceOf(RunnerPersistenceAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void isIndependentOfTheExecutionJobSelector() {
        // The job selector's property must not move the runner adapter, and vice versa. Sharing one
        // property would make a runner rollback also revert the job write path.
        String jobProperty = PersistenceImplementationSelector
                .propertyName("app-server", "execution-job-reference");

        runner.withPropertyValues(jobProperty + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RunnerRepository.class))
                    .as("the job selector must not move the runner adapter")
                    .isInstanceOf(RunnerPersistenceAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(RunnerRepository.class))
                        .isInstanceOf(RunnerJpaPersistenceAdapter.class));
    }

    static class Stubs {
        @Bean
        RunnerEntityMbgMapper runnerEntityMbgMapper() {
            return Mockito.mock(RunnerEntityMbgMapper.class);
        }

        @Bean
        RunnerAssignmentEntityMbgMapper runnerAssignmentEntityMbgMapper() {
            return Mockito.mock(RunnerAssignmentEntityMbgMapper.class);
        }

        @Bean
        RunnerDomainMapper runnerDomainMapper() {
            return Mockito.mock(RunnerDomainMapper.class);
        }

        @Bean
        RunnerAssignmentDomainMapper runnerAssignmentDomainMapper() {
            return Mockito.mock(RunnerAssignmentDomainMapper.class);
        }

        @Bean
        RunnerJpaRepository runnerJpaRepository() {
            return Mockito.mock(RunnerJpaRepository.class);
        }

        @Bean
        RunnerAssignmentJpaRepository runnerAssignmentJpaRepository() {
            return Mockito.mock(RunnerAssignmentJpaRepository.class);
        }
    }
}
