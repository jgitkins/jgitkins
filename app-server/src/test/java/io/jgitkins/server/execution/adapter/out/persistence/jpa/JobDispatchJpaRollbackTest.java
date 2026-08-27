package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.JobDispatchQueryAdapter;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.infrastructure.config.ExecutionJobDispatchQuerySelectorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The rollback half of the dispatch-query cutover contract.
 *
 * <p>The MyBatis XML this query was translated from is still on disk and still registered, which is what
 * makes this rollback a property change rather than a redeploy. Task 2.77 is where that XML is retired,
 * and until then a bad translation costs one restart to undo.
 */
class JobDispatchJpaRollbackTest {

    private static final String PROPERTY = PersistenceImplementationSelector
            .propertyName("app-server", "execution-job-dispatch-query-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JobDispatchJpaSelectorTest.Stubs.class,
                    ExecutionJobDispatchQuerySelectorConfiguration.class);

    @Test
    void rollsBackDispatchQueryToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(JobDispatchQueryPort.class))
                        .isInstanceOf(JobDispatchJpaQueryAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobDispatchQueryPort.class))
                    .isInstanceOf(JobDispatchQueryAdapter.class);
            assertThat(context).doesNotHaveBean(JobDispatchJpaQueryAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobDispatchQueryPort.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(JobDispatchQueryAdapter.class);
        });
    }
}
