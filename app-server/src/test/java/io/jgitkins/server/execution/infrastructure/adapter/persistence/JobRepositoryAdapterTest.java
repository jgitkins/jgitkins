package io.jgitkins.server.execution.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import io.jgitkins.server.execution.infrastructure.persistence.model.JobHistoryEntity;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import io.jgitkins.server.shared.domain.model.vo.SequenceNumber;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobRepositoryAdapterTest {
    @Mock JobEntityMbgMapper jobMapper;
    @Mock JobHistoryEntityMbgMapper historyMapper;
    @Mock JobDomainMapper domainMapper;

    @Test void appendHistoryIfCurrent_locksComparesAndReturnsGeneratedId() {
        JobRepositoryAdapter adapter = new JobRepositoryAdapter(jobMapper, historyMapper, domainMapper);
        LocalDateTime created = LocalDateTime.of(2026, 8, 20, 12, 0);
        JobHistory previous = JobHistory.reconstruct(JobHistoryId.of("10"), JobId.of("1"), SequenceNumber.first(), null, JobStatus.PENDING, ExecutionSystemActor.SYSTEM, created);
        Job job = Job.reconstruct(JobId.of("1"), ExecutionRepositoryId.of(2L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(3L), created, List.of(previous));
        job.publish(io.jgitkins.server.execution.domain.vo.RunnerId.of("7"));
        JobHistoryEntity locked = new JobHistoryEntity();
        locked.setId(10L); locked.setStatus("PENDING"); locked.setCreatedAt(created);
        JobHistoryEntity inserted = new JobHistoryEntity(); inserted.setId(11L);
        when(historyMapper.selectLatestHistoryForUpdate(1L)).thenReturn(locked);
        when(historyMapper.selectByCondition(any())).thenReturn(List.of(locked));
        when(domainMapper.toHistoryEntity(any(JobHistory.class), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(inserted);
        assertThat(adapter.appendHistoryIfCurrent(job, previous)).contains(11L);
        verify(historyMapper).selectLatestHistoryForUpdate(1L);
        verify(historyMapper).insertSelective(inserted);
    }
}
