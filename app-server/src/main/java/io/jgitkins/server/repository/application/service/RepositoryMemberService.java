package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.policy.RepositoryMemberManagementPolicy;
import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.repository.application.validate.RepositoryMemberValidator;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryMemberService implements RepositoryMemberManagementUseCase,
                                                RepositoryMemberLoadUseCase {

    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final RepositoryMemberValidator repositoryMemberValidator;
    private final RepositoryMembershipFactory repositoryMembershipFactory;
    private final RepositoryMemberManagementPolicy repositoryMemberManagementPolicy;

    @Override
    @Transactional
    public void addRepositoryMember(RepositoryMemberAddCommand command) {
        repositoryMemberValidator.validateAddCommand(command);
        // Authorization before any member read or write. The early return below makes an existing
        // membership a silent no-op, so authorizing afterwards would let a non-owner probe
        // membership by observing which requests are quietly accepted.
        repositoryMemberManagementPolicy.validateCanManageMembers(
                command.requesterUserId(), command.repositoryId());
        RepositoryMember member = repositoryMembershipFactory.createMember(command);
        if (repositoryMemberValidator.isAlreadyMember(member.getRepositoryId(), member.getUserId())) return;
        repositoryMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeRepositoryMember(Long requesterUserId, Long repositoryId, Long userId) {
        repositoryMemberValidator.validateMemberIdentifiers(repositoryId, userId);
        repositoryMemberManagementPolicy.validateCanManageMembers(requesterUserId, repositoryId);
        repositoryMemberPort.deleteByRepositoryIdAndUserId(
                RepositoryId.of(repositoryId), RepositoryMemberUserId.of(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId) {
        repositoryMemberValidator.validateRepositoryId(repositoryId);
        return repositoryMemberPort.findAllByRepositoryId(RepositoryId.of(repositoryId)).stream()
                .map(member -> new RepositoryMemberSummary(member.getUserId().getValue(), member.getRole(), member.getAddedAt()))
                .toList();
    }
}
