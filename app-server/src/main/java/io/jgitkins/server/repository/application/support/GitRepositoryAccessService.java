package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitRepositoryAccessService {
    private final RepositoryLookupService repositoryLookupService;
    private final OrganizationMembershipPort organizationMembershipPort;
    private final RepositoryMemberPersistencePort repositoryMemberPort;

    public boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) return false;
        Repository repo = repository.get();
        if (repo.getVisibility() == RepositoryVisibility.PUBLIC) return true;
        return resolvePermission(repo, userId).member();
    }
    public boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return resolveRepository(ownerType, ownerName, repositoryName).map(repo -> resolvePermission(repo, userId).writable()).orElse(false);
    }
    public RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return resolveRepository(ownerType, ownerName, repositoryName).map(repo -> resolvePermission(repo, userId)).orElseGet(RepositoryPermission::none);
    }
    public boolean isPublicRepo(OwnerType ownerType, String ownerName, String repositoryName) {
        return resolveRepository(ownerType, ownerName, repositoryName).map(repo -> repo.getVisibility() == RepositoryVisibility.PUBLIC).orElse(false);
    }
    public Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName) {
        return resolveRepository(ownerType, ownerName, repositoryName).map(repo -> repo.getVisibility() == RepositoryVisibility.PUBLIC);
    }
    public RepositoryPermission resolvePermission(Repository repo, Long userId) {
        if (repo == null) return RepositoryPermission.none();
        if (repo.getVisibility() == RepositoryVisibility.PUBLIC && userId == null) return new RepositoryPermission("PUBLIC_READ_ONLY", false, true);
        if (userId == null) return RepositoryPermission.anonymous();
        if (repo.getOwnerType() == OwnerType.USER && repo.getOwnerId() != null && repo.getOwnerId().getValue().equals(userId)) return new RepositoryPermission("OWNER", true, true);
        Optional<RepositoryMember> member = repositoryMemberPort.findByRepositoryIdAndUserId(repo.getId(), RepositoryMemberUserId.of(userId));
        if (member.isPresent()) {
            RepositoryMemberRole role = member.get().getRole();
            return new RepositoryPermission("REPOSITORY_" + role.name(), role == RepositoryMemberRole.WRITER || role == RepositoryMemberRole.MAINTAINER, true);
        }
        if (repo.getOwnerType() == OwnerType.ORGANIZATION && repo.getOwnerId() != null) {
            Optional<OrganizationMembershipRole> role = organizationMembershipPort.findRoleByOrganizationIdAndUserId(repo.getOwnerId().getValue(), userId);
            if (role.isPresent()) {
                OrganizationMembershipRole value = role.get();
                return new RepositoryPermission("ORGANIZATION_" + value.name(), value != OrganizationMembershipRole.MEMBER, true);
            }
        }
        return RepositoryPermission.none();
    }
    private Optional<Repository> resolveRepository(OwnerType ownerType, String ownerName, String repositoryName) {
        if (ownerName == null || ownerName.isBlank() || repositoryName == null || repositoryName.isBlank()) return Optional.empty();
        return ownerType == null ? repositoryLookupService.resolveByPath(ownerName, repositoryName) : repositoryLookupService.resolveByOwner(ownerType, ownerName, repositoryName);
    }
}
