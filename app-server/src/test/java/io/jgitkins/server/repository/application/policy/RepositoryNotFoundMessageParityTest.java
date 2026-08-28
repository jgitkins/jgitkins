package io.jgitkins.server.repository.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Task 2.112. The two outcomes behind a 404 must be indistinguishable in the RESPONSE BODY, not just
 * in the status and the error code.
 *
 * <p>Task 2.92 made every denial answer 404 REPO-404, and the class javadoc of
 * {@link RepositoryMemberManagementPolicy} says the two cases are "deliberately indistinguishable".
 * They were not. {@code RepositoryNotFoundException} has an id-carrying constructor, and the missing
 * -repository branches used it while the denial branch used the no-argument one, so a missing
 * repository read "Repository not found: 12" and a refused one read "Repository not found". Same
 * status, same code, different body. {@code $.error.message} is served to clients.
 *
 * <p>These tests assert EQUALITY of the two messages rather than the absence of an id. Equality is
 * what closes the oracle; dropping the id is only one way to get there, and asserting the mechanism
 * instead of the property would let the next refactor satisfy the test while reopening the leak.
 *
 * <p>Deliberately NOT changed: the pull-request paths still echo the namespace and repository name
 * the caller typed into the URL ({@code PullRequestControllerTest}, and the two service tests). Those
 * carry no authorization decision, so there are no two outcomes to tell apart.
 */
class RepositoryNotFoundMessageParityTest {

    private static final long REPOSITORY_ID = 12L;
    private static final long STRANGER_ID = 99L;

    // --- member management ---------------------------------------------------------------------

    private final RepositoryQueryPort repositoryQueryPort = mock(RepositoryQueryPort.class);
    private final OrganizationMembershipPort organizationMembershipPort =
            mock(OrganizationMembershipPort.class);
    private final RepositoryMemberManagementPolicy memberPolicy =
            new RepositoryMemberManagementPolicy(repositoryQueryPort, organizationMembershipPort);

    @Test
    void memberManagement_saysTheSameThingWhetherTheRepositoryIsMissingOrForbidden() {
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.empty());
        Throwable missing = catchThrowable(
                () -> memberPolicy.validateCanManageMembers(STRANGER_ID, REPOSITORY_ID));

        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.of(
                new io.jgitkins.server.repository.application.contract.result.RepositoryResult(
                        REPOSITORY_ID, "USER", "repo", "alice/repo", "main", "PRIVATE",
                        null, 1L, null, "/alice/repo.git", null, false, null, null, null)));
        Throwable forbidden = catchThrowable(
                () -> memberPolicy.validateCanManageMembers(STRANGER_ID, REPOSITORY_ID));

        assertThat(missing).isInstanceOf(RepositoryNotFoundException.class);
        assertThat(forbidden).isInstanceOf(RepositoryNotFoundException.class);
        assertThat(forbidden).hasMessage(missing.getMessage());
    }

    // --- repository deletion -------------------------------------------------------------------

    private final RepositoryRepository repositoryRepository = mock(RepositoryRepository.class);
    private final GitRepositoryAccessService accessService = mock(GitRepositoryAccessService.class);
    private final RepositoryDeletionPolicy deletionPolicy =
            new RepositoryDeletionPolicy(accessService, organizationMembershipPort);

    @Test
    void deletion_saysTheSameThingWhetherTheRepositoryIsMissingOrInvisible() {
        // The missing case is produced by RepositoryManagementService's lookup, the invisible case by
        // RepositoryDeletionPolicy. They are different classes, which is how they drifted apart.
        Throwable missing = catchThrowable(() -> repositoryRepository.findById(RepositoryId.of(REPOSITORY_ID))
                .orElseThrow(RepositoryNotFoundException::new));

        Repository privateRepository = Repository.rehydrate(
                RepositoryId.of(REPOSITORY_ID), OwnerType.USER, OwnerId.of(1L),
                RepositoryName.from("repo"), RepositoryPath.from("repo"), BranchName.of("main"),
                RepositoryVisibility.PRIVATE, null, "/alice/repo.git", null, null, null, null);
        when(accessService.resolvePermission(privateRepository, STRANGER_ID))
                .thenReturn(RepositoryPermission.none());
        Throwable invisible = catchThrowable(
                () -> deletionPolicy.validateCanDelete(STRANGER_ID, privateRepository));

        assertThat(missing).isInstanceOf(RepositoryNotFoundException.class);
        assertThat(invisible).isInstanceOf(RepositoryNotFoundException.class);
        assertThat(invisible).hasMessage(missing.getMessage());
    }

    // --- task 2.118: the operator signal the 404 no longer carries ------------------------------

    @Test
    void memberManagement_logsTheRequesterAndRepositoryItRefused() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(RepositoryMemberManagementPolicy.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.empty());
            catchThrowable(() -> memberPolicy.validateCanManageMembers(STRANGER_ID, REPOSITORY_ID));

            // The response says nothing on purpose. If the log says nothing either, an enumeration
            // sweep leaves no trace anywhere -- which is what task 2.92 accidentally arranged.
            assertThat(appender.list)
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
                        assertThat(event.getFormattedMessage())
                                .contains(String.valueOf(STRANGER_ID))
                                .contains(String.valueOf(REPOSITORY_ID));
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
