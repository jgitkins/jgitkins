
package io.jgitkins.core.persistence;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * Shared MyBatis wiring: the {@link SqlSessionFactory} and the mapper XML locations.
 *
 * <p>Deliberately carries no {@code @MapperScan}. Mapper interface packages belong to the module
 * that owns them, and app-server already declares its own scan on {@code JGitkinsServerApplication}.
 * Listing the same five packages here registered a second {@code MapperScannerConfigurer} over the
 * identical packages. That is harmless while every mapper is only consumed by component-scanned
 * classes, and becomes {@code ConflictingBeanDefinitionException} as soon as one of those mappers is
 * injected into an explicitly declared bean instead, which is what the persistence selector work
 * does to one capability slice after another.
 *
 * <p>It also meant a core module hard-coded {@code io.jgitkins.server.*} package names as
 * annotation strings, which the module-boundary architecture test cannot detect because they are
 * not imports.
 */
@Configuration
public class MybatisConfig {

    /**
     * getSqlSessionFactory
     */
    @Bean
    public SqlSessionFactory getSqlSessionFactory(DataSource dataSource) throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.addMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setConfiguration(configuration);
        return sessionFactoryBean.getObject();
    }
}
