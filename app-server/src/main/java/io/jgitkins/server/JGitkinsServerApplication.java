package io.jgitkins.server;

import io.jgitkins.core.persistence.DataSourceConfig;
import io.jgitkins.core.persistence.MybatisConfig;
import io.jgitkins.server.common.infrastructure.config.JpaPersistenceConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({DataSourceConfig.class, MybatisConfig.class, JpaPersistenceConfiguration.class})
@MapperScan(basePackages = {
    "io.jgitkins.server.collaboration.adapter.out.persistence.translator",
    "io.jgitkins.server.repository.adapter.out.persistence.translator",
    "io.jgitkins.server.execution.adapter.out.persistence.translator",
    "io.jgitkins.server.change.review.adapter.out.persistence.translator",
    "io.jgitkins.server.identity.access.adapter.out.persistence.translator"
})
public class JGitkinsServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(JGitkinsServerApplication.class, args);
	}
}
