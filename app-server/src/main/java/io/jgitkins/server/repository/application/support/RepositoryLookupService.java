package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;


import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryLookupService {

    private final RepositoryRepository repositoryRepository;
    private final UserNamespacePort userNamespacePort;
    private final OrganizationNamespacePort organizationNamespacePort;

    public Optional<Repository> resolveByPath(String namespace, String repoName) {
        String normalizedNamespace = trimSlashes(namespace);
        String normalizedRepoName = trimSlashes(repoName);

        return findByClonePath(normalizedNamespace, normalizedRepoName)
                .or(() -> findUserOwned(normalizedNamespace, normalizedRepoName))
                .or(() -> findOrganizationOwned(normalizedNamespace, normalizedRepoName));
    }

    public Optional<Repository> resolveByOwner(
            OwnerType ownerType,
            String ownerName,
            String repositoryName) {
        if (ownerType == null) {
            throw new IllegalArgumentException("ownerType must not be null");
        }

        String normalizedOwnerName = trimSlashes(ownerName);
        String normalizedRepositoryName = trimSlashes(repositoryName);

        return switch (ownerType) {
            case USER -> findUserOwned(normalizedOwnerName, normalizedRepositoryName);
            case ORGANIZATION -> findOrganizationOwned(normalizedOwnerName, normalizedRepositoryName);
        };
    }

    private Optional<Repository> findByClonePath(String namespace, String repoName) {
        String clonePath = RepositoryPathHelper.buildClonePath(namespace, repoName);
        return repositoryRepository.findByClonePath(clonePath);
    }

    private Optional<Repository> findUserOwned(String namespace, String repoName) {
        Optional<Long> userId = userNamespacePort.findUserIdByUsername(namespace);
        if (userId.isEmpty()) {
            return Optional.empty();
        }

        return repositoryRepository.findByOwnerAndName(
                OwnerType.USER,
                OwnerId.of(userId.get()),
                RepositoryName.from(repoName));
    }

    private Optional<Repository> findOrganizationOwned(String namespace, String repoName) {
        Optional<Long> organizationId = findOrganizationByNamespace(namespace);
        if (organizationId.isEmpty()) {
            return Optional.empty();
        }

        return repositoryRepository.findByOwnerAndPath(
                OwnerType.ORGANIZATION,
                OwnerId.of(organizationId.get()),
                RepositoryPath.from(repoName));
    }

    private Optional<Long> findOrganizationByNamespace(String namespace) {
        try {
            return organizationNamespacePort.findOrganizationIdByName(namespace);
        } catch (IllegalArgumentException ex) {
            log.debug("invalid organization namespace. namespace={}", namespace, ex);
            return Optional.empty();
        }
    }

    private String trimSlashes(String value) {
        if (value == null) {
            throw new IllegalArgumentException("path segment must not be null");
        }
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
