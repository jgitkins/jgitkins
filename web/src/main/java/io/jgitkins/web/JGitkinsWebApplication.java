package io.jgitkins.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JGitkinsWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(JGitkinsWebApplication.class, args);
	}
}
