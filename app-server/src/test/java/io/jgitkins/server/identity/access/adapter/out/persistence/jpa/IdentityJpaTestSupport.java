package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

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
 * Shared wiring for the identity JPA tests that run against the local MariaDB.
 *
 * <p>Repositories are built from {@link SharedEntityManagerCreator}, not
 * {@code emf.createEntityManager()}. A plain EntityManager is not transaction-aware and is shared
 * across threads, so a repository built on one never joins the surrounding transaction and a
 * pessimistic lock is never taken - it fails as a timeout on an unrelated assertion.
 */
final class IdentityJpaTestSupport {

    static final String URL = "jdbc:mariadb://127.0.0.1:53306/JGITKINS";
    static final String USER = "root";
    static final String PASSWORD = "root1234";

    private IdentityJpaTestSupport() {
    }

    static boolean mariaDbReachable() {
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USER, PASSWORD);
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        return dataSource;
    }

    static LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DriverManagerDataSource dataSource, String persistenceUnitName) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("io.jgitkins.server.identity.access.adapter.out.persistence.jpa");
        factoryBean.setPersistenceUnitName(persistenceUnitName);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // The schema is owned by app-server/data/ddl.sql.
        factoryBean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    static <T> T repository(EntityManagerFactory emf, Class<T> repositoryInterface) {
        return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
                .getRepository(repositoryInterface);
    }
}
