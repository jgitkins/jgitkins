package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryValidator {

    private final RepositoryPersistencePort repositoryPort;
    private final OrganizeMemberPersistencePort organizeMemberPort;
    private final CurrentUserPort currentUserPersistencePort;

    public void validateCreation(OwnerType ownerType, Long organizeId, RepositoryName repositoryName) {
        validateOwnership(ownerType, organizeId);
        OwnerId ownerId = resolveOwnerId(ownerType, organizeId);
        validateRepositoryNameUnique(ownerType, ownerId, repositoryName);
    }

    public void validateRepositoryNameUnique(OwnerType ownerType, OwnerId ownerId, RepositoryName name) {
        repositoryPort.findByOwnerAndName(ownerType, ownerId, name)
                .ifPresent(existing -> {
                    throw new ApplicationException(ApplicationErrorCode.REPOSITORY_ALREADY_EXISTS,
                            "Repository name already exists for owner: " + name.getValue());
                });
    }

    public void validateOwnership(OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.USER) {
            if (organizeId != null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID_OWNER_CONTEXT,
                        "organizeId must be null when ownerType is USER.");
            }
            requireCurrentUserId();
            return;
        }

        if (organizeId == null) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_OWNER_CONTEXT,
                    "organizeId is required when ownerType is ORGANIZATION.");
        }
        assertOrganizeMembership(organizeId);
    }

    public void enforceDeletionPermission(Repository repository) {
        if (repository.getOwnerType() != OwnerType.USER
                || repository.getOwnerId() == null
                || repository.getOwnerId().getValue() == null) {
            return;
        }
        Long requesterId = requireCurrentUserId();
        if (!repository.getOwnerId().getValue().equals(requesterId)) {
            throw new ApplicationException(ApplicationErrorCode.REPOSITORY_ACCESS_DENIED,
                    "Cannot delete another user's repository");
        }
    }

    public Long requireCurrentUserId() {
        // 인증 실패는 ApplicationException으로 처리 - presentation 계층(Spring Security)에서 이미
        // 필터링하지만
        // 서비스 내부에서 currentUserId 조회 실패는 application 정책 위반으로 간주
        return currentUserPersistencePort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.ACCESS_DENIED, "Unauthenticated"));
    }

    private void assertOrganizeMembership(Long organizeId) {
        Long requesterId = requireCurrentUserId();
        boolean isMember = organizeMemberPort.existsByOrganizeIdAndUserId(OrganizeId.of(organizeId),
                UserId.of(requesterId));
        if (!isMember) {
            throw new ApplicationException(ApplicationErrorCode.ORGANIZE_ACCESS_DENIED,
                    "User is not a member of the organization.");
        }
    }

    private OwnerId resolveOwnerId(OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return OwnerId.of(organizeId);
        }
        return OwnerId.of(requireCurrentUserId());
    }
}
