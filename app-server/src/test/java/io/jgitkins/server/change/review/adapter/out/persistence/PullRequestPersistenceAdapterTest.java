package io.jgitkins.server.change.review.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.adapter.out.persistence.support.PullRequestDomainMapper;
import io.jgitkins.server.change.review.adapter.out.persistence.translator.PullRequestEntityMbgMapper;
import io.jgitkins.server.change.review.adapter.out.persistence.model.PullRequestEntity;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PullRequestPersistenceAdapterTest {
    private final PullRequestEntityMbgMapper mapper = mock(PullRequestEntityMbgMapper.class);
    private final PullRequestDomainMapper domainMapper = mock(PullRequestDomainMapper.class);
    private final PullRequestPersistenceAdapter adapter = new PullRequestPersistenceAdapter(mapper, domainMapper);
    private final PullRequest pullRequest = PullRequest.create(ReviewRepositoryId.of(1L),
            BranchHeadSnapshot.of("feature", "feature-head"), BranchHeadSnapshot.of("main", "base-head"));

    @Test
    void saveInsertUsesGeneratedIdAndTimestamps() {
        PullRequestEntity entity = new PullRequestEntity();
        when(domainMapper.toEntity(pullRequest)).thenReturn(entity);
        doAnswer(invocation -> { entity.setId(42L); return null; }).when(mapper).insertSelective(entity);

        PullRequest saved = adapter.save(pullRequest);

        assertEquals(42L, saved.getId().value());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        verify(mapper).insertSelective(entity);
    }

    @Test
    void saveUpdateRefreshesUpdatedTimestamp() {
        PullRequest existing = pullRequest.withIdentity(PullRequestId.of(7L), pullRequest.getCreatedAt(), pullRequest.getUpdatedAt());
        PullRequestEntity entity = new PullRequestEntity();
        when(domainMapper.toEntity(existing)).thenReturn(entity);

        PullRequest updated = adapter.save(existing);

        assertEquals(7L, updated.getId().value());
        assertEquals(existing.getCreatedAt(), updated.getCreatedAt());
        assertNotNull(updated.getUpdatedAt());
        verify(mapper).updateByPrimaryKeySelective(entity);
    }

    @Test
    void findByIdMapsFirstEntity() {
        PullRequestEntity entity = new PullRequestEntity();
        when(mapper.selectByCondition(any())).thenReturn(List.of(entity));
        when(domainMapper.toDomain(entity)).thenReturn(pullRequest.withIdentity(PullRequestId.of(3L), pullRequest.getCreatedAt(), pullRequest.getUpdatedAt()));

        assertEquals(3L, adapter.findById(PullRequestId.of(3L)).orElseThrow().getId().value());
    }

    @Test
    void saveWrapsMapperFailureAndPreservesCause() {
        RuntimeException cause = new RuntimeException("insert failed");
        when(domainMapper.toEntity(pullRequest)).thenThrow(cause);

        InfrastructureException exception = assertThrows(InfrastructureException.class, () -> adapter.save(pullRequest));
        assertEquals(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, exception.getErrorCode());
        assertEquals("Database operation failed during save pull request", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void findByIdWrapsMapperFailureAndPreservesCause() {
        RuntimeException cause = new RuntimeException("select failed");
        when(mapper.selectByCondition(any())).thenThrow(cause);

        InfrastructureException exception = assertThrows(InfrastructureException.class, () -> adapter.findById(PullRequestId.of(1L)));
        assertEquals(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, exception.getErrorCode());
        assertEquals("Database operation failed during find pull request by id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
