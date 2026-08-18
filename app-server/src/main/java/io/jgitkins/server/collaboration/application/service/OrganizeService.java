package io.jgitkins.server.collaboration.application.service;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.collaboration.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.collaboration.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.collaboration.application.port.out.UserIdentityPort;
import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.identity.access.domain.vo.UserId;
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

    private final OrganizePersistencePort organizePort;
    private final UserIdentityPort userIdentityPort;
    private final DomainEventPublisher domainEventPublisher;
    private final OrganizeValidator organizeValidator;
    private final OrganizeApplicationMapper organizeApplicationMapper;

    @Override
    @Transactional
    public OrganizeCreationResult createOrganize(OrganizeCreationCommand command) {
        // 1. 입력 정합성 검증 (Domain VO 생성)
        OrganizeName name = OrganizeName.from(command.name());
        UserId ownerId = userIdentityPort.resolveCurrentActiveUserId()
                .map(UserId::of)
                .orElseThrow(() -> new OrganizeAccessDeniedException("An authenticated user is required"));

        // 2. 데이터 정합성 검증
        organizeValidator.validateCreation(name);

        // 3. 비즈니스 로직 수행 (Aggregate 생성 및 저장)
        Organize organize = Organize.createWithoutEvent(
                null,
                name,
                ownerId == null ? null : OwnerId.of(ownerId.getValue()),
                command.description(),
                LocalDateTime.now());

        Organize saved = organizePort.save(organize);
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
        return organizePort.findAll().stream()
                .map(organizeApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizeCreationResult> getAccessibleOrganizes() {
        return userIdentityPort.resolveCurrentActiveUserId()
                .map(id -> {
                    UserId userId = UserId.of(id);
                    return organizePort.findAll().stream()
                            .filter(org -> organizeValidator.isAccessible(org, userId))
                            .map(organizeApplicationMapper::toDto)
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void deleteOrganize(Long organizeId) {
        organizeValidator.findByIdOrThrow(organizeId);
        organizePort.deleteById(OrganizeId.of(organizeId));
    }
}
