package io.jgitkins.server;

import io.jgitkins.core.persistence.DataSourceConfig;
import io.jgitkins.core.persistence.MybatisConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * There is deliberately no explicit {@code @ComponentScan} here.
 *
 * <p>This class lives in {@code io.jgitkins.server}, which is exactly the package
 * {@code @SpringBootApplication} already scans. Restating it as
 * {@code @ComponentScan("io.jgitkins.server")} covered the identical packages while replacing the
 * filters {@code @SpringBootApplication} contributes, notably {@code TypeExcludeFilter}. That
 * filter is what makes {@code @WebMvcTest} a slice: without it every such test loaded the whole
 * application, and any {@code @Configuration} in test sources under this package was registered
 * into the production context.
 */
@SpringBootApplication
@Import({DataSourceConfig.class, MybatisConfig.class})
@MapperScan(basePackages = {
    "io.jgitkins.server.collaboration.infrastructure.persistence.mapper",
    "io.jgitkins.server.repository.infrastructure.persistence.mapper",
    "io.jgitkins.server.execution.infrastructure.persistence.mapper",
    "io.jgitkins.server.change.review.infrastructure.persistence.mapper",
    "io.jgitkins.server.identity.access.infrastructure.persistence.mapper"
})
public class JGitkinsServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(JGitkinsServerApplication.class, args);
	}
}
