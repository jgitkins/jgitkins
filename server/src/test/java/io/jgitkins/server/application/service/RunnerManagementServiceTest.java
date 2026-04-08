package io.jgitkins.server.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.RunnerRuntimeConfig;
import io.jgitkins.server.application.dto.command.RunnerRegisterCommand;
import io.jgitkins.server.application.dto.result.RunnerActivateResult;
import io.jgitkins.server.application.dto.result.RunnerRegistrationResult;
import io.jgitkins.server.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.application.exception.RunnerNotFoundException;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.domain.error.DomainProblemSpec;
import io.jgitkins.server.infrastructure.exception.RunnerActivationFailedException;
import io.jgitkins.server.infrastructure.exception.RunnerDeletionFailedException;
import io.jgitkins.server.domain.aggregate.Runner;
import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import io.jgitkins.server.domain.model.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunnerManagementServiceTest {

    @Mock
    private RunnerPersistencePort runnerPort;

    @Mock
    private RunnerApplicationMapper runnerApplicationMapper;

    @Mock
    private RunnerRuntimeConfigProvider runtimeConfigProvider;

    @InjectMocks
    private RunnerManagementService service;

    @Test
    void register_returnsMappedResult() {
        RunnerRegisterCommand command = new RunnerRegisterCommand("runner", RunnerScopeType.GLOBAL, null);

        Runner saved = Runner.restore(10L, "RNR-123", "runner", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        RunnerRegistrationResult mapped = new RunnerRegistrationResult(10L, "RNR-123", "OFFLINE", LocalDateTime.now());

        when(runnerPort.save(any(Runner.class))).thenReturn(saved);
        when(runnerApplicationMapper.toRegistrationResult(saved)).thenReturn(mapped);

        RunnerRegistrationResult result = service.register(command);

        assertThat(result.runnerId()).isEqualTo(10L);
        verify(runnerPort).save(any(Runner.class));
    }

    @Test
    void deleteRunner_throwsNotFound_whenMissing() {
        when(runnerPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRunner(99L))
                .isInstanceOf(RunnerNotFoundException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(ApplicationErrorCode.NOT_FOUND);
    }

    @Test
    void deleteRunner_mapsRuntimeFailureToInfrastructureError() {
        Runner runner = Runner.restore(99L, "RNR-TOKEN", "runner", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        when(runnerPort.findById(99L)).thenReturn(Optional.of(runner));
        doThrow(new RunnerDeletionFailedException("Runner deletion failed", new RuntimeException("delete failed")))
                .when(runnerPort).deleteById(99L);

        assertThatThrownBy(() -> service.deleteRunner(99L))
                .isInstanceOf(RunnerDeletionFailedException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED);
    }

    @Test
    void activate_returnsRuntimeAndExecutionConfig() {
        Runner offline = Runner.restore(1L, "RNR-TOKEN", "runner", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        Runner persisted = offline.activate("RNR-TOKEN", "127.0.0.1");

        when(runnerPort.findByToken("RNR-TOKEN")).thenReturn(Optional.of(offline));
        when(runnerPort.save(any(Runner.class))).thenReturn(persisted);
        when(runtimeConfigProvider.createConfig()).thenReturn(
                RunnerRuntimeConfig.builder()
                        .serviceHost("localhost")
                        .restScheme("http")
                        .restPort(8080)
                        .restBasePath("/api")
                        .grpcPort(6565)
                        .pollIntervalMs(5000L)
                        .busyWaitIntervalMs(1000L)
                        .build()
        );

        RunnerActivateResult result = service.activate("RNR-TOKEN", "127.0.0.1");

        assertThat(result.runtimeConfig().getServiceHost()).isEqualTo("localhost");
        assertThat(result.executionConfig().getRunnerImageName()).isEqualTo("jenkins/jenkinsfile-runner");
        verify(runnerPort).save(any(Runner.class));
    }

    @Test
    void activate_throwsConflict_whenAlreadyActive() {
        Runner online = Runner.restore(1L, "RNR-TOKEN", "runner", RunnerStatus.ONLINE,
                RunnerScopeType.GLOBAL, null, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now());

        when(runnerPort.findByToken("RNR-TOKEN")).thenReturn(Optional.of(online));

        assertThatThrownBy(() -> service.activate("RNR-TOKEN", "127.0.0.1"))
                .isInstanceOf(JgitkinsException.class)
                .extracting(ex -> ((JgitkinsException) ex).getProblemCode())
                .isEqualTo(DomainProblemSpec.RUNNER_ALREADY_ACTIVE.getCode());
    }

    @Test
    void activate_mapsPersistenceFailureToInfrastructureError() {
        Runner offline = Runner.restore(1L, "RNR-TOKEN", "runner", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());

        when(runnerPort.findByToken("RNR-TOKEN")).thenReturn(Optional.of(offline));
        when(runnerPort.save(any(Runner.class)))
                .thenThrow(new RunnerActivationFailedException("Runner activation persistence failed", new RuntimeException("save failed")));

        assertThatThrownBy(() -> service.activate("RNR-TOKEN", "127.0.0.1"))
                .isInstanceOf(RunnerActivationFailedException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED);
    }
}
