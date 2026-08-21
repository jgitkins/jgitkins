package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.exception.InvalidNamespaceException;
import io.jgitkins.server.repository.application.exception.InvalidOwnerContextException;
import io.jgitkins.server.repository.application.exception.MemberIdentifierRequiredException;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryAlreadyExistsException;
import io.jgitkins.server.repository.application.exception.RepositoryNotInitializedException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.*;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryValidator {

    private final RepositoryRepository repositoryRepository;
    private final OrganizationMembershipPort organizationMembershipPort;
    private final RepositoryActorPort repositoryActorPort;

    public void validateCreation(OwnerType ownerType, Long organizeId, RepositoryName repositoryName) {
        validateOwnership(ownerType, organizeId);
        OwnerId ownerId = resolveOwnerId(ownerType, organizeId);
        validateRepositoryNameUnique(ownerType, ownerId, repositoryName);
    }

    public void validateRepositoryNameUnique(OwnerType ownerType, OwnerId ownerId, RepositoryName name) {
        repositoryRepository.findByOwnerAndName(ownerType, ownerId, name)
                .ifPresent(existing -> {
                    throw new RepositoryAlreadyExistsException(
                            "Repository name already exists for owner: " + name.getValue());
                });
    }

    public void validateOwnership(OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.USER) {
            if (organizeId != null) {
                throw new InvalidOwnerContextException(
                        "organizeId must be null when ownerType is USER.");
            }
            requireCurrentUserId();
            return;
        }

        if (organizeId == null) {
            throw new InvalidOwnerContextException(
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
            throw new RepositoryAccessDeniedException(
                    "Cannot delete another user's repository");
        }
    }

    public Long requireCurrentUserId() {
        // 인증 실패는 ApplicationException으로 처리 - presentation 계층(Spring Security)에서 이미
        // 필터링하지만
        // 서비스 내부에서 currentUserId 조회 실패는 application 정책 위반으로 간주
        return repositoryActorPort.resolveCurrentUserId()
                .orElseThrow(UnauthenticatedException::new);
    }

    private void assertOrganizeMembership(Long organizeId) {
        Long requesterId = requireCurrentUserId();
        boolean isMember = organizationMembershipPort
                .findRoleByOrganizationIdAndUserId(organizeId, requesterId)
                .isPresent();
        if (!isMember) {
            throw new RepositoryAccessDeniedException(
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
