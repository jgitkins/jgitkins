package io.jgitkins.server.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.command.PushHookRequest;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushEventCommandResolverTest {

    @Mock
    private RepositoryPersistencePort repositoryPort;

    @Test
    void resolve_buildsPushEventCommandFromPushHookRequest() {
        PushEventCommandResolver resolver = new PushEventCommandResolver(repositoryPort, "/bare");
        when(repositoryPort.findByPath("/bare/users/alice/repo.git"))
                .thenReturn(Optional.of(repository()));

        PushHookRequest request = new PushHookRequest(
                "/bare/users/alice/repo.git",
                7L,
                "main",
                true,
                false,
                "0123456789012345678901234567890123456789"
        );

        PushEventCommand result = resolver.resolve(request);

        assertThat(result.getRepositoryId()).isEqualTo(9L);
        assertThat(result.getNamespace()).isEqualTo("users/alice");
        assertThat(result.getRepoName()).isEqualTo("repo");
        assertThat(result.getBranchName()).isEqualTo("main");
        assertThat(result.getCommitHash()).isEqualTo("0123456789012345678901234567890123456789");
        assertThat(result.isBranchCreated()).isTrue();
    }

    @Test
    void resolve_throwsWhenRepositoryCannotBeResolved() {
        PushEventCommandResolver resolver = new PushEventCommandResolver(repositoryPort, "/bare");
        when(repositoryPort.findByPath("/bare/users/alice/repo.git")).thenReturn(Optional.empty());

        PushHookRequest request = new PushHookRequest(
                "/bare/users/alice/repo.git",
                7L,
                "main",
                true,
                false,
                "0123456789012345678901234567890123456789"
        );

        assertThrows(ApplicationException.class, () -> resolver.resolve(request));
    }

    @Test
    void resolve_fallsBackToClonePathWhenAbsoluteGitDirIsProvided() {
        PushEventCommandResolver resolver = new PushEventCommandResolver(repositoryPort, "/Users/hwiryungkim/jgitkins/bare");
        when(repositoryPort.findByPath("/Users/hwiryungkim/jgitkins/bare/hrk11mmmm/private-m2.git"))
                .thenReturn(Optional.empty());
        when(repositoryPort.findByClonePath("/hrk11mmmm/private-m2.git"))
                .thenReturn(Optional.of(privateRepository()));

        PushHookRequest request = new PushHookRequest(
                "/Users/hwiryungkim/jgitkins/bare/hrk11mmmm/private-m2.git",
                7L,
                "main",
                true,
                false,
                "0123456789012345678901234567890123456789"
        );

        PushEventCommand result = resolver.resolve(request);

        assertThat(result.getRepoName()).isEqualTo("private-m2");
        assertThat(result.getNamespace()).isEqualTo("hrk11mmmm");
    }

    private Repository repository() {
        return Repository.rehydrate(
                RepositoryId.of(9L),
                OwnerType.USER,
                OwnerId.of(1L),
                RepositoryName.from("repo"),
                RepositoryPath.from("repo"),
                null,
                null,
                "desc",
                "/users/alice/repo.git",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private Repository privateRepository() {
        return Repository.rehydrate(
                RepositoryId.of(19L),
                OwnerType.USER,
                OwnerId.of(11L),
                RepositoryName.from("private-m2"),
                RepositoryPath.from("private-m2"),
                null,
                null,
                "desc",
                "/hrk11mmmm/private-m2.git",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }
}
