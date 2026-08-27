package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserCredentialPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserIdentityPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistenceAdapter;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.infrastructure.config.IdentityPersistenceSelectorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The rollback half of the identity cutover contract.
 *
 * <p>A cutover nobody can undo is not a cutover. Resetting the selector to {@code mybatis} must
 * restore all three MyBatis adapters, and deleting the property must land in the same place, because
 * an operator undoing a bad deploy is as likely to remove the property as to set it back.
 */
class IdentityJpaRollbackTest {

    private static final String PROPERTY = PersistenceImplementationSelector.propertyName(
            "app-server", "identity-access-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(IdentityJpaSelectorTest.Stubs.class,
                    IdentityPersistenceSelectorConfiguration.class);

    @Test
    void rollsBackReferenceSliceToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(UserRepository.class))
                        .isInstanceOf(UserJpaPersistenceAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(UserRepository.class)).isInstanceOf(UserPersistenceAdapter.class);
            assertThat(context.getBean(UserCredentialPersistencePort.class))
                    .isInstanceOf(UserCredentialPersistenceAdapter.class);
            assertThat(context.getBean(UserIdentityPersistencePort.class))
                    .as("all three adapters must come back, not just the user; a partial rollback is the "
                            + "same split state the atomic-slice rule forbids")
                    .isInstanceOf(UserIdentityPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(UserJpaPersistenceAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(UserRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(UserPersistenceAdapter.class);
        });
    }
}
