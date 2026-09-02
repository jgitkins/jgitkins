package io.jgitkins.runner.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.domain.RunnerExecutionConfig;
import io.jgitkins.runner.domain.RunnerRuntimeConfig;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigFileJpaRepository;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigJpaRepository;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerJpaRepository;
import io.jgitkins.runner.infrastructure.translator.RunnerDomainMapperImpl;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What the runner remembers about itself across a restart.
 *
 * <p>First test in this module to touch persistence at all. The MyBatis adapter it replaces had none,
 * which is how the load path came to read one table and the save path write two without anyone
 * noticing -- see {@code restoresTheExecutionSettingsTheMyBatisPathLostOnRestart}, which is the case
 * that would have failed against the old adapter.
 *
 * <p>Runs against a real in-memory H2 with the runner's own {@code DDL.sql} applied, because the point
 * of every assertion here is what the database does with the columns: {@code NOT NULL} on
 * {@code RUNNER.NAME}, the generated {@code ID} the save path reads back before writing the child
 * rows, and the {@code (RUNNER_ID, CONFIG_KEY)} uniqueness the upsert relies on. Mocked repositories
 * would assert the adapter calls methods, which is the thing that was never in doubt.
 *
 * <p>No Spring context. app-runner's test sources contain a {@code @Component @Primary} test double
 * named {@code JobRunnerPortTest} that would be picked up by any component scan, so the wiring is
 * done by hand -- three lines -- rather than dragging that in. The EntityManagerFactory is built
 * per-test with {@code hibernate.hbm2ddl.auto=none} so the schema under test is the one the runner
 * actually ships, and the repositories come from {@link SharedEntityManagerCreator} so they join the
 * surrounding transaction instead of quietly running outside it.
 */
