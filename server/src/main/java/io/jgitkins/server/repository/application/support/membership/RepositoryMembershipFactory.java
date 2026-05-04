package io.jgitkins.server.repository.application.support.membership;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMembershipFactory {

    public RepositoryMember createMember(RepositoryMemberAddCommand command) {
        RepositoryId repositoryId = RepositoryId.of(command.repositoryId());
        UserId userId = UserId.of(command.userId());
        RepositoryMemberRole role = command.role() != null ? command.role() : RepositoryMemberRole.READER;
        return RepositoryMember.create(repositoryId, userId, role, null);
    }
}
