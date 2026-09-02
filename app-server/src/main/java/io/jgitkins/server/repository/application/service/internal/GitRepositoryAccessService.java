package io.jgitkins.server.repository.application.service.internal;

import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.repository.application.contract.RepositoryPermission;
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
        return decide(repo.getVisibility() == RepositoryVisibility.PUBLIC,
                repo.getOwnerType(),
                repo.getOwnerId() != null ? repo.getOwnerId().getValue() : null,
                repo.getId(),
                userId);
    }

    /**
     * The same decision, made from the read model instead of the aggregate.
     *
     * <p>Added for task 2.65, which moved read authorization onto the {@code RepositoryResult} the route
     * already loaded. It delegates to the same private rule rather than restating it: two copies of a
     * permission rule drift, and the direction they drift in is whichever one a later change forgets —
     * which for this rule means either denying an owner or admitting a stranger.
     */
    public RepositoryPermission resolvePermission(RepositoryResult repo, Long userId) {
        if (repo == null) return RepositoryPermission.none();
        return decide("PUBLIC".equals(repo.visibility()),
                repo.ownerType() != null ? OwnerType.from(repo.ownerType()) : null,
                repo.ownerId(),
                repo.id() != null ? RepositoryId.of(repo.id()) : null,
                userId);
    }

    private RepositoryPermission decide(boolean isPublic, OwnerType ownerType, Long ownerId,
                                        RepositoryId repositoryId, Long userId) {
        // Public plus anonymous is a real, named outcome rather than a fallthrough: it is readable and
        // not writable, and collapsing it into "none" would break every anonymous read of a public
        // repository.
        if (isPublic && userId == null) return new RepositoryPermission("PUBLIC_READ_ONLY", false, true);
        if (userId == null) return RepositoryPermission.anonymous();
        if (ownerType == OwnerType.USER && ownerId != null && ownerId.equals(userId)) {
            return new RepositoryPermission("OWNER", true, true);
        }
        Optional<RepositoryMember> member = repositoryId == null
                ? Optional.empty()
                : repositoryMemberPort.findByRepositoryIdAndUserId(
                        repositoryId, RepositoryMemberUserId.of(userId));
        if (member.isPresent()) {
            RepositoryMemberRole role = member.get().getRole();
            return new RepositoryPermission("REPOSITORY_" + role.name(),
                    role == RepositoryMemberRole.WRITER || role == RepositoryMemberRole.MAINTAINER, true);
        }
        if (ownerType == OwnerType.ORGANIZATION && ownerId != null) {
            Optional<OrganizationMembershipRole> role =
                    organizationMembershipPort.findRoleByOrganizationIdAndUserId(ownerId, userId);
            if (role.isPresent()) {
                OrganizationMembershipRole value = role.get();
                return new RepositoryPermission("ORGANIZATION_" + value.name(),
                        value != OrganizationMembershipRole.MEMBER, true);
            }
        }
        return RepositoryPermission.none();
    }

    private Optional<Repository> resolveRepository(OwnerType ownerType, String ownerName, String repositoryName) {
        if (ownerName == null || ownerName.isBlank() || repositoryName == null || repositoryName.isBlank()) return Optional.empty();
        return ownerType == null ? repositoryLookupService.resolveByPath(ownerName, repositoryName) : repositoryLookupService.resolveByOwner(ownerType, ownerName, repositoryName);
    }
}
