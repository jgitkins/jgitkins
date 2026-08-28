package io.jgitkins.server.collaboration.application.service;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.collaboration.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;

import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
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

    @Override
    @Transactional
    public OrganizeCreationResult createOrganize(OrganizeCreationCommand command) {
        // 1. 입력 정합성 검증 (Domain VO 생성)
        OrganizeName name = OrganizeName.from(command.name());
        // No null guard on the requester here. OwnerId.of below rejects null and non-positive with a
        // mapped domain exception, and the "authenticated user required" answer belongs to the adapter,
        // which OrganizeController gives as a 401. Keeping a copy here answered 403 for the same
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
                io.jgitkins.server.collaboration.domain.vo.MemberUserId.of(ownerId),
                OrganizeMemberRole.OWNER,
                LocalDateTime.now()));
        saved.recordCreated(Instant.now());
        List<io.jgitkins.server.shared.domain.event.DomainEvent> events = List.copyOf(saved.getDomainEvents());
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
    public void deleteOrganize(Long organizeId) {
        organizeValidator.findByIdOrThrow(organizeId);
        organizeRepository.deleteById(OrganizeId.of(organizeId));
    }
}
