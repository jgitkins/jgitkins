package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.dto.ExploreRepoSummary;
import io.jgitkins.web.application.dto.ExploreSummary;
import io.jgitkins.web.application.dto.OrganizeSummary;
import io.jgitkins.web.application.dto.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.application.port.in.facade.ExploreFacadeUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.application.port.out.UserPort;
import io.jgitkins.web.infrastructure.util.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExploreFacade implements ExploreFacadeUseCase {

    private final RepositoryPort repositoryPort;
    private final OrganizePort organizePort;
    private final UserPort userPort;

    @Override
    public ExploreSummary getExploreSummary(String type) {
        String resolvedType = resolveExploreType(type);

        List<ExploreRepoSummary> repositories = null;
        List<OrganizeSummary> organizations = null;
        List<io.jgitkins.web.application.dto.UserSummary> users = null;

        if ("repositories".equals(resolvedType)) {
            repositories = loadPublicRepositories();
        } else if ("organizations".equals(resolvedType)) {
            organizations = organizePort.fetchOrganizes().organizes();
        } else if ("users".equals(resolvedType)) {
            users = userPort.fetchUsers();
        }

        return new ExploreSummary(resolvedType, repositories, organizations, users);
    }

    private String resolveExploreType(String type) {
        if (type == null || type.isBlank()) {
            return "repositories";
        }
        return switch (type) {
            case "repositories", "users", "organizations" -> type;
            default -> "repositories";
        };
    }

    private List<ExploreRepoSummary> loadPublicRepositories() {
        Map<Long, String> organizeNameMap = organizePort.fetchOrganizes()
                .organizes()
                .stream()
                .collect(Collectors.toMap(OrganizeSummary::id, OrganizeSummary::name, (a, b) -> a));

        return repositoryPort.fetchRepositories()
                .stream()
                .filter(this::isPublicRepository)
                .map(repository -> toExploreView(repository, organizeNameMap))
                .toList();
    }

    private boolean isPublicRepository(RepositorySummary summary) {
        return summary != null
                && summary.visibility() != null
                && "PUBLIC".equalsIgnoreCase(summary.visibility());
    }

    private ExploreRepoSummary toExploreView(RepositorySummary summary, Map<Long, String> organizeNameMap) {
        RepositoryKey key = resolveRepositoryKey(summary);
        String namespace = resolveNamespace(summary, key, organizeNameMap);
        String repoName = key == null ? summary.name() : key.repoName();
        String description = summary.description();
        String visibility = summary.visibility() == null ? "" : summary.visibility().toUpperCase(Locale.ROOT);
        return new ExploreRepoSummary(namespace, repoName, description, visibility, summary.createdAt());
    }

    private String resolveNamespace(RepositorySummary summary,
            RepositoryKey key,
            Map<Long, String> organizeNameMap) {
        if (key != null) {
            String normalized = PathUtils.lastSegment(key.namespace());
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        if (summary == null || summary.ownerType() == null) {
            return "unknown";
        }
        if ("ORGANIZATION".equalsIgnoreCase(summary.ownerType()) && summary.ownerId() != null) {
            String organizeName = organizeNameMap.get(summary.ownerId());
            if (organizeName != null && !organizeName.isBlank()) {
                return organizeName;
            }
        }
        return "unknown";
    }

    private RepositoryKey resolveRepositoryKey(RepositorySummary repository) {
        if (repository == null) {
            return null;
        }
        return PathUtils.resolveRepositoryKey(repository.clonePath(), repository.path());
    }
}
