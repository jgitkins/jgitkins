package io.jgitkins.server.common.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The application's JPA foundation, and now its only persistence foundation.
 *
 * <p>Everything here is declared rather than auto-configured. That began as consistency with how the
 * application already treated persistence -- {@code DataSourceConfig} builds the {@code DataSource}
 * by hand, and {@code MybatisConfig} built the {@code SqlSessionFactory} the same way -- and the
 * reason that outlives MyBatis is the one that mattered: Boot's Hibernate auto-configuration did not
 * produce an {@code entityManagerFactory} in this context, and chasing that condition is worse than
 * owning the three beans outright.
 *
 * <p>Entity and repository discovery is scoped to {@code io.jgitkins.server}, app-server's own root.
 * Entities live under each context's {@code adapter/out/persistence/jpa} package, which keeps
 * {@code jakarta.persistence} out of the domain and application roots that Task 2.66 guards.
 *
 * <p>Schema management stays off. {@code ddl-auto} is {@code none} in {@code application.yml}: the
 * migration swapped the persistence provider and never the schema, and the tables are owned by
 * {@code app-server/data/ddl.sql}. Letting Hibernate validate or update them would turn a provider
 * swap into a schema change, and the rows in those tables were written by a provider that is no
 * longer here to be asked what it meant.
 *
 * <p>{@link JpaTransactionManager} is the single transaction manager for the application. It was
 * already the single one before MyBatis was removed, managing the same {@code DataSource} connection
 * both providers used, which is what let a {@code @Transactional} boundary behave identically on
 * either side of a slice's cutover. Boot's {@code DataSourceTransactionManager} auto-configuration is
 * {@code @ConditionalOnMissingBean}, so declaring this one makes it back off rather than compete.
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.jgitkins.server")
@EntityScan(basePackages = "io.jgitkins.server")
@EnableTransactionManagement
public class JpaPersistenceConfiguration {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("io.jgitkins.server");
        factoryBean.setPersistenceUnitName("jgitkins");
        // No dialect is set on purpose: Hibernate resolves it from the live connection, so the same
        // configuration serves MariaDB in production and H2 in the profiles that still use it.
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