class RunnerConfigurationJpaAdapterH2Test {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private RunnerConfigurationJpaPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:runner-" + Long.toString(System.nanoTime(), 36) + ";DB_CLOSE_DELAY=-1",
                "runner", "runner");
        dataSource.setDriverClassName("org.h2.Driver");

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("DDL.sql"));
        populator.execute(dataSource);

        jdbc = new JdbcTemplate(dataSource);

        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("io.jgitkins.runner.infrastructure.persistence.jpa");
        factoryBean.setPersistenceUnitName("runner-configuration");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factoryBean.afterPropertiesSet();
        EntityManagerFactory emf = factoryBean.getObject();

        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        adapter = new RunnerConfigurationJpaPersistenceAdapter(
                repository(emf, RunnerJpaRepository.class),
                repository(emf, RunnerConfigJpaRepository.class),
                repository(emf, RunnerConfigFileJpaRepository.class),
                new RunnerDomainMapperImpl());
    }

    @AfterEach
    void tearDown() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void loadsNothingBeforeTheRunnerHasEverBeenActivated() {
        // RunnerInitService reads this on startup and leaves the scheduler idle on an empty answer.
        // Anything other than empty here would start a runner with no server to talk to.
        assertThat(load()).isEmpty();
    }

    @Test
    void firstActivationInsertsTheRunnerAndItsSettings() {
        save(configuration("token-1", "server.invalid", 8080, "grpc.invalid", 9090, "runner:1", "{}"));

        assertThat(jdbc.queryForObject("select count(*) from RUNNER", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select TOKEN from RUNNER", String.class)).isEqualTo("token-1");
        assertThat(jdbc.queryForObject("select STATUS from RUNNER", String.class))
                .as("save is what activation calls, so the row it writes is an active runner")
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select NAME from RUNNER", String.class))
                .as("NAME is NOT NULL and the runner has no name of its own; the server names runners")
                .isEmpty();
    }

    @Test
    void reloadsEveryRuntimeSettingItWasGiven() {
        save(configuration("token-1", "server.invalid", 8080, "grpc.invalid", 9090, "runner:1", "{}"));

        RunnerConfiguration loaded = load().orElseThrow();

        assertThat(loaded.runnerToken()).isEqualTo("token-1");
        assertThat(loaded.restHost()).isEqualTo("server.invalid");
        assertThat(loaded.restPort()).isEqualTo(8080);
        assertThat(loaded.grpcHost()).isEqualTo("grpc.invalid");
        assertThat(loaded.grpcPort()).isEqualTo(9090);
        assertThat(loaded.getPollInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(loaded.isReadyForScheduling())
                .as("a fully configured runner has to come back schedulable, or it restarts idle")
                .isTrue();
    }

    @Test
    void restoresTheExecutionSettingsTheMyBatisPathLostOnRestart() {
        // The regression this migration fixes. save() writes these two to RUNNER_CONFIG_FILE; the
        // MyBatis load path read only RUNNER_CONFIG, so both came back null. RunnerJobService hands
        // the image name straight to `docker create`, so a restarted runner failed every job it took.
        save(configuration("token-1", "server.invalid", 8080, "grpc.invalid", 9090,
                "jgitkins/runner:1.2.3", "{\"plugin\":\"config\"}"));

        assertThat(jdbc.queryForObject("select count(*) from RUNNER_CONFIG_FILE", Integer.class))
                .as("both execution settings are stored in the file-shaped table")
                .isEqualTo(2);

        RunnerConfiguration loaded = load().orElseThrow();

        assertThat(loaded.getRunnerImageName()).isEqualTo("jgitkins/runner:1.2.3");
        assertThat(loaded.getJenkinsPluginConfig()).isEqualTo("{\"plugin\":\"config\"}");
    }

    @Test
    void reactivationUpdatesTheRunnerInPlaceAndKeepsItsCreationTime() {
        save(configuration("token-1", "server.invalid", 8080, "grpc.invalid", 9090, "runner:1", "{}"));
        LocalDateTime created = jdbc.queryForObject("select CREATED_AT from RUNNER", LocalDateTime.class);

        save(configuration("token-2", "other.invalid", 8081, "grpc.invalid", 9090, "runner:2", "{}"));

        assertThat(jdbc.queryForObject("select count(*) from RUNNER", Integer.class))
                .as("TOKEN is UNIQUE: a second row rather than an update would fail on re-activation")
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select TOKEN from RUNNER", String.class)).isEqualTo("token-2");
        assertThat(jdbc.queryForObject("select CREATED_AT from RUNNER", LocalDateTime.class))
                .as("re-linking to the server is not a new runner")
                .isEqualTo(created);

        RunnerConfiguration loaded = load().orElseThrow();
        assertThat(loaded.restHost()).isEqualTo("other.invalid");
        assertThat(loaded.getRunnerImageName()).isEqualTo("runner:2");
    }

    @Test
    void reactivationRewritesSettingRowsRatherThanAccumulatingThem() {
        save(configuration("token-1", "server.invalid", 8080, "grpc.invalid", 9090, "runner:1", "{}"));
        int settingsAfterFirst = jdbc.queryForObject(
                "select count(*) from RUNNER_CONFIG", Integer.class);

        save(configuration("token-1", "server.invalid", 8081, "grpc.invalid", 9090, "runner:1", "{}"));

        assertThat(jdbc.queryForObject("select count(*) from RUNNER_CONFIG", Integer.class))
                .as("(RUNNER_ID, CONFIG_KEY) is UNIQUE, so an insert-only save path would throw here")
                .isEqualTo(settingsAfterFirst);
        assertThat(jdbc.queryForObject(
                "select CONFIG_VALUE from RUNNER_CONFIG where CONFIG_KEY = 'restPort'", String.class))
                .isEqualTo("8081");
    }

    private Optional<RunnerConfiguration> load() {
        return transactions.execute(status -> adapter.loadConfiguration());
    }

    private void save(RunnerConfiguration configuration) {
        transactions.executeWithoutResult(status -> adapter.save(configuration));
    }

    private static RunnerConfiguration configuration(String token, String restHost, int restPort,
            String grpcHost, int grpcPort, String image, String pluginConfig) {
        return RunnerConfiguration.builder()
                .runtimeConfig(RunnerRuntimeConfig.builder()
                        .runnerToken(token)
                        .restHost(restHost)
                        .restPort(restPort)
                        .restBasePath("/api")
                        .grpcHost(grpcHost)
                        .grpcPort(grpcPort)
                        .pollInterval(Duration.ofSeconds(5))
                        .busyWaitInterval(Duration.ofSeconds(1))
                        .build())
                .executionConfig(RunnerExecutionConfig.builder()
                        .runnerImageName(image)
                        .jenkinsPluginConfig(pluginConfig)
                        .build())
                .build();
    }

    private static <T> T repository(EntityManagerFactory emf, Class<T> repositoryInterface) {
        return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
                .getRepository(repositoryInterface);
    }
}
