package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.*;
import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerActivateResult;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.application.port.in.RunnerActivateUseCase;
import io.jgitkins.server.application.port.in.RunnerDeleteUseCase;
import io.jgitkins.server.application.port.in.RunnerRegisterUseCase;
import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.domain.aggregate.Runner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunnerManagementService implements RunnerRegisterUseCase, RunnerDeleteUseCase, RunnerActivateUseCase {

    private final RunnerPersistencePort runnerPort;
    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRuntimeConfigProvider runtimeConfigProvider;

    @Override
    @Transactional
    public RunnerRegistrationResult register(RunnerRegisterCommand command) {
        Runner runner = Runner.create(command.getDescription(),
                command.getScopeType(),
                command.getTargetId());
        Runner savedRunner = runnerPort.save(runner);
        log.info("Runner registered. runnerId={}", savedRunner.getId());
        return runnerApplicationMapper.toRegistrationResult(savedRunner);
    }

    @Override
    @Transactional
    public void deleteRunner(Long runnerId) {
        runnerPort.findById(runnerId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.RUNNER_NOT_FOUND));

        try {
            runnerPort.deleteById(runnerId);
        } catch (RuntimeException ex) {
            log.error("Runner deletion failed. runnerId={}", runnerId, ex);
            // Infrastructure 어댑터에서 InfrastructureException으로 감싸지 않은 경우
            // ApplicationException으로 처리
            // TODO: 어댑터에서 Infrastructure 예외던질것
            throw new ApplicationException(ApplicationErrorCode.RUNNER_DELETE_FAILED, "Runner deletion failed", ex);
        }
    }

    @Override
    @Transactional
    public RunnerActivateResult activate(String token, String remoteIp) {
        Runner runner = runnerPort.findByToken(token)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.RUNNER_NOT_FOUND));

        // DomainException(RunnerAlreadyActiveException, RunnerTokenMismatchException,
        // RunnerTokenMissingException)은
        // 재포장 없이 그대로 전파 → GlobalExceptionHandler.handleDomainException 처리
        Runner activatedInfo = runner.activate(token, remoteIp);

        try {
            Runner persisted = runnerPort.save(activatedInfo);
            log.info("Runner activated. runnerId={}", persisted.getId());
            return RunnerActivateResult.builder()
                    .executionConfig(RunnerExecutionConfig.defaultConfig())
                    .runtimeConfig(runtimeConfigProvider.createConfig())
                    .build();
        } catch (RuntimeException ex) {
            log.error("Runner activation failed. runnerId={}", runner.getId(), ex);
            // Infrastructure 어댑터에서 InfrastructureException으로 감싸지 않은 경우
            // ApplicationException으로 처리
            // TODO: 어댑터에서 Infrastructure 예외던질것
            throw new ApplicationException(ApplicationErrorCode.RUNNER_ACTIVATION_FAILED,
                    "Runner activation persistence failed", ex);
        }
    }
}
