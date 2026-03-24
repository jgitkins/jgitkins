package io.jgitkins.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("io.jgitkins.server")
public class JGitkinsServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(JGitkinsServerApplication.class, args);
	}
}
