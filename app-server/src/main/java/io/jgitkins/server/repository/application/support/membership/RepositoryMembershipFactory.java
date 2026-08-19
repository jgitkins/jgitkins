package io.jgitkins.server.repository.application.support.membership;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMembershipFactory {

    public RepositoryMember createMember(RepositoryMemberAddCommand command) {
        RepositoryId repositoryId = RepositoryId.of(command.repositoryId());
        RepositoryMemberUserId userId = RepositoryMemberUserId.of(command.userId());
        RepositoryMemberRole role = command.role() != null ? command.role() : RepositoryMemberRole.READER;
        return RepositoryMember.create(repositoryId, userId, role, null);
    }
}
