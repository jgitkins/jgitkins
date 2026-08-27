package io.jgitkins.server.persistence.jpa;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Shared wiring for every JPA test that runs against the local MariaDB.
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
 */
public final class JpaMariaDbTestSupport {

    public static final String URL = "jdbc:mariadb://127.0.0.1:53306/JGITKINS";
    public static final String USER = "root";
    public static final String PASSWORD = "root1234";

    private JpaMariaDbTestSupport() {
    }

    public static boolean mariaDbReachable() {
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
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
