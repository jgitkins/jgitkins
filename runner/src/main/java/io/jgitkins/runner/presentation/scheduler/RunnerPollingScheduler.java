package io.jgitkins.runner.presentation.scheduler;

import io.jgitkins.runner.application.port.in.RunnerJobUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunnerPollingScheduler {

    private final RunnerJobUseCase runnerJobUseCase;

    @Scheduled(fixedDelayString = "#{@runnerInitService.pollIntervalMillis()}")
    public void pollServer() {
        runnerJobUseCase.execute();
    }
}
