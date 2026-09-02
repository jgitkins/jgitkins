package io.jgitkins.server.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import org.junit.jupiter.api.Test;

/**
 * An identifier built from a value the client supplied must answer with a typed domain exception, not
 * a bare {@code IllegalArgumentException}. One built from a value read out of storage must not.
 *
 * <p>The conversion exists because tasks 2.95 and 2.96 removed the application-layer guards that used
 * to turn these into a meaningful error; without a mapped exception on the value object those became
 * 500s. That is the floor those tasks depend on, independent of what the boundary catches over HTTP.
 *
 * <p><strong>The rule is provenance, not which guard was deleted.</strong> This used to be stated as
 * "the three identifiers 2.95 and 2.96 touched, and every other identifier stays a 500". Those three
 * are all request-derived, so that set is a subset of this one and this is a narrowing, not a
 * reversal. The principle is unchanged and is the reason the second half of the rule still holds: a
 * value object broken by internal code is a server bug, and 500 is the honest answer. Converting all
 * of them would report server bugs as client errors -- and file data corruption under a status code
 * no alert is watching.
 */
class IdentifierInvariantTest {

    @Test
    void organizeOwnerIdRejectsNonPositiveWithAMappedException() {
        assertThatThrownBy(() -> OrganizeOwnerId.of(null)).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> OrganizeOwnerId.of(0L)).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> OrganizeOwnerId.of(-1L)).isInstanceOf(InvalidIdentifierException.class);
    }

    /**
     * Untested until now. This test class sits in {@code shared.domain} and is named for the shared
     * kernel, but every assertion in it named collaboration's owner id -- so the shared type that
     * eleven repository files depend on had no rejection coverage while a test that looked like its
     * coverage existed.
     */
    @Test
    void repositoryOwnerIdFromARequestRejectsNonPositiveWithAMappedException() {
        assertThatThrownBy(() -> RepositoryOwnerId.of(null))
                .isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> RepositoryOwnerId.of(0L))
                .isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> RepositoryOwnerId.of(-1L))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    /**
     * The other half of the rule, and the one that is easy to lose. A stored owner id of zero is a
     * corrupt row, not a bad request: answering 400 would blame a client whose request was fine and
     * would count server-side data corruption as a client error, where nothing is alerting.
     */
    @Test
    void repositoryOwnerIdFromStorageStaysAnUnmappedServerError() {
        assertThatThrownBy(() -> RepositoryOwnerId.fromStoredValue(0L, "REPOSITORY id=7"))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidIdentifierException.class)
                .hasMessageContaining("REPOSITORY id=7")
                .hasMessageContaining("OWNER_ID=0");

        assertThatThrownBy(() -> RepositoryOwnerId.fromStoredValue(-1L, "REPOSITORY id=9"))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(InvalidIdentifierException.class);
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
