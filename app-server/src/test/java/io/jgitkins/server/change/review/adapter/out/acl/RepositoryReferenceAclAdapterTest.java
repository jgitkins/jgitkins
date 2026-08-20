package io.jgitkins.server.change.review.adapter.out.acl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepositoryReferenceAclAdapterTest {
    private final RepositoryLookupService lookup = mock(RepositoryLookupService.class);
    private final RepositoryRepository repositories = mock(RepositoryRepository.class);
    private final RepositoryNamespaceResolver namespaces = mock(RepositoryNamespaceResolver.class);
    private final RepositoryReferenceAclAdapter adapter = new RepositoryReferenceAclAdapter(lookup, repositories, namespaces);

    @Test
    void findByPathMapsCanonicalReference() {
        Repository repository = repository();
        when(lookup.resolveByPath("alice", "demo")).thenReturn(Optional.of(repository));
        when(namespaces.resolve(repository)).thenReturn("alice");

        ReviewRepositoryReference result = adapter.findByPath("alice", "demo").orElseThrow();
        assertEquals(1L, result.id().value());
        assertEquals("alice", result.namespace());
        assertEquals("demo", result.repoName());
    }

    @Test
    void findByIdDelegatesUsingRepositoryId() {
        Repository repository = repository();
        when(repositories.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(namespaces.resolve(repository)).thenReturn("alice");

        assertEquals("demo", adapter.findById(ReviewRepositoryId.of(1L)).orElseThrow().repoName());
        verify(repositories).findById(RepositoryId.of(1L));
    }

    @Test
    void missingPathAndIdRemainEmpty() {
        when(lookup.resolveByPath("alice", "missing")).thenReturn(Optional.empty());
        when(repositories.findById(RepositoryId.of(9L))).thenReturn(Optional.empty());

        assertTrue(adapter.findByPath("alice", "missing").isEmpty());
        assertTrue(adapter.findById(ReviewRepositoryId.of(9L)).isEmpty());
    }

    @Test
    void resolverFailurePropagatesUnchanged() {
        Repository repository = repository();
        RuntimeException failure = new RuntimeException("owner lookup failed");
        when(lookup.resolveByPath("alice", "demo")).thenReturn(Optional.of(repository));
        when(namespaces.resolve(repository)).thenThrow(failure);

        RuntimeException actual = assertThrows(RuntimeException.class, () -> adapter.findByPath("alice", "demo"));
        assertSame(failure, actual);
    }

    private Repository repository() {
        Repository repository = mock(Repository.class);
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.getPath()).thenReturn(RepositoryPath.from("demo"));
        return repository;
    }
}
