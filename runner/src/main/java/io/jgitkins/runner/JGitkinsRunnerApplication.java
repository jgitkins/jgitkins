package io.jgitkins.runner;

import io.jgitkins.runner.presentation.cli.RunnerCliArguments;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
//@EnableConfigurationProperties(RunnerProperties.class)
public class JGitkinsRunnerApplication {
    public static void main(String[] args) {

        RunnerCliArguments cliArguments;

        try {
            cliArguments = RunnerCliArguments.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(RunnerCliArguments.usage());
            System.exit(2);
            return;
        }

        SpringApplicationBuilder builder = new SpringApplicationBuilder(JGitkinsRunnerApplication.class)
            .properties(cliArguments.toSpringProperties());

        if (cliArguments.isActivate()) {
            builder.web(WebApplicationType.NONE);
        }

        builder.run(args);
    }
}
