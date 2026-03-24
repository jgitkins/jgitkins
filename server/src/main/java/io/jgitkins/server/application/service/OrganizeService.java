package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.validate.OrganizeValidator;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
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
    private final CurrentUserPort currentUserPersistencePort;
    private final OrganizeValidator organizeValidator;
    private final OrganizeApplicationMapper organizeApplicationMapper;

    @Override
    @Transactional
    public OrganizeCreationResult createOrganize(OrganizeCreationCommand command) {
        // 1. 입력 정합성 검증 (Domain VO 생성)
        OrganizeName name = OrganizeName.from(command.getName());
        UserId ownerId = command.getOwnerId() != null ? UserId.of(command.getOwnerId()) : null;

        // 2. 데이터 정합성 검증
        organizeValidator.validateCreation(name);

        // 3. 비즈니스 로직 수행 (Aggregate 생성 및 저장)
        Organize organize = Organize.create(name, ownerId, command.getDescription());

        return organizeApplicationMapper.toDto(organizePort.save(organize));
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
        return currentUserPersistencePort.resolveCurrentUserId()
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
