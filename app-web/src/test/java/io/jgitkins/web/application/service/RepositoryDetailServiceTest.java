package io.jgitkins.web.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.contract.RepositoryDetailData;
import io.jgitkins.web.application.contract.RepositoryFileEntry;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import io.jgitkins.web.application.contract.RepositoryOverviewResult;
import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.translator.RepositoryDetailDataMapper;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.application.service.support.RepositoryDetailDataFactory;
import io.jgitkins.web.application.service.support.RepositoryFileSearchPolicy;
import io.jgitkins.web.application.service.support.RepositoryKeyResolver;
import java.util.List;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryDetailServiceTest {

    @Mock
    private RepositoryPort repositoryPort;

    private RepositoryDetailService service;

    @BeforeEach
    void setUp() {
        RepositoryDetailDataMapper mapper = Mappers.getMapper(RepositoryDetailDataMapper.class);
        service = new RepositoryDetailService(
                repositoryPort,
                new RepositoryDetailDataFactory(mapper),
                new RepositoryFileSearchPolicy(),
                new RepositoryKeyResolver()
        );
    }

    @Test
    void loadRepositoryByPath_loadsTreeFromPort() {
        RepositorySummary summary = repositorySummary();
        RepositoryOverviewResult overview = new RepositoryOverviewResult(summary, List.of(), List.of(), "main", "OWNER", true);
        List<RepositoryFileEntry> loaded = List.of(entry("loaded-file"));

        when(repositoryPort.fetchRepositoryOverviewByPath("users/alice", "demo", "main")).thenReturn(overview);
        when(repositoryPort.fetchRepositoryTree("users/alice", "demo", "main", "src"))
                .thenReturn(loaded);

        RepositoryDetailData result = service.loadRepositoryByPath("users/alice", "demo", "main", "src");

        assertThat(result.files()).hasSize(1);
        assertThat(result.files().get(0).name()).isEqualTo("loaded-file");
        verify(repositoryPort).fetchRepositoryTree("users/alice", "demo", "main", "src");
    }

    @Test
    void searchRepositoryFilesByPath_usesIndexCacheAndFiltersByKeyword() {
        List<RepositoryFileIndexEntry> cached = List.of(
                new RepositoryFileIndexEntry("README.md", "README.md", "blob"),
                new RepositoryFileIndexEntry("RepoService.java", "src/RepoService.java", "blob"),
                new RepositoryFileIndexEntry("Other.java", "src/Other.java", "blob")
        );

        when(repositoryPort.fetchRepositoryFileIndex("users/alice", "demo", "main")).thenReturn(cached);

        List<RepositoryFileIndexEntry> result = service.searchRepositoryFilesByPath("users/alice", "demo", "main", "repo", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).path()).isEqualTo("src/RepoService.java");
        verify(repositoryPort).fetchRepositoryFileIndex("users/alice", "demo", "main");
    }

    @Test
    void loadRepositoryByPath_returnsNotFound_whenOverviewMissing() {
        when(repositoryPort.fetchRepositoryOverviewByPath("users/alice", "demo", "main")).thenReturn(null);

        RepositoryDetailData result = service.loadRepositoryByPath("users/alice", "demo", "main", "src");

        assertThat(result.repository()).isNull();
        assertThat(result.errorMessage()).isEqualTo("Repository not found.");
    }

    @Test
    void loadRepositoryByPath_loadsTree_whenOverviewExists() {
        RepositorySummary summary = repositorySummary();
        RepositoryOverviewResult overview = new RepositoryOverviewResult(summary, List.of(), List.of(), "main", "OWNER", true);
        List<RepositoryFileEntry> loaded = List.of(entry("loaded-file"));

        when(repositoryPort.fetchRepositoryOverviewByPath("users/alice", "demo", "main")).thenReturn(overview);
        when(repositoryPort.fetchRepositoryTree("users/alice", "demo", "main", "src")).thenReturn(loaded);

        RepositoryDetailData result = service.loadRepositoryByPath("users/alice", "demo", "main", "src");

        assertThat(result.files()).hasSize(1);
        assertThat(result.files().get(0).name()).isEqualTo("loaded-file");
        verify(repositoryPort).fetchRepositoryTree("users/alice", "demo", "main", "src");
    }

    private RepositorySummary repositorySummary() {
        return new RepositorySummary(
                1L,
                "USER",
                "demo",
                "users/alice/demo",
                "main",
                "PUBLIC",
                null,
                1L,
                "users/alice/demo.git",
                null,
                null,
                null,
                null
        );
    }

    private RepositoryFileEntry entry(String name) {
        return new RepositoryFileEntry("id", name, name, "blob", "100644", 12L);
    }
}
