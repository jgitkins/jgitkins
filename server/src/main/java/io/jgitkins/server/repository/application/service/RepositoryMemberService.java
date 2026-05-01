package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberRemoveUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipPolicy;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryMemberService implements RepositoryMemberAddUseCase,
                                                RepositoryMemberRemoveUseCase,
                                                RepositoryMemberQueryUseCase {

    private final RepositoryMemberPersistencePort repositoryMemberPort;
    private final RepositoryMembershipPolicy repositoryMembershipPolicy;

    @Override
    @Transactional
    public void addRepositoryMember(RepositoryMemberAddCommand command) {
        RepositoryMember member = repositoryMembershipPolicy.createMember(command);
        if (repositoryMembershipPolicy.isAlreadyMember(member.getRepositoryId(), member.getUserId())) {
            return;
        }
        repositoryMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeRepositoryMember(Long repositoryId, Long userId) {
        repositoryMembershipPolicy.validateMemberIdentifiers(repositoryId, userId);
        repositoryMemberPort.deleteByRepositoryIdAndUserId(
                RepositoryId.of(repositoryId),
                io.jgitkins.server.domain.model.vo.UserId.of(userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId) {
        repositoryMembershipPolicy.validateRepositoryId(repositoryId);
        return repositoryMemberPort.findAllByRepositoryId(RepositoryId.of(repositoryId))
                .stream()
                .map(member -> new RepositoryMemberSummary(
                        member.getUserId().getValue(),
                        member.getRole(),
                        member.getAddedAt()
                ))
                .toList();
    }
}
