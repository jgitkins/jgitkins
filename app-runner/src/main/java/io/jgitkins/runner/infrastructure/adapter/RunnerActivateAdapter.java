package io.jgitkins.runner.infrastructure.adapter;

import io.jgitkins.runner.application.port.out.RunnerActivatePort;
import io.jgitkins.runner.application.mapper.RunnerConfigurationMapper;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.infrastructure.http.ActivationResult;
import io.jgitkins.runner.infrastructure.http.RunnerActivateClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerActivateAdapter implements RunnerActivatePort {

    private final RunnerActivateClient activateClient;
    private final RunnerConfigurationMapper responseMapper;

    @Override
    public RunnerConfiguration activateRunner(String runnerToken, String baseUrl) {
        ActivationResult result = activateClient.activateRunner(runnerToken, baseUrl);
        return responseMapper.toDomain(result, runnerToken, baseUrl);
    }
}
