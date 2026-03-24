package io.jgitkins.server.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.JobDispatchScope;
import io.jgitkins.server.application.dto.RunnerDispatchContext;
import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.JobHistory;
import io.jgitkins.server.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobDispatchQueryMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.DispatchableJobRow;
import io.jgitkins.server.infrastructure.persistence.model.JobHistoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobPersistenceAdapterTest {

    @Mock
    private JobDispatchQueryMapper jobDispatchQueryMapper;

    @Mock
    private JobEntityMbgMapper jobEntityMbgMapper;

    @Mock
    private JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper;

    @Mock
    private JobDomainMapper jobDomainMapper;

    @InjectMocks
    private JobPersistenceAdapter adapter;

    @Test
    void findNextDispatchableJob_returnsPendingJobWithinRunnerScope() {
        RunnerDispatchContext context = new RunnerDispatchContext(7L, JobDispatchScope.ORGANIZE, 33L);
        DispatchableJobRow row = dispatchableJobRow(100L, 10L, "ORGANIZATION", 33L, "/org/repo.git");
        JobHistoryEntity latestPending = jobHistoryEntity(100L, "PENDING", LocalDateTime.of(2026, 3, 12, 10, 5));
        JobHistory mappedHistory = mock(JobHistory.class);
        Job mappedJob = mock(Job.class);

        when(jobDispatchQueryMapper.selectNextDispatchableJob("ORGANIZE", 33L)).thenReturn(row);
        when(jobHistoryEntityMbgMapper.selectByCondition(any())).thenReturn(List.of(latestPending));
        when(jobDomainMapper.toHistoryDomain(any(JobHistoryEntity.class))).thenReturn(mappedHistory);
        when(jobDomainMapper.toDomain(any(DispatchableJobRow.class), any())).thenReturn(mappedJob);

        var result = adapter.findNextDispatchableJob(context);

        assertThat(result).isPresent();
        assertThat(result.get().job()).isSameAs(mappedJob);
        assertThat(result.get().organizeId()).isEqualTo(33L);
        assertThat(result.get().repositoryClonePath()).isEqualTo("/org/repo.git");
        verify(jobDispatchQueryMapper).selectNextDispatchableJob("ORGANIZE", 33L);
    }

    @Test
    void findNextDispatchableJob_returnsEmpty_whenNoDispatchableJobExists() {
        RunnerDispatchContext context = new RunnerDispatchContext(7L, JobDispatchScope.REPOSITORY, 10L);
        when(jobDispatchQueryMapper.selectNextDispatchableJob("REPOSITORY", 10L)).thenReturn(null);

        var result = adapter.findNextDispatchableJob(context);

        assertThat(result).isEmpty();
        verify(jobDispatchQueryMapper).selectNextDispatchableJob("REPOSITORY", 10L);
        verifyNoMoreInteractions(jobDomainMapper);
    }

    private DispatchableJobRow dispatchableJobRow(Long jobId,
                                                  Long repositoryId,
                                                  String ownerType,
                                                  Long ownerId,
                                                  String clonePath) {
        return new DispatchableJobRow(
                jobId,
                repositoryId,
                "abc123",
                "main",
                3L,
                LocalDateTime.of(2026, 3, 12, 10, 0),
                ownerType,
                ownerId,
                clonePath
        );
    }

    private JobHistoryEntity jobHistoryEntity(Long jobId, String status, LocalDateTime createdAt) {
        JobHistoryEntity entity = new JobHistoryEntity();
        entity.setId(jobId + 1000);
        entity.setJobId(jobId);
        entity.setStatus(status);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
