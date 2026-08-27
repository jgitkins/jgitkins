package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import io.jgitkins.server.execution.adapter.out.persistence.JobDispatchQueryAdapter;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobDispatchQuerySelectorConfiguration;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobDispatchQuerySelectorConfiguration.ExecutionJobDispatchQuerySelection;
import io.jgitkins.server.execution.adapter.out.persistence.support.JobDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.mapper.JobDispatchQueryMapper;
import io.jgitkins.server.execution.adapter.out.persistence.mapper.JobHistoryEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Selection behaviour for the job-dispatch query capability.
 *
 * <p>Separate from the execution-job write selector on purpose. The two have different failure modes —
 * a bad projection translation returns the wrong job, a bad write translation loses a transition — and
 * an operator should be able to roll back the one that actually broke. The independence is asserted, not
 * assumed, because the two capabilities live in the same context and sharing one property would look
 * like a simplification.
 */
class JobDispatchJpaSelectorTest {

    private static final String PROPERTY = PersistenceImplementationSelector
            .propertyName("app-server", "execution-job-dispatch-query-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, ExecutionJobDispatchQuerySelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExecutionJobDispatchQuerySelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(JobDispatchQueryPort.class))
                    .isInstanceOf(JobDispatchQueryAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void selectsTheJpaQueryWhenAsked() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobDispatchQueryPort.class))
                    .isInstanceOf(JobDispatchJpaQueryAdapter.class);
            assertThat(context).doesNotHaveBean(JobDispatchQueryAdapter.class);
        });
    }

    @Test
    void isIndependentOfTheExecutionJobWriteSelector() {
        String writeProperty = PersistenceImplementationSelector
                .propertyName("app-server", "execution-job-reference");

        runner.withPropertyValues(writeProperty + "=jpa").run(context ->
                assertThat(context.getBean(JobDispatchQueryPort.class))
                        .as("the write selector must not move the dispatch query; the two failures are "
                                + "different and each must be rollback-able on its own")
                        .isInstanceOf(JobDispatchQueryAdapter.class));
    }

    static class Stubs {
        @Bean
        JobDispatchQueryMapper jobDispatchQueryMapper() {
            return Mockito.mock(JobDispatchQueryMapper.class);
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
        JobDispatchJpaRepository jobDispatchJpaRepository() {
            return Mockito.mock(JobDispatchJpaRepository.class);
        }

        @Bean
        JobHistoryJpaRepository jobHistoryJpaRepository() {
            return Mockito.mock(JobHistoryJpaRepository.class);
        }
    }
}
