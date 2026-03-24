package io.jgitkins.server.domain.model;

import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RepositoryMember {

    private final RepositoryId repositoryId;
    private final UserId userId;
    private final RepositoryMemberRole role;
    private final LocalDateTime addedAt;

    public static RepositoryMember create(RepositoryId repositoryId,
                                          UserId userId,
                                          RepositoryMemberRole role,
                                          LocalDateTime addedAt) {
        if (repositoryId == null || userId == null || role == null) {
            throw new IllegalArgumentException("RepositoryMember requires repositoryId, userId and role");
        }
        return new RepositoryMember(repositoryId,
                                    userId,
                                    role,
                                    addedAt != null ? addedAt : LocalDateTime.now());
    }
}
