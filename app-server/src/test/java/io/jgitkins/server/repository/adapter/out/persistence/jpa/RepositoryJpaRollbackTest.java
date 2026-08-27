package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryMemberPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.query.BranchQueryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.repository.BranchRepositoryAdapter;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.infrastructure.config.RepositoryPersistenceSelectorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The rollback half of the repository cutover contract.
 *
 * <p>A cutover nobody can undo is not a cutover. Resetting the selector to {@code mybatis} must bring
 * back all four MyBatis adapters, and deleting the property must land in the same place — an operator
 * undoing a bad deploy is at least as likely to remove the property as to set it back, and a rollback
 * that only works one of those two ways will be discovered at the worst possible moment.
 */
class RepositoryJpaRollbackTest {

    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "repository-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RepositoryJpaSelectorTest.Stubs.class,
                    RepositoryPersistenceSelectorConfiguration.class);

    @Test
    void rollsBackReferenceSliceToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(RepositoryRepository.class))
                        .isInstanceOf(RepositoryJpaPersistenceAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RepositoryRepository.class))
                    .isInstanceOf(RepositoryPersistenceAdapter.class);
            assertThat(context.getBean(RepositoryMemberPersistencePort.class))
                    .isInstanceOf(RepositoryMemberPersistenceAdapter.class);
            assertThat(context.getBean(BranchRepository.class)).isInstanceOf(BranchRepositoryAdapter.class);
            assertThat(context.getBean(BranchQueryPort.class))
                    .as("all four adapters must come back, not just the repository; a partial rollback "
                            + "is the same split state the atomic-slice rule forbids")
                    .isInstanceOf(BranchQueryAdapter.class);
            assertThat(context).doesNotHaveBean(RepositoryJpaPersistenceAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RepositoryRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(RepositoryPersistenceAdapter.class);
            assertThat(context.getBean(BranchQueryPort.class)).isInstanceOf(BranchQueryAdapter.class);
        });
    }
}
