package io.jgitkins.runner.application.service;

import io.jgitkins.runner.application.dto.RunnerActivateResult;
import io.jgitkins.runner.application.mapper.RunnerActivateResultMapper;
import io.jgitkins.runner.application.port.in.RunnerActivationUseCase;
import io.jgitkins.runner.application.port.out.RunnerActivatePort;
import io.jgitkins.runner.application.port.out.RunnerConfigurationPort;
import io.jgitkins.runner.domain.RunnerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerActivationService implements RunnerActivationUseCase {

    private final RunnerActivatePort activatePort;
    private final RunnerConfigurationPort configurationPort;
    private final RunnerInitService initService;

    @Override
    public RunnerActivateResult activate(String runnerToken, String activateEndpoint) {
        log.info("Activating runner with endpoint {}", activateEndpoint);
        RunnerConfiguration configuration = activatePort.activateRunner(runnerToken, activateEndpoint);
        configurationPort.save(configuration);
        initService.updateCachedConfiguration(configuration);
        return RunnerActivateResultMapper.fromConfiguration(configuration);
    }
}
