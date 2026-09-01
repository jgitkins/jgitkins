package io.jgitkins.server.collaboration.application.service;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.collaboration.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.exception.OrganizeHasRepositoriesException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeOwnedRepositoryCountPort;

import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizeService implements OrganizeCreationUseCase,
                                        OrganizeLoadUseCase,
                                        OrganizeDeletionUseCase {

    private final OrganizeRepository organizeRepository;
    private final OrganizeMemberPersistencePort organizeMemberPersistencePort;

    private final DomainEventPublisher domainEventPublisher;
    private final OrganizeValidator organizeValidator;
    private final OrganizeApplicationMapper organizeApplicationMapper;
    private final OrganizeMembershipQueryPort organizeMembershipQueryPort;
    private final OrganizeOwnedRepositoryCountPort organizeOwnedRepositoryCountPort;

    @Override
    @Transactional
    public OrganizeCreationResult createOrganize(OrganizeCreationCommand command) {
        // 1. 입력 정합성 검증 (Domain VO 생성)
        OrganizeName name = OrganizeName.from(command.name());
        // No null guard on the requester here. OwnerId.of below rejects null and non-positive with a
        // mapped domain exception, and the "authenticated user required" answer belongs to the adapter,
        // which OrganizeManagementController gives as a 401. Keeping a copy here answered 403 for the same
        // condition and could only disagree with it.
        Long ownerId = command.requesterUserId();

        // 2. 데이터 정합성 검증
        organizeValidator.validateCreation(name);

        // 3. 비즈니스 로직 수행 (Aggregate 생성 및 저장)
        Organize organize = Organize.createWithoutEvent(
                null,
                name,
                OwnerId.of(ownerId),
                command.description(),
                LocalDateTime.now());

        Organize saved = organizeRepository.save(organize);
        organizeMemberPersistencePort.save(OrganizeMember.create(
                saved.getId(),
                MemberUserId.of(ownerId),
                OrganizeMemberRole.OWNER,
                LocalDateTime.now()));
        saved.recordCreated(Instant.now());
        List<DomainEvent> events = List.copyOf(saved.getDomainEvents());
        domainEventPublisher.publish(events);
        saved.clearDomainEvents();
        return organizeApplicationMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizeCreationResult getOrganize(Long organizeId) {
        return organizeApplicationMapper.toDto(organizeValidator.findByIdOrThrow(organizeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizeCreationResult> getOrganizes() {
        return organizeRepository.findAll().stream()
                .map(organizeApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizeCreationResult> getAccessibleOrganizes(Long requesterUserId) {
        if (requesterUserId == null) {
            return List.of();
        }
        return organizeRepository.findAll().stream()
                .filter(org -> organizeValidator.isAccessible(org, requesterUserId))
                .map(organizeApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    /**
     * @throws OrganizeAccessDeniedException when the requester is absent or is not an organization
     *     OWNER. 403 rather than 404: {@code GET /api/organizes} lists every organization without
     *     authentication, so the existence of one is not a secret worth hiding behind a not-found.
     * @throws OrganizeHasRepositoriesException when the organization still owns repositories
     */
    public void deleteOrganize(Long requesterUserId, Long organizeId) {
        organizeValidator.findByIdOrThrow(organizeId);

        // Before this the method took no requester at all, so no layer could authorize: the
        // controller passed no principal, SecurityConfig is permitAll, and there is no method
        // security. Anyone could delete any organization.
        requireOwner(requesterUserId, organizeId);

        // Refused rather than cascaded. The schema declares no foreign keys, so deleting the
        // ORGANIZE row would leave every repository it owns pointing at an id that no longer
        // resolves, with no way to re-attach them. See OrganizeHasRepositoriesException and the
        // delayed-deletion direction in TODOS.md, which is what removes this guard.
        long ownedRepositories = organizeOwnedRepositoryCountPort.countByOrganizeId(organizeId);
        if (ownedRepositories > 0) {
            throw new OrganizeHasRepositoriesException(ownedRepositories);
        }

        organizeRepository.deleteById(OrganizeId.of(organizeId));
    }

    /**
     * OWNER only, matching {@code OrganizeMemberService} for add and remove. Deleting the
     * organization is a strictly larger authority than administering its membership, so it cannot
     * be granted to a role that membership administration withholds.
     *
     * <p>Third copy of this check in this context ({@code OrganizeMemberService:47} and {@code :84}
     * are the others). Not extracted yet: the three sit in different services with different
     * surrounding flows, and a shared helper now would be an abstraction over a coincidence.
     * Extract on the fourth.
     */
    private void requireOwner(Long requesterUserId, Long organizeId) {
        if (requesterUserId == null) {
            throw new OrganizeAccessDeniedException("Authentication is required");
        }
        if (organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(organizeId, requesterUserId)
                .filter(role -> role == OrganizeMemberRole.OWNER)
                .isEmpty()) {
            throw new OrganizeAccessDeniedException("Only an organization owner can delete it");
        }
    }
}
