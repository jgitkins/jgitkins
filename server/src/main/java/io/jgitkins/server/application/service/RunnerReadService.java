package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.application.port.in.RunnerLoadUseCase;
import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.domain.aggregate.Runner;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunnerReadService implements RunnerLoadUseCase {

    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerPersistencePort runnerPort;

    @Override
    @Transactional(readOnly = true)
    public RunnerDetailResult getRunner(Long runnerId) {
        Runner runner = runnerPort.findById(runnerId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.RUNNER_NOT_FOUND));
        return runnerApplicationMapper.toActivationResult(runner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunnerDetailResult> getRunners() {
        return runnerPort.findAll().stream()
                .map(runnerApplicationMapper::toActivationResult)
                .toList();
    }
}
