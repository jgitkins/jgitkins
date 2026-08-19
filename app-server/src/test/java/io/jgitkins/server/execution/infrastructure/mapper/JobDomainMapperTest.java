package io.jgitkins.server.execution.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.infrastructure.persistence.model.JobHistoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobDomainMapperTest {
    private final JobDomainMapper mapper = new JobDomainMapper() {
        @Override
        public io.jgitkins.server.execution.infrastructure.persistence.model.JobEntity toEntity(
                io.jgitkins.server.execution.domain.aggregate.Job job) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void historyMappingDerivesOrderedSequenceAndSystemActor() {
        JobHistoryEntity first = entity(10L, LocalDateTime.of(2026, 1, 1, 0, 0));
        JobHistoryEntity second = entity(11L, LocalDateTime.of(2026, 1, 1, 0, 1));

        List<JobHistory> histories = mapper.toHistoryDomain(List.of(first, second));

        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getSeqNo().getValue()).isEqualTo(1);
        assertThat(histories.get(1).getSeqNo().getValue()).isEqualTo(2);
        assertThat(histories).allSatisfy(history -> assertThat(history.getCreatedBy()).isEqualTo(ExecutionSystemActor.SYSTEM));
    }

    private JobHistoryEntity entity(Long id, LocalDateTime createdAt) {
        JobHistoryEntity entity = new JobHistoryEntity();
        entity.setId(id);
        entity.setJobId(1L);
        entity.setStatus("PENDING");
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
