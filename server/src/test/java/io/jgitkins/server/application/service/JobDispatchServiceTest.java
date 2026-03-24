package io.jgitkins.server.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.DispatchableJob;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import io.jgitkins.server.application.dto.command.DispatchJobCommand;
import io.jgitkins.server.application.dto.result.JobDispatchResult;
import io.jgitkins.server.application.port.out.JobPersistencePort;
import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.aggregate.Runner;
import io.jgitkins.server.domain.model.JobHistory;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.domain.model.vo.JobHistoryId;
import io.jgitkins.server.domain.model.vo.JobId;
import io.jgitkins.server.domain.model.vo.JobStatus;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import io.jgitkins.server.domain.model.vo.RunnerStatus;
import io.jgitkins.server.domain.model.vo.SequenceNumber;
import io.jgitkins.server.domain.model.vo.SystemUser;
import io.jgitkins.server.domain.model.vo.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobDispatchServiceTest {

    @Mock
    private JobPersistencePort jobPort;

    @Mock
    private RunnerPersistencePort runnerPort;

    @Mock
    private CloneUrlBuilder cloneUrlBuilder;

    @InjectMocks
    private JobDispatchService service;

    @Test
    void dispatch_returnsEmpty_whenRunnerTokenMissing() {
        DispatchJobCommand command = new DispatchJobCommand(" ");

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verifyNoInteractions(runnerPort, jobPort, cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsEmpty_whenRunnerNotFound() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        when(runnerPort.findByToken("token")).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(runnerPort).findByToken("token");
        verifyNoInteractions(jobPort, cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsEmpty_whenNoDispatchableJobForRunner() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        Runner runner = runner(7L);

        when(runnerPort.findByToken("token")).thenReturn(Optional.of(runner));
        when(jobPort.findNextDispatchableJob(any(RunnerDispatchContext.class))).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(runnerPort).findByToken("token");
        verify(jobPort).findNextDispatchableJob(any(RunnerDispatchContext.class));
        verifyNoInteractions(cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsResult_whenDispatchSucceeds() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        Runner runner = runner(7L);
        DispatchableJob dispatchableJob = dispatchableJob(101L, 55L, "org/repo.git");

        when(runnerPort.findByToken("token")).thenReturn(Optional.of(runner));
        when(jobPort.findNextDispatchableJob(any(RunnerDispatchContext.class))).thenReturn(Optional.of(dispatchableJob));
        when(jobPort.saveHistory(any(Job.class), any(JobHistory.class))).thenReturn(Optional.of(999L));
        when(cloneUrlBuilder.build("org/repo.git")).thenReturn("https://git.example/org/repo.git");

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isPresent();
        assertThat(result.get().jobId()).isEqualTo(101L);
        assertThat(result.get().jobHistoryId()).isEqualTo(999L);
        assertThat(result.get().runnerId()).isEqualTo(7L);
        assertThat(result.get().repositoryId()).isEqualTo(55L);
        assertThat(result.get().organizeId()).isEqualTo(12L);
        assertThat(result.get().commitHash()).isEqualTo("abc123def456");
        assertThat(result.get().branchName()).isEqualTo("main");
        assertThat(result.get().triggeredBy()).isEqualTo(3L);
        assertThat(result.get().cloneUrl()).isEqualTo("https://git.example/org/repo.git");

        verify(jobPort).saveHistory(any(Job.class), any(JobHistory.class));
        verify(cloneUrlBuilder).build("org/repo.git");
    }

    @Test
    void dispatch_returnsEmpty_whenAnotherDispatcherAlreadySavedHistory() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        Runner runner = runner(7L);
        DispatchableJob dispatchableJob = dispatchableJob(101L, 55L, "org/repo.git");

        when(runnerPort.findByToken("token")).thenReturn(Optional.of(runner));
        when(jobPort.findNextDispatchableJob(any(RunnerDispatchContext.class))).thenReturn(Optional.of(dispatchableJob));
        when(jobPort.saveHistory(any(Job.class), any(JobHistory.class))).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(jobPort).saveHistory(any(Job.class), any(JobHistory.class));
        verifyNoInteractions(cloneUrlBuilder);
    }

    private Runner runner(Long runnerId) {
        return Runner.restore(
                runnerId,
                "token",
                "runner",
                RunnerStatus.OFFLINE,
                RunnerScopeType.GLOBAL,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private DispatchableJob dispatchableJob(Long jobId, Long repositoryId, String clonePath) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        Job job = Job.reconstruct(
                JobId.of(String.valueOf(jobId)),
                RepositoryId.of(repositoryId),
                CommitHash.of("abc123def456"),
                BranchName.of("main"),
                UserId.of(3L),
                createdAt,
                List.of(JobHistory.reconstruct(
                        JobHistoryId.generate(),
                        JobId.of(String.valueOf(jobId)),
                        SequenceNumber.first(),
                        null,
                        JobStatus.PENDING,
                        SystemUser.SYSTEM,
                        createdAt
                ))
        );

        return new DispatchableJob(job, 12L, clonePath);
    }
}
