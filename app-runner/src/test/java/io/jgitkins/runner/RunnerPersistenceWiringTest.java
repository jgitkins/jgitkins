package io.jgitkins.runner;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.runner.application.port.out.RunnerConfigurationPort;
import io.jgitkins.runner.infrastructure.adapter.RunnerConfigurationJpaPersistenceAdapter;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerJpaRepository;
import io.jgitkins.runner.infrastructure.translator.RunnerDomainMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * That Spring finds the runner's JPA persistence at all.
 *
 * <p>The adapter's own behaviour is covered by {@code RunnerConfigurationJpaAdapterH2Test}, which
 * wires everything by hand. Hand-wiring proves the SQL and leaves the question this test answers:
 * whether the container discovers the entities and the repository interfaces on its own. Nothing else
 * in app-runner would notice if it did not -- the module has no bootable Spring context, so a wiring
 * failure would first appear as a runner that will not start in someone's terminal.
 *
 * <p>This class lives in {@code io.jgitkins.runner} deliberately: {@link EnableAutoConfiguration}
 * derives the packages Boot scans for entities and repositories from the annotated class, so a nested
 * configuration here scans the same roots {@code JGitkinsRunnerApplication} does. Moving this test
 * into a subpackage would still pass while proving something narrower than production.
 *
 * <p>Auto-configuration runs whole rather than being narrowed to the persistence slice, because the
 * package derivation being tested is registered by {@link EnableAutoConfiguration} itself -- listing
 * the four JPA auto-configurations individually leaves no {@code AutoConfigurationPackages} entry, so
 * Hibernate finds no entities and the test passes against an empty mapping. What is left out is the
 * component scan: AMQP, gRPC and the Docker client are on the classpath, and none of the runner's own
 * components are created here, so nothing tries to reach a broker or a Docker socket.
 */
class RunnerPersistenceWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PersistenceOnly.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:runner-wiring;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.sql.init.mode=always",
                    "spring.sql.init.schema-locations=classpath:/DDL.sql",
                    "spring.jpa.hibernate.ddl-auto=none");

    @Test
    void springBuildsTheJpaAdapterAsTheRunnersConfigurationPort() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RunnerConfigurationPort.class);
            assertThat(context.getBean(RunnerConfigurationPort.class))
                    .as("the MyBatis adapter is gone; there is one implementation and no selector")
                    .isInstanceOf(RunnerConfigurationJpaPersistenceAdapter.class);
            assertThat(context).hasSingleBean(RunnerJpaRepository.class);
        });
    }

    @Test
    void theSchemaFromDdlSqlIsWhatTheRepositoriesQuery() {
        runner.run(context -> {
            // Reaching the store through the port at all proves three things at once: DDL.sql ran, the
            // repository proxy resolved against those tables, and the adapter's @Transactional method
            // found a transaction manager. With hbm2ddl at none, no table exists unless DDL.sql made it.
            assertThat(context.getBean(RunnerConfigurationPort.class).loadConfiguration())
                    .as("an un-activated runner reports nothing rather than failing to start")
                    .isEmpty();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({RunnerConfigurationJpaPersistenceAdapter.class, RunnerDomainMapperImpl.class})
    static class PersistenceOnly {
    }
}
