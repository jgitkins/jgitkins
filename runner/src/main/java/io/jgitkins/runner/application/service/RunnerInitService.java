package io.jgitkins.runner.application.service;

import io.jgitkins.runner.application.port.out.RunnerConfigurationPort;
import io.jgitkins.runner.domain.RunnerConfiguration;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerInitService {

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(5);

    private final RunnerConfigurationPort configurationPort;
    private final AtomicReference<RunnerConfiguration> currentConfiguration = new AtomicReference<>();

    @PostConstruct
    private void initialize() {
        Optional<RunnerConfiguration> loaded = configurationPort.loadConfiguration();
        loaded.ifPresent(configuration -> {
            log.info("configuration: [{}]", configuration);
            currentConfiguration.set(configuration);
            logConfigurationStatus(configuration);
        });
        if (loaded.isEmpty()) {
            log.info("Runner activation pending. Scheduler will remain idle until activation completes.");
        }
    }

    void updateCachedConfiguration(RunnerConfiguration configuration) {
        currentConfiguration.set(configuration);
        logConfigurationStatus(configuration);
    }

    RunnerConfiguration getCurrentConfiguration() {
        return currentConfiguration.get();
    }

    boolean isReadyForScheduling() {
        RunnerConfiguration configuration = currentConfiguration.get();
        return configuration != null && configuration.isReadyForScheduling();
    }

    public long pollIntervalMillis() {
        RunnerConfiguration configuration = currentConfiguration.get();

        Duration duration = configuration != null && configuration.getPollInterval() != null
            ? configuration.getPollInterval()
            : DEFAULT_POLL_INTERVAL;
        return duration.toMillis();
    }

    private void logConfigurationStatus(RunnerConfiguration configuration) {
        if (configuration == null) {
            log.warn("Runner configuration is not available.");
        } else if (!configuration.isLinked()) {
            log.warn("서버 연동이 필요합니다. Runner 토큰을 등록하고 서버 연동 API를 호출하세요.");
        } else if (!configuration.isReadyForScheduling()) {
            log.warn("Runner 설정이 아직 완전하지 않아 스케줄링이 비활성화되어 있습니다.");
        } else {
            log.info("Runner 설정 로딩 완료. 서버: [{}]", configuration.getMasterBaseUrl());
        }
    }
}
