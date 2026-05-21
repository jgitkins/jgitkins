package io.jgitkins.server.execution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import io.jgitkins.server.execution.application.support.JobDispatchResultAssembler;
import io.jgitkins.server.execution.application.support.RunnerDispatchContextResolver;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import io.jgitkins.server.domain.model.vo.SequenceNumber;
import io.jgitkins.server.identity.access.domain.vo.SystemUser;
import io.jgitkins.server.identity.access.domain.vo.UserId;
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
    private JobDispatchQueryPort jobDispatchQueryPort;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private RunnerDispatchContextResolver runnerDispatchContextResolver;

    @Mock
    private JobDispatchResultAssembler jobDispatchResultAssembler;

    @Mock
    private CloneUrlBuilder cloneUrlBuilder;

    @InjectMocks
    private JobDispatchService service;

    @Test
    void dispatch_returnsEmpty_whenRunnerTokenMissing() {
        DispatchJobCommand command = new DispatchJobCommand(" ");

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(runnerDispatchContextResolver).resolve(" ");
        verifyNoInteractions(jobDispatchQueryPort, jobRepository, jobDispatchResultAssembler, cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsEmpty_whenRunnerNotFound() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        when(runnerDispatchContextResolver.resolve("token")).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(runnerDispatchContextResolver).resolve("token");
        verifyNoInteractions(jobDispatchQueryPort, jobRepository, jobDispatchResultAssembler, cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsEmpty_whenNoDispatchableJobForRunner() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        RunnerDispatchContext runnerContext = new RunnerDispatchContext(7L, io.jgitkins.server.execution.application.contract.internal.JobDispatchScope.GLOBAL, null);

        when(runnerDispatchContextResolver.resolve("token")).thenReturn(Optional.of(runnerContext));
        when(jobDispatchQueryPort.fetchNextJob(any(RunnerDispatchContext.class))).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(runnerDispatchContextResolver).resolve("token");
        verify(jobDispatchQueryPort).fetchNextJob(any(RunnerDispatchContext.class));
        verifyNoInteractions(jobRepository, jobDispatchResultAssembler, cloneUrlBuilder);
    }

    @Test
    void dispatch_returnsResult_whenDispatchSucceeds() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        RunnerDispatchContext runnerContext = new RunnerDispatchContext(7L, io.jgitkins.server.execution.application.contract.internal.JobDispatchScope.GLOBAL, null);
        DispatchableJob dispatchableJob = dispatchableJob(101L, 55L, "org/repo.git");

        when(runnerDispatchContextResolver.resolve("token")).thenReturn(Optional.of(runnerContext));
        when(jobDispatchQueryPort.fetchNextJob(any(RunnerDispatchContext.class))).thenReturn(Optional.of(dispatchableJob));
        when(jobRepository.appendHistoryIfCurrent(any(Job.class), any(JobHistory.class))).thenReturn(Optional.of(999L));
        when(cloneUrlBuilder.build("org/repo.git")).thenReturn("https://git.example/org/repo.git");
        when(jobDispatchResultAssembler.assemble(any(RunnerDispatchContext.class), any(DispatchableJob.class), any(Job.class), any(Long.class), any(String.class)))
                .thenReturn(new JobDispatchResult(
                        101L,
                        999L,
                        7L,
                        55L,
                        12L,
                        "abc123def456",
                        "main",
                        3L,
                        LocalDateTime.of(2026, 3, 12, 10, 30),
                        "https://git.example/org/repo.git"
                ));

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

        verify(jobRepository).appendHistoryIfCurrent(any(Job.class), any(JobHistory.class));
        verify(cloneUrlBuilder).build("org/repo.git");
    }

    @Test
    void dispatch_returnsEmpty_whenAnotherDispatcherAlreadySavedHistory() {
        DispatchJobCommand command = new DispatchJobCommand("token");
        RunnerDispatchContext runnerContext = new RunnerDispatchContext(7L, io.jgitkins.server.execution.application.contract.internal.JobDispatchScope.GLOBAL, null);
        DispatchableJob dispatchableJob = dispatchableJob(101L, 55L, "org/repo.git");

        when(runnerDispatchContextResolver.resolve("token")).thenReturn(Optional.of(runnerContext));
        when(jobDispatchQueryPort.fetchNextJob(any(RunnerDispatchContext.class))).thenReturn(Optional.of(dispatchableJob));
        when(jobRepository.appendHistoryIfCurrent(any(Job.class), any(JobHistory.class))).thenReturn(Optional.empty());

        Optional<JobDispatchResult> result = service.dispatch(command);

        assertThat(result).isEmpty();
        verify(jobRepository).appendHistoryIfCurrent(any(Job.class), any(JobHistory.class));
        verifyNoInteractions(jobDispatchResultAssembler, cloneUrlBuilder);
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

        return new DispatchableJob(jobId, job, 12L, clonePath);
    }
}
