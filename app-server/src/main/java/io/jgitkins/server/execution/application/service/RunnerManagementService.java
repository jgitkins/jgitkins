package io.jgitkins.server.execution.application.service;

import io.jgitkins.server.execution.application.contract.result.RunnerExecutionConfig;
import io.jgitkins.server.execution.application.contract.command.RunnerRegisterCommand;
import io.jgitkins.server.execution.application.contract.result.RunnerActivateResult;
import io.jgitkins.server.execution.application.contract.result.RunnerRegistrationResult;
import io.jgitkins.server.execution.application.exception.RunnerNotFoundException;
import io.jgitkins.server.execution.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.execution.application.port.in.RunnerActivateUseCase;
import io.jgitkins.server.execution.application.port.in.RunnerDeleteUseCase;
import io.jgitkins.server.execution.application.port.in.RunnerRegisterUseCase;
import io.jgitkins.server.execution.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunnerManagementService implements RunnerRegisterUseCase, RunnerDeleteUseCase, RunnerActivateUseCase {

    private final RunnerRepository runnerRepository;
    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRuntimeConfigProvider runtimeConfigProvider;

    @Override
    @Transactional
    public RunnerRegistrationResult register(RunnerRegisterCommand command) {
        Runner runner = Runner.create(command.description(),
                command.scopeType(),
                command.targetId());
        Runner savedRunner = runnerRepository.save(runner);
        log.info("Runner registered. runnerId={}", savedRunner.getId());
        return runnerApplicationMapper.toRegistrationResult(savedRunner);
    }

    @Override
    @Transactional
    public void deleteRunner(Long runnerId) {
        runnerRepository.findById(runnerId)
                .orElseThrow(() -> new RunnerNotFoundException(runnerId));
        runnerRepository.deleteById(runnerId);
    }

    @Override
    @Transactional
    public RunnerActivateResult activate(String token, String remoteIp) {
        Runner runner = runnerRepository.findByToken(token)
                .orElseThrow(() -> new RunnerNotFoundException("Runner not found"));

        // DomainException(RunnerAlreadyActiveException, RunnerTokenMismatchException,
        // RunnerTokenMissingException)은
        // 재포장 없이 그대로 전파 → GlobalExceptionHandler.handleDomainException 처리
        Runner activatedInfo = runner.activate(token, remoteIp);

        Runner persisted = runnerRepository.save(activatedInfo);
        log.info("Runner activated. runnerId={}", persisted.getId());
        return new RunnerActivateResult(
                runtimeConfigProvider.createConfig(),
                RunnerExecutionConfig.defaultConfig()
        );
    }
}
