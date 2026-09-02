package io.jgitkins.server.repository.domain.aggregate;

import io.jgitkins.server.repository.domain.event.RepositorySynchronizedEvent;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTest {

    @Test
    void shouldCreateRepositoryWithoutProvisionedEvent() {
        Repository repository = Repository.create(
                OwnerType.ORGANIZATION,
                RepositoryOwnerId.of(1L),
                RepositoryName.from("demo"),
                RepositoryPath.from("demo-path"),
                BranchName.of("main"),
                RepositoryVisibility.PRIVATE,
                " Demo repository ",
                "/demo/demo-path.git",
                "cred-1",
                true
        );

        assertThat(repository.getId()).isNull();
        assertThat(repository.getOwnerId().getValue()).isEqualTo(1L);
        assertThat(repository.getName().getValue()).isEqualTo("demo");
        assertThat(repository.getPath().getValue()).isEqualTo("demo-path");
        assertThat(repository.getDefaultBranch().getValue()).isEqualTo("main");
        assertThat(repository.getDescription()).isEqualTo("Demo repository");
        assertThat(repository.isRequiresInitialContent()).isTrue();
        assertThat(repository.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldMarkRepositoryAsSyncedAndEmitEvent() {
        Repository repository = Repository.create(
                OwnerType.ORGANIZATION,
                RepositoryOwnerId.of(2L),
                RepositoryName.from("demo"),
                RepositoryPath.from("demo"),
                BranchName.of("main"),
                RepositoryVisibility.PRIVATE,
                null,
                "/demo/demo.git",
                null,
                true
        );
        LocalDateTime syncedAt = LocalDateTime.now();

        Repository syncedRepository = repository.markInit(syncedAt);

        assertThat(syncedRepository.isRequiresInitialContent()).isFalse();
        assertThat(syncedRepository.getLastSyncedAt()).isEqualTo(syncedAt);
        assertThat(syncedRepository.getDomainEvents())
                .hasSize(1)
                .filteredOn(event -> event instanceof RepositorySynchronizedEvent)
                .hasSize(1);
    }

    @Test
    void shouldCopyEventsWhenAssigningIdentity() {
        Repository repository = Repository.create(OwnerType.ORGANIZATION,
                                                  RepositoryOwnerId.of(3L),
                                                  RepositoryName.from("demo"),
                                                  RepositoryPath.from("demo-path"),
                                                  BranchName.of("main"),
                                                  RepositoryVisibility.PRIVATE,
                                                  null,
                                                  "/demo/demo-path.git",
                                                  null,
                                                  false);

        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Repository withIdentity = repository.withIdentity(RepositoryId.of(100L), createdAt, updatedAt);

        assertThat(withIdentity.getId()).isEqualTo(RepositoryId.of(100L));
        assertThat(withIdentity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(withIdentity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(withIdentity.getDomainEvents()).containsExactlyElementsOf(repository.getDomainEvents());
    }
}
