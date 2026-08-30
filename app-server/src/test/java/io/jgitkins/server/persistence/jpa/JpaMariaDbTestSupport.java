package io.jgitkins.server.persistence.jpa;

import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Owns the MariaDB every JPA test in this module runs against, and starts it.
 *
 * <p>Extracted in task 2.72 from two byte-identical per-context copies. Tasks 2.73 through 2.77 each
 * migrate another context and would each have copied it again; the point of one home is that the two
 * non-obvious rules below get stated once and cannot drift between contexts.
 *
 * <p><strong>Rule one: build repositories from {@link SharedEntityManagerCreator}</strong>, never
 * from {@code emf.createEntityManager()}. A plain EntityManager is not transaction-aware and is
 * shared across threads, so a repository built on one never joins the surrounding transaction and a
 * pessimistic lock is never actually taken. The symptom is not an error at the wiring; it is a hang
 * followed by a failure on an unrelated assertion, which is why this is worth writing down.
 *
 * <p><strong>Rule two: the schema belongs to {@code app-server/data/ddl.sql}</strong>, so
 * {@code hibernate.hbm2ddl.auto} is pinned to {@code none}. A test that lets Hibernate create its own
 * tables passes against a schema that does not exist in any environment, which makes it worse than no
 * test — it reports the mapping as verified.
 *
 * <p>Each caller passes its own {@code packagesToScan} and a distinct persistence unit name. The unit
 * names must differ: two factory beans sharing one name collide inside the same JVM once the whole
 * suite runs together, and the failure surfaces only when both test classes are present.
 *
 * <h2>Task 2.103: the database is started here, not assumed</h2>
 *
 * <p>Until 2.103 the URL pointed at {@code 127.0.0.1:53306} and sixteen classes opened each test with a
 * JUnit assumption on {@code mariaDbReachable}. On a machine without the compose override — CI, every
 * time, since no verify workflow declared a {@code services:} block — those twenty-two tests skipped
 * and Gradle still printed BUILD SUCCESSFUL. Green with no evidence behind it is worse than red.
 *
 * <p><strong>There is deliberately no fallback.</strong> If Docker is unavailable the container fails
 * to start and every test that touches this class fails. That is the point of the change: a skip
 * that nobody reads is exactly what was removed, so re-adding one under a different name would undo
 * it.
 *
 * <p>The container is a {@code static} singleton started once per JVM rather than a JUnit
 * {@code @Container} field, because JUnit manages that per test class and there are sixteen of them —
 * sixteen MariaDB starts instead of one. Ryuk reaps it when the JVM exits, so there is no shutdown
 * hook here to get wrong.
 *
 * <p>The schema is mounted into {@code /docker-entrypoint-initdb.d/} rather than passed to
 * {@code withInitScript}. {@code withInitScript} parses through Spring's {@code ScriptUtils}, and
 * {@code ddl.sql} is {@code mariadb-dump} output: it opens with {@code /*M!999999 ...} and carries
 * fifty-eight conditional-execution comments that the dump's own reader understands and a generic
 * SQL splitter does not. Mounting hands the file to the MariaDB entrypoint — the same tool family
 * that wrote it.
 *
 * <p>The image is pinned to {@code mariadb:11.4}: the tag the local compose override runs, not the
 * {@code 10.11} that produced the dump. CI exists to catch what a developer will hit, and every
 * developer is on 11.4 today. {@code latest} is not used — the day the engine changes, CI goes red
 * for a reason that appears in no commit.
 */
public final class JpaMariaDbTestSupport {

    /** Set by {@code app-server/build.gradle} on the test task. See {@link #startContainer()}. */
    private static final String DDL_PROPERTY = "jgitkins.test.ddl";

    /**
     * {@code MariaDbEvidenceConnectivityTest} queries {@code information_schema} for this schema by
     * name, and {@code ddl.sql} itself issues {@code USE `JGITKINS`}. Both would have to change
     * together.
     */
    private static final String DATABASE = "JGITKINS";

    private static final MariaDBContainer<?> CONTAINER = startContainer();

    public static final String URL = CONTAINER.getJdbcUrl();
    public static final String USER = CONTAINER.getUsername();
    public static final String PASSWORD = CONTAINER.getPassword();

    private JpaMariaDbTestSupport() {
    }

    private static MariaDBContainer<?> startContainer() {
        String configured = System.getProperty(DDL_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    DDL_PROPERTY + " is not set. app-server/build.gradle sets it on the test task; a "
                            + "runner that bypasses Gradle would otherwise start a container on an "
                            + "empty schema, and the mapping tests would report success against no "
                            + "tables at all.");
        }
        Path ddl = Path.of(configured);
        if (!Files.isRegularFile(ddl)) {
            throw new IllegalStateException(
                    DDL_PROPERTY + " points at " + ddl.toAbsolutePath() + ", which is not a file. The "
                            + "container would start with an empty schema rather than fail.");
        }
        MariaDBContainer<?> container = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
                .withDatabaseName(DATABASE)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(ddl), "/docker-entrypoint-initdb.d/ddl.sql");
        container.start();
        return container;
    }

    public static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USER, PASSWORD);
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        return dataSource;
    }

    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DriverManagerDataSource dataSource, String persistenceUnitName, String... packagesToScan) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan(packagesToScan);
        factoryBean.setPersistenceUnitName(persistenceUnitName);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    public static <T> T repository(EntityManagerFactory emf, Class<T> repositoryInterface) {
        return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
                .getRepository(repositoryInterface);
    }
}
