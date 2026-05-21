package io.jgitkins.server.execution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.execution.application.exception.RunnerNotFoundException;
import io.jgitkins.server.execution.application.contract.result.RunnerDetailResult;
import io.jgitkins.server.execution.application.mapper.RunnerApplicationMapper;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunnerReadServiceTest {

    @Mock
    private RunnerApplicationMapper runnerApplicationMapper;

    @Mock
    private RunnerRepository runnerPort;

    @InjectMocks
    private RunnerReadService service;

    @Test
    void getRunner_returnsMappedResult() {
        Runner runner = Runner.restore(1L, "RNR-TOKEN", "runner", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        RunnerDetailResult mapped = new RunnerDetailResult(1L, null, null, "OFFLINE", null, null);

        when(runnerPort.findById(1L)).thenReturn(Optional.of(runner));
        when(runnerApplicationMapper.toActivationResult(runner)).thenReturn(mapped);

        RunnerDetailResult result = service.getRunner(1L);

        assertThat(result.runnerId()).isEqualTo(1L);
        verify(runnerPort).findById(1L);
        verify(runnerApplicationMapper).toActivationResult(runner);
    }

    @Test
    void getRunner_throwsNotFound_whenMissing() {
        when(runnerPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRunner(99L))
                .isInstanceOf(RunnerNotFoundException.class)
                .extracting(ex -> ((JgitkinsException) ex).getErrorCode())
                .isEqualTo(ApplicationErrorCode.NOT_FOUND);
    }

    @Test
    void getRunners_returnsMappedList() {
        Runner a = Runner.restore(1L, "T1", "a", RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        Runner b = Runner.restore(2L, "T2", "b", RunnerStatus.ONLINE,
                RunnerScopeType.GLOBAL, null, "10.0.0.1", LocalDateTime.now(), LocalDateTime.now());

        RunnerDetailResult ma = new RunnerDetailResult(1L, null, null, null, null, null);
        RunnerDetailResult mb = new RunnerDetailResult(2L, null, null, null, null, null);

        when(runnerPort.findAll()).thenReturn(List.of(a, b));
        when(runnerApplicationMapper.toActivationResult(a)).thenReturn(ma);
        when(runnerApplicationMapper.toActivationResult(b)).thenReturn(mb);

        List<RunnerDetailResult> results = service.getRunners();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).runnerId()).isEqualTo(1L);
        assertThat(results.get(1).runnerId()).isEqualTo(2L);
    }
}
