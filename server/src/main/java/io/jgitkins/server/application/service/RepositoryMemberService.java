package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import io.jgitkins.server.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberRemoveUseCase;
import io.jgitkins.server.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.application.validate.RepositoryMemberValidator;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
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
    private final RepositoryMemberValidator repositoryMemberValidator;

    @Override
    @Transactional
    public void addRepositoryMember(RepositoryMemberAddCommand command) {
        repositoryMemberValidator.validateAddCommand(command);
        
        RepositoryId repositoryId = RepositoryId.of(command.getRepositoryId());
        UserId userId = UserId.of(command.getUserId());
        
        if (repositoryMemberValidator.isAlreadyMember(repositoryId, userId)) {
            return;
        }
        
        RepositoryMemberRole role = command.getRole() != null ? command.getRole() : RepositoryMemberRole.READER;
        RepositoryMember member = RepositoryMember.create(repositoryId, userId, role, null);
        repositoryMemberPort.save(member);
    }

    @Override
    @Transactional
    public void removeRepositoryMember(Long repositoryId, Long userId) {
        repositoryMemberValidator.validateMemberIdentifiers(repositoryId, userId);
        repositoryMemberPort.deleteByRepositoryIdAndUserId(RepositoryId.of(repositoryId), UserId.of(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryMemberSummary> getRepositoryMembers(Long repositoryId) {
        repositoryMemberValidator.validateRepositoryId(repositoryId);
        
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
