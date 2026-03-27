package io.jgitkins.runner.presentation.cli;

import io.jgitkins.runner.application.dto.RunnerActivateResult;
import io.jgitkins.runner.application.exception.RunnerRegistrationException;
import io.jgitkins.runner.application.port.in.RunnerActivationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunnerCliApplicationRunner implements ApplicationRunner {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_INVALID_ARGS = 2;
    private static final int EXIT_AUTH_REJECTED = 3;
    private static final int EXIT_CLIENT_ERROR = 4;
    private static final int EXIT_SERVER_ERROR = 5;
    private static final int EXIT_UNEXPECTED = 10;

    private final RunnerActivationUseCase activationUseCase;
    private final ApplicationContext context;

    @Value("${runner.mode:start}")
    private String mode;

    @Value("${runner.activate.token:}")
    private String token;

    @Value("${runner.activate.server:}")
    private String server;

    @Override
    public void run(ApplicationArguments args) {
        if (!"activate".equalsIgnoreCase(mode)) {
            return;
        }

        if (token == null || token.isBlank() || server == null || server.isBlank()) {
            log.error("Missing activation arguments. Expected --token and --server.");
            exit(EXIT_INVALID_ARGS);
            return;
        }

        try {
            RunnerActivateResult result = activationUseCase.activate(token, server);
            log.info("Runner activated. server={}", result.getMasterBaseUrl());
            exit(EXIT_SUCCESS);
        } catch (RunnerRegistrationException ex) {
            log.error("Runner activation failed. status={}, message={}", ex.getStatusCode(), ex.getMessage());
            exit(exitCodeForStatus(ex.getStatusCode()));
        } catch (Exception ex) {
            log.error("Runner activation failed.", ex);
            exit(EXIT_UNEXPECTED);
        }
    }

    private void exit(int code) {
        int exitCode = SpringApplication.exit(context, () -> code);
        System.exit(exitCode);
    }

    private int exitCodeForStatus(int statusCode) {
        if (statusCode == 401 || statusCode == 403 || statusCode == 409) {
            return EXIT_AUTH_REJECTED;
        }
        if (statusCode >= 400 && statusCode < 500) {
            return EXIT_CLIENT_ERROR;
        }
        if (statusCode >= 500) {
            return EXIT_SERVER_ERROR;
        }
        return EXIT_UNEXPECTED;
    }
}
