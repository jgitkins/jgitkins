package io.jgitkins.server.repository.application.support;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.identity.access.domain.vo.UserId;
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
    private final OrganizeMemberPersistencePort organizeMemberPort;
    private final RepositoryMemberPersistencePort repositoryMemberPort;

    public boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) {
            return false;
        }
        Repository repo = repository.get();
        if (repo.getVisibility() == io.jgitkins.server.repository.domain.vo.RepositoryVisibility.PUBLIC) {
            return true;
        }
        return resolvePermission(repo, userId).member();
    }

    public boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) {
            return false;
        }
        return resolvePermission(repository.get(), userId).writable();
    }

    public RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        Optional<Repository> repository = resolveRepository(ownerType, ownerName, repositoryName);
        if (repository.isEmpty()) {
            return RepositoryPermission.none();
        }
        return resolvePermission(repository.get(), userId);
    }

    public boolean isPublicRepo(OwnerType ownerType, String ownerName, String repositoryName) {
        return resolveRepository(ownerType, ownerName, repositoryName)
                .map(repo -> repo.getVisibility() == io.jgitkins.server.repository.domain.vo.RepositoryVisibility.PUBLIC)
                .orElse(false);
    }

    public Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName) {
        log.debug("find repository ownerType: [{}], ownerName: [{}], repositoryName: [{}]", ownerType, ownerName, repositoryName);
        return resolveRepository(ownerType, ownerName, repositoryName)
                .map(repo -> repo.getVisibility() == RepositoryVisibility.PUBLIC);
    }

    public RepositoryPermission resolvePermission(Repository repo, Long userId) {
        if (repo == null) {
            return RepositoryPermission.none();
        }
        if (repo.getVisibility() == RepositoryVisibility.PUBLIC && userId == null) {
            return new RepositoryPermission("PUBLIC_READ_ONLY", false, true);
        }
        if (userId == null) {
            return RepositoryPermission.anonymous();
        }
        UserId uid = UserId.of(userId);
        if (repo.getOwnerType() == OwnerType.USER
                && repo.getOwnerId() != null
                && repo.getOwnerId().getValue().equals(uid.getValue())) {
            return new RepositoryPermission("OWNER", true, true);
        }

        Optional<RepositoryMember> repositoryMember = repositoryMemberPort.findByRepositoryIdAndUserId(repo.getId(), uid);
        if (repositoryMember.isPresent()) {
            var role = repositoryMember.get().getRole();
            boolean writable = role == RepositoryMemberRole.WRITER
                    || role == RepositoryMemberRole.MAINTAINER;
            return new RepositoryPermission("REPOSITORY_" + role.name(), writable, true);
        }

        if (repo.getOwnerType() == OwnerType.ORGANIZATION && repo.getOwnerId() != null) {
            Optional<OrganizeMember> organizeMember = organizeMemberPort.findByOrganizeIdAndUserId(
                    OrganizeId.of(repo.getOwnerId().getValue()),
                    io.jgitkins.server.collaboration.domain.vo.MemberUserId.of(uid.getValue())
            );
            if (organizeMember.isPresent()) {
                var role = organizeMember.get().getRole();
                boolean writable = role == io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.OWNER
                        || role == io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.MAINTAINER;
                return new RepositoryPermission("ORGANIZATION_" + role.name(), writable, true);
            }
        }
        return RepositoryPermission.none();
    }

    private Optional<Repository> resolveRepository(OwnerType ownerType, String ownerName, String repositoryName) {
        if (ownerName == null || ownerName.isBlank()
            || repositoryName == null || repositoryName.isBlank()) {
            return Optional.empty();
        }
        if (ownerType == null) {
            return repositoryLookupService.resolveByPath(ownerName, repositoryName);
        }
        return repositoryLookupService.resolveByOwner(ownerType, ownerName, repositoryName);
    }
}
