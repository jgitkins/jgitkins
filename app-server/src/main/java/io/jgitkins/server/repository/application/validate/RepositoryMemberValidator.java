package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.contract.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.exception.MemberIdentifierRequiredException;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Membership queries for the repository member flow.
 *
 * <p>Three identifier-checking methods used to live here and are gone. The TODO they carried said form
 * validation belonged in the presentation layer, and that is now where it is: the request DTO carries
 * {@code @NotNull @Positive} on userId and every path-variable id carries {@code @Positive}, so a null
 * or non-positive identifier no longer reaches this layer over HTTP at all. Behind them,
 * {@code RepositoryId.of} and {@code RepositoryMemberUserId.of} reject the same values with a mapped
 * domain exception, and the policy answers a null repository id as not-found.
 */
@Component
@RequiredArgsConstructor
public class RepositoryMemberValidator {

    private final RepositoryMemberPersistencePort repositoryMemberPort;

    /**
     * The one check that was not a duplicate. No value object guards whether the command object itself
     * is null, so deleting this with the field checks turned a typed 422 into a NullPointerException
     * and a 500. The field checks are gone because {@code RepositoryId.of} and
     * {@code RepositoryMemberUserId.of} reject the same values; this one has nothing behind it.
     */
    public void validateAddCommand(RepositoryMemberAddCommand command) {
        if (command == null) {
            throw new MemberIdentifierRequiredException("A repository member command is required");
        }
    }

    public boolean isAlreadyMember(RepositoryId repositoryId, RepositoryMemberUserId userId) {
        return repositoryMemberPort.existsByRepositoryIdAndUserId(repositoryId, userId);
    }
}
