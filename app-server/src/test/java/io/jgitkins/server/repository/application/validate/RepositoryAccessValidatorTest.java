package io.jgitkins.server.repository.application.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryAccessValidatorTest {

    @Mock
    private RepositoryActorPort currentUserPersistencePort;

    @Mock
    private GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    @Mock
    private Repository repository;

    private RepositoryAccessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RepositoryAccessValidator(currentUserPersistencePort, gitRepositoryAccessUseCase);
    }

    @Test
    void validateReadAccess_throwsForbidden_whenReadPermissionDenied() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(gitRepositoryAccessUseCase.resolvePermission(repository, 7L))
                .thenReturn(RepositoryPermission.none());

        JgitkinsException ex = assertThrows(JgitkinsException.class,
                () -> validator.validateReadAccess(repository));

        assertEquals(ApplicationErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void validateReadAccess_allows_whenReadPermissionGranted() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.empty());
        when(gitRepositoryAccessUseCase.resolvePermission(repository, null))
                .thenReturn(new RepositoryPermission("PUBLIC_READ_ONLY", false, true));

        assertDoesNotThrow(() -> validator.validateReadAccess(repository));
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
