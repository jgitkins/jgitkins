package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import io.jgitkins.server.execution.adapter.out.persistence.JobRepositoryAdapter;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobPersistenceSelectorConfiguration;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobPersistenceSelectorConfiguration.ExecutionJobPersistenceSelection;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Selection behaviour for the execution-job capability.
 *
 * <p>This capability is deliberately narrower than its context: {@code JobDispatchQueryAdapter} reads
 * the same two tables but migrates separately in task 2.76. The assertion that this selector governs
 * {@code JobRepository} and nothing else is therefore part of the contract, not an omission — it is
 * what keeps 2.76 free to move the dispatch query without disturbing the write path.
 */
class ExecutionJpaSelectorTest {

    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "execution-job-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, ExecutionJobPersistenceSelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExecutionJobPersistenceSelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(JobRepository.class)).isInstanceOf(JobRepositoryAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void selectsTheJpaWritePathWhenAsked() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobRepository.class)).isInstanceOf(JobJpaRepositoryAdapter.class);
            assertThat(context).doesNotHaveBean(JobRepositoryAdapter.class);
        });
    }

    static class Stubs {
        @Bean
        JobEntityMbgMapper jobEntityMbgMapper() {
            return Mockito.mock(JobEntityMbgMapper.class);
        }

        @Bean
        JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper() {
            return Mockito.mock(JobHistoryEntityMbgMapper.class);
        }

        @Bean
        JobDomainMapper jobDomainMapper() {
            return Mockito.mock(JobDomainMapper.class);
        }

        @Bean
        JobJpaRepository jobJpaRepository() {
            return Mockito.mock(JobJpaRepository.class);
        }

        @Bean
        JobHistoryJpaRepository jobHistoryJpaRepository() {
            return Mockito.mock(JobHistoryJpaRepository.class);
        }
    }
}
