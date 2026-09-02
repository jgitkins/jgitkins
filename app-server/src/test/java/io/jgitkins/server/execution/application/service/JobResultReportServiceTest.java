package io.jgitkins.server.execution.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.application.contract.command.JobResultReportCommand;
import io.jgitkins.server.execution.application.internal.JobResultStatus;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobResultReportServiceTest {
    @Mock JobRepository jobs;
    @Mock RunnerRepository runners;
    @Test void report_rejectsUnknownRunner() {
        when(runners.findByToken("missing")).thenReturn(Optional.empty());
        var service = new JobResultReportService(jobs, runners);
        assertThatThrownBy(() -> service.reportJobResult(new JobResultReportCommand("missing", 1L, JobResultStatus.SUCCESS)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void report_rejectsUnknownJob() {
        Runner runner = Runner.create("worker", io.jgitkins.server.execution.domain.vo.RunnerScopeType.GLOBAL, null).withId(7L);
        when(runners.findByToken("token")).thenReturn(Optional.of(runner));
        when(jobs.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new JobResultReportService(jobs, runners).reportJobResult(new JobResultReportCommand("token", 1L, JobResultStatus.SUCCESS)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
