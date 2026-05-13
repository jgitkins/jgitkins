package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.result.RunnerDetailResult;
import io.jgitkins.server.application.exception.RunnerNotFoundException;
import io.jgitkins.server.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.execution.application.port.in.RunnerLoadUseCase;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunnerReadService implements RunnerLoadUseCase {

    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRepository runnerRepository;

    @Override
    @Transactional(readOnly = true)
    public RunnerDetailResult getRunner(Long runnerId) {
        Runner runner = runnerRepository.findById(runnerId)
                .orElseThrow(() -> new RunnerNotFoundException(runnerId));
        return runnerApplicationMapper.toActivationResult(runner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunnerDetailResult> getRunners() {
        return runnerRepository.findAll().stream()
                .map(runnerApplicationMapper::toActivationResult)
                .toList();
    }
}
