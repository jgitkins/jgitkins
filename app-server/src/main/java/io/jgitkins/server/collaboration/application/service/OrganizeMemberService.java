package io.jgitkins.server.collaboration.application.service;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.exception.OrganizeMemberNotFoundException;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizeMemberService implements OrganizeMemberAddUseCase,
                                               OrganizeMemberRemoveUseCase,
                                               OrganizeMemberQueryUseCase {

    private final OrganizeMemberPersistencePort organizeMemberPort;
    private final OrganizeMembershipQueryPort organizeMembershipQueryPort;
    private final OrganizeRepository organizeRepository;

    @Override
    @Transactional
    public void addOrganizeMember(OrganizeMemberAddCommand command) {
        if (command.requesterUserId() == null) {
            throw new OrganizeAccessDeniedException("Authentication required");
        }
        OrganizeId organizeId = OrganizeId.of(command.organizeId());
        if (organizeRepository.findById(organizeId).isEmpty()) {
            throw new io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException(command.organizeId());
        }
        organizeRepository.lockByIdForMembershipMutation(organizeId);
        if (organizeMembershipQueryPort.countOwnersByOrganizeId(command.organizeId()) == 0) {
            throw new OrganizeAccessDeniedException("Organization membership migration is required");
        }
        if (organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(
                command.organizeId(), command.requesterUserId())
                .filter(role -> role == io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.OWNER)
                .isEmpty()) {
            throw new OrganizeAccessDeniedException("Only an organization owner can add members");
        }
        MemberUserId userId = MemberUserId.of(command.userId());
        if (organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(command.organizeId(), command.userId()).isPresent()) {
            throw new io.jgitkins.server.collaboration.application.exception.OrganizeMemberAlreadyExistsException(
                    command.organizeId(), command.userId());
        }
        OrganizeMember member = OrganizeMember.create(
                organizeId,
                userId,
                command.role() != null ? command.role() : io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.MEMBER,
                null);
        organizeMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeOrganizeMember(Long organizeId, Long requesterUserId, Long targetUserId) {
        // Narrowed to the requester. The other two are identifiers, and OrganizeId.of and
        // MemberUserId.of below reject null and non-positive with a mapped domain exception, so
        // checking them here answered 403 for what is a malformed identifier rather than a permission
        // problem. The requester check stays: it is an authorization answer, not a value check.
        if (requesterUserId == null) {
            throw new OrganizeAccessDeniedException("Authentication is required");
        }
        OrganizeId id = OrganizeId.of(organizeId);
        organizeRepository.lockByIdForMembershipMutation(id);
        long ownerCount = organizeMembershipQueryPort.countOwnersByOrganizeId(organizeId);
        if (ownerCount == 0) {
            throw new OrganizeAccessDeniedException("Organization membership migration is required");
        }
        io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole requesterRole =
                organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(organizeId, requesterUserId)
                        .orElseThrow(() -> new OrganizeAccessDeniedException("Requester is not an organization member"));
        boolean selfRemoval = requesterUserId.equals(targetUserId);
        if (!selfRemoval && requesterRole != io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.OWNER) {
            throw new OrganizeAccessDeniedException("Only an organization owner can remove another member");
        }
        OrganizeMember target = organizeMemberPort.findByOrganizeIdAndUserId(id, MemberUserId.of(targetUserId))
                .orElseThrow(() -> new OrganizeMemberNotFoundException(organizeId, targetUserId));
        if (target.getRole() == io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.OWNER && ownerCount <= 1) {
            throw new OrganizeAccessDeniedException("Organization must retain an active owner");
        }
        organizeMemberPort.deleteByOrganizeIdAndUserId(id, MemberUserId.of(targetUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizeMemberSummary> getOrganizeMembers(Long organizeId) {
        return organizeMemberPort.findAllByOrganizeId(OrganizeId.of(organizeId))
                .stream()
                .map(member -> new OrganizeMemberSummary(
                        member.getUserId().getValue(),
                        member.getRole(),
                        member.getJoinedAt()
                ))
                .toList();
    }
}
