package io.jgitkins.server.repository.application.validate;

import static org.mockito.Mockito.verify;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryAccessValidatorTest {


    @Mock
    private GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    /**
     * The read model, not the aggregate.
     *
     * <p>Task 2.65 changed validateReadAccess to take the {@code RepositoryResult} the route already
     * loaded. A concrete record rather than a mock, because the point of the change is that the
     * validator authorizes the exact object being returned.
     */
    private final RepositoryResult repository = new RepositoryResult(
            1L, "USER", "repo", "org/repo", "main", "PRIVATE",
            null, 1L, null, "/org/repo.git", null, false, null, null, null);

    private RepositoryAccessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RepositoryAccessValidator(gitRepositoryAccessUseCase);
    }

    @Test
    void validateReadAccess_usesExplicitRequester() {
        when(gitRepositoryAccessUseCase.resolvePermission(repository, 42L))
                .thenReturn(new RepositoryPermission("REPOSITORY_READER", false, true));

        validator.validateReadAccess(repository, 42L);

        // The requester the caller supplied reaches the permission resolver unchanged. Before task 2.65
        // this method read RepositoryActorPort, so it authorized against the security context regardless
        // of which repository the route had loaded for whom.
        verify(gitRepositoryAccessUseCase).resolvePermission(repository, 42L);
    }

    /**
     * The regression. This denied 403 before the fix: decide()'s public short-circuit is
     * `isPublic && userId == null`, so an authenticated non-member fell through to none() and
     * member() was false. The same GET succeeded while logged out and failed once logged in.
     *
     * <p>allowsAnonymousReadOfAPublicRepository below did not catch it because the anonymous caller
     * is precisely the one case the short-circuit does cover.
     */
    @Test
    void validateReadAccess_allowsAnAuthenticatedNonMemberToReadAPublicRepository() {
        RepositoryResult publicRepository = new RepositoryResult(
                1L, "USER", "repo", "org/repo", "main", "PUBLIC",
                null, 1L, null, "/org/repo.git", null, false, null, null, null);
        when(gitRepositoryAccessUseCase.resolvePermission(publicRepository, 99L))
                .thenReturn(RepositoryPermission.none());

        assertDoesNotThrow(() -> validator.validateReadAccess(publicRepository, 99L));
    }

    @Test
    void validateReadAccess_stillDeniesANonMemberOnAPrivateRepository() {
        when(gitRepositoryAccessUseCase.resolvePermission(repository, 99L))
                .thenReturn(RepositoryPermission.none());

        // The fix must not open private repositories: visibleOn(false) is still member().
        assertThrows(JgitkinsException.class, () -> validator.validateReadAccess(repository, 99L));
    }

    @Test
    void validateReadAccess_allowsAnonymousReadOfAPublicRepository() {
        RepositoryResult publicRepository = new RepositoryResult(
                2L, "USER", "open", "org/open", "main", "PUBLIC",
                null, 1L, null, "/org/open.git", null, false, null, null, null);
        when(gitRepositoryAccessUseCase.resolvePermission(publicRepository, null))
                .thenReturn(new RepositoryPermission("PUBLIC_READ_ONLY", false, true));

        // Null is a value here, not a rejection. Anonymous public reads are existing behaviour, and a
        // validator that demanded a requester would break every one of them.
        assertDoesNotThrow(() -> validator.validateReadAccess(publicRepository, null));
    }

    @Test
    void validateReadAccess_throwsForbidden_whenReadPermissionDenied() {
        when(gitRepositoryAccessUseCase.resolvePermission(repository, 7L))
                .thenReturn(RepositoryPermission.none());

        JgitkinsException ex = assertThrows(JgitkinsException.class,
                () -> validator.validateReadAccess(repository, 7L));

        assertEquals(ApplicationErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void validateReadAccess_allows_whenReadPermissionGranted() {
        when(gitRepositoryAccessUseCase.resolvePermission(repository, null))
                .thenReturn(new RepositoryPermission("PUBLIC_READ_ONLY", false, true));

        assertDoesNotThrow(() -> validator.validateReadAccess(repository, null));
    }

    @Test
    void validateCanCommit_throwsUnauthorized_whenRequesterMissing() {
        // Task 2.64: the requester is an argument, so "missing" is a null argument rather than an empty
        // Optional from a port. The error code must not have changed with it -- that is what keeps the
        // existing 401 envelope.
        JgitkinsException ex = assertThrows(JgitkinsException.class,
                () -> validator.validateCanCommit("team", "repo", null));

        assertEquals(ApplicationErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void validateCanCommit_throwsForbidden_whenWritePermissionDenied() {
        when(gitRepositoryAccessUseCase.canWrite(null, "team", "repo", 7L)).thenReturn(false);

        JgitkinsException ex = assertThrows(JgitkinsException.class,
                () -> validator.validateCanCommit("team", "repo", 7L));

        assertEquals(ApplicationErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void validateCanCommit_allows_whenWritePermissionGranted() {
        when(gitRepositoryAccessUseCase.canWrite(null, "team", "repo", 7L)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validateCanCommit("team", "repo", 7L));
    }
}
