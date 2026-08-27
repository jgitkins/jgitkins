package io.jgitkins.server;

import io.jgitkins.core.persistence.DataSourceConfig;
import io.jgitkins.core.persistence.MybatisConfig;
import io.jgitkins.server.common.infrastructure.config.JpaPersistenceConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan("io.jgitkins.server")
@Import({DataSourceConfig.class, MybatisConfig.class, JpaPersistenceConfiguration.class})
@MapperScan(basePackages = {
    "io.jgitkins.server.collaboration.adapter.out.persistence.mapper",
    "io.jgitkins.server.repository.adapter.out.persistence.mapper",
    "io.jgitkins.server.execution.adapter.out.persistence.mapper",
    "io.jgitkins.server.change.review.adapter.out.persistence.mapper",
    "io.jgitkins.server.identity.access.adapter.out.persistence.mapper"
})
public class JGitkinsServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(JGitkinsServerApplication.class, args);
	}
}
