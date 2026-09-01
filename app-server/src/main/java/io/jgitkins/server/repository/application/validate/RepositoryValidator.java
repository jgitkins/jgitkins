package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.exception.InvalidNamespaceException;
import io.jgitkins.server.repository.application.exception.InvalidOwnerContextException;
import io.jgitkins.server.repository.application.exception.MemberIdentifierRequiredException;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryAlreadyExistsException;
import io.jgitkins.server.repository.application.exception.RepositoryNotInitializedException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
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

    public void validateCreation(Long requesterUserId, OwnerType ownerType, Long organizeId,
                                 RepositoryName repositoryName) {
        validateOwnership(requesterUserId, ownerType, organizeId);
        RepositoryOwnerId ownerId = resolveOwnerId(requesterUserId, ownerType, organizeId);
        validateRepositoryNameUnique(ownerType, ownerId, repositoryName);
    }

    public void validateRepositoryNameUnique(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryName name) {
        repositoryRepository.findByOwnerAndName(ownerType, ownerId, name)
                .ifPresent(existing -> {
                    throw new RepositoryAlreadyExistsException(
                            "Repository name already exists for owner: " + name.getValue());
                });
    }

    public void validateOwnership(Long requesterUserId, OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.USER) {
            if (organizeId != null) {
                throw new InvalidOwnerContextException(
                        "organizeId must be null when ownerType is USER.");
            }
            requireRequesterId(requesterUserId);
            return;
        }

        if (organizeId == null) {
            throw new InvalidOwnerContextException(
                    "organizeId is required when ownerType is ORGANIZATION.");
        }
        assertOrganizeMembership(requesterUserId, organizeId);
    }

    /**
     * Kept as a method rather than inlined so the failure has exactly one definition.
     *
     * <p>Task 2.64 replaced its body. It used to call {@code RepositoryActorPort} and throw when the port
     * returned empty; it now validates the value it was handed. The exception type is unchanged, because
     * it is what produces the existing 401 envelope.
     */
    public Long requireRequesterId(Long requesterUserId) {
        // 인증 실패는 ApplicationException으로 처리 - presentation 계층(Spring Security)에서 이미
        // 필터링하지만
        // 서비스 내부에서 currentUserId 조회 실패는 application 정책 위반으로 간주
        if (requesterUserId == null) {
            throw new UnauthenticatedException();
        }
        return requesterUserId;
    }

    private void assertOrganizeMembership(Long requesterUserId, Long organizeId) {
        Long requesterId = requireRequesterId(requesterUserId);
        boolean isMember = organizationMembershipPort
                .findRoleByOrganizationIdAndUserId(organizeId, requesterId)
                .isPresent();
        if (!isMember) {
            throw new RepositoryAccessDeniedException(
                    "User is not a member of the organization.");
        }
    }

    private RepositoryOwnerId resolveOwnerId(Long requesterUserId, OwnerType ownerType, Long organizeId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return RepositoryOwnerId.of(organizeId);
        }
        return RepositoryOwnerId.of(requireRequesterId(requesterUserId));
    }
}
