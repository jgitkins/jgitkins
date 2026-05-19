package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
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
    private final UserPersistencePort userPort;
    private final OrganizePersistencePort organizePort;

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
        Optional<User> user = userPort.findByUsername(namespace);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        return repositoryRepository.findByOwnerAndName(
                OwnerType.USER,
                OwnerId.of(user.get().getId()),
                RepositoryName.from(repoName));
    }

    private Optional<Repository> findOrganizationOwned(String namespace, String repoName) {
        Optional<Organize> organize = findOrganizationByNamespace(namespace);
        if (organize.isEmpty()) {
            return Optional.empty();
        }

        return repositoryRepository.findByOwnerAndPath(
                OwnerType.ORGANIZATION,
                OwnerId.of(organize.get().getId().getValue()),
                RepositoryPath.from(repoName));
    }

    private Optional<Organize> findOrganizationByNamespace(String namespace) {
        try {
            return organizePort.findByName(OrganizeName.from(namespace));
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
