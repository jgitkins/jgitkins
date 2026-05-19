package io.jgitkins.server.collaboration.application.service;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.validate.OrganizeMemberValidator;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.identity.access.domain.vo.UserId;
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
    private final OrganizeMemberValidator organizeMemberValidator;

    @Override
    @Transactional
    public void addOrganizeMember(OrganizeMemberAddCommand command) {
        OrganizeId organizeId = OrganizeId.of(command.organizeId());
        UserId userId = UserId.of(command.userId());

        OrganizeMember member = OrganizeMember.create(
                organizeId,
                userId,
                organizeMemberValidator.resolveRole(command.role()),
                null
        );

        organizeMemberValidator.validateMemberNotExists(member.getOrganizeId(), member.getUserId());
        organizeMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeOrganizeMember(Long organizeId, Long userId) {
        organizeMemberPort.deleteByOrganizeIdAndUserId(OrganizeId.of(organizeId), UserId.of(userId));
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
