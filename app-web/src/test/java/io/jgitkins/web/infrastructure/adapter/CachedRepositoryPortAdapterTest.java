package io.jgitkins.web.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.contract.CommitSummary;
import io.jgitkins.web.application.contract.RepositoryFileEntry;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import io.jgitkins.web.infrastructure.cache.RepositoryFileIndexCacheSupport;
import io.jgitkins.web.infrastructure.cache.RepositoryTreeCacheSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachedRepositoryPortAdapterTest {

	@Mock
	private JGitkinsServerAdapter delegate;

	@Mock
	private RepositoryTreeCacheSupport repositoryTreeCacheSupport;

	@Mock
	private RepositoryFileIndexCacheSupport repositoryFileIndexCacheSupport;

	private CachedRepositoryPortAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new CachedRepositoryPortAdapter(delegate, repositoryTreeCacheSupport, repositoryFileIndexCacheSupport);
	}

	@Test
	void fetchRepositoryTree_returnsCachedValue_whenCacheHit() {
		List<RepositoryFileEntry> cached = List.of(new RepositoryFileEntry("1", "a.txt", "a.txt", "blob", "100644", 10L));
		when(delegate.fetchCommits("users/alice", "demo", "main"))
				.thenReturn(List.of(new CommitSummary("c1", "alice", "a@test.com", "msg", LocalDateTime.now())));
		when(repositoryTreeCacheSupport.get("users/alice", "demo", "main", "src", "c1"))
				.thenReturn(Optional.of(cached));

		List<RepositoryFileEntry> result = adapter.fetchRepositoryTree("users/alice", "demo", "main", "src");

		assertThat(result).containsExactlyElementsOf(cached);
		verify(delegate, never()).fetchRepositoryTree("users/alice", "demo", "main", "src");
	}

	@Test
	void fetchRepositoryFileIndex_fetchesAndCaches_whenCacheMiss() {
		List<RepositoryFileIndexEntry> loaded = List.of(new RepositoryFileIndexEntry("a.txt", "a.txt", "blob"));
		when(delegate.fetchCommits("users/alice", "demo", "main"))
				.thenReturn(List.of(new CommitSummary("c1", "alice", "a@test.com", "msg", LocalDateTime.now())));
		when(repositoryFileIndexCacheSupport.get("users/alice", "demo", "main", "c1"))
				.thenReturn(Optional.empty());
		when(delegate.fetchRepositoryFileIndex("users/alice", "demo", "main")).thenReturn(loaded);

		List<RepositoryFileIndexEntry> result = adapter.fetchRepositoryFileIndex("users/alice", "demo", "main");

		assertThat(result).containsExactlyElementsOf(loaded);
		verify(repositoryFileIndexCacheSupport).put(
				eq("users/alice"),
				eq("demo"),
				eq("main"),
				eq("c1"),
				eq(loaded),
				any()
		);
	}
}
