package io.jgitkins.server;

import io.jgitkins.core.persistence.DataSourceConfig;
import io.jgitkins.server.common.infrastructure.config.JpaPersistenceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({DataSourceConfig.class, JpaPersistenceConfiguration.class})
public class JGitkinsServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(JGitkinsServerApplication.class, args);
	}
}
