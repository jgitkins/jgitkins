package io.jgitkins.server.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;
import org.junit.jupiter.api.Test;

/**
 * The three identifiers whose application-layer guards tasks 2.95 and 2.96 remove must answer with a
 * typed domain exception, not a bare {@code IllegalArgumentException}.
 *
 * <p>Those guards exist to convert what would otherwise be a 500 into a meaningful error. Removing them
 * before the value objects carry a mapped exception would turn a working 4xx into a 500, which is why
 * this conversion is the floor 2.95 and 2.96 depend on, independent of what the boundary already
 * catches over HTTP.
 *
 * <p>Deliberately narrow. Every other identifier in the codebase still throws
 * {@code IllegalArgumentException} and still answers 500, because a value object broken by internal
 * code is a server bug and 500 is the honest answer. Converting all of them would report server bugs as
 * client errors.
 */
class IdentifierInvariantTest {

    @Test
    void ownerIdRejectsNonPositiveWithAMappedException() {
        assertThatThrownBy(() -> OrganizeOwnerId.of(null)).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> OrganizeOwnerId.of(0L)).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> OrganizeOwnerId.of(-1L)).isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void repositoryIdRejectsNonPositiveWithAMappedException() {
        assertThatThrownBy(() -> RepositoryId.of(null)).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> RepositoryId.of(0L)).isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void repositoryMemberUserIdRejectsNonPositiveWithAMappedException() {
        assertThatThrownBy(() -> RepositoryMemberUserId.of(null))
                .isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> RepositoryMemberUserId.of(0L))
                .isInstanceOf(InvalidIdentifierException.class);
    }
}
