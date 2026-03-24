package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.validate.OrganizeMemberValidator;
import io.jgitkins.server.domain.model.OrganizeMember;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.UserId;
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
        OrganizeId organizeId = OrganizeId.of(command.getOrganizeId());
        UserId userId = UserId.of(command.getUserId());

        OrganizeMember member = OrganizeMember.create(
                organizeId,
                userId,
                organizeMemberValidator.resolveRole(command.getRole()),
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
