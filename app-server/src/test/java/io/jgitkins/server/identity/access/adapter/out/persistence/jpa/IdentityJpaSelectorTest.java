package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserCredentialPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserIdentityPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistenceAdapter;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.infrastructure.config.IdentityPersistenceSelectorConfiguration;
import io.jgitkins.server.identity.access.infrastructure.config.IdentityPersistenceSelectorConfiguration.IdentityPersistenceSelection;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserCredentialDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserIdentityDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.mapper.UserCredentialsEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.mapper.UserIdentitiesEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Selection behaviour for the identity capability.
 *
 * <p>The identity slice has three adapters rather than two, so the thing most worth asserting is
 * that they move together. A selector that swapped the user but left credentials on MyBatis would
 * still boot and still pass every per-adapter test, while a login read an identity through one
 * provider and its user through the other.
 */
class IdentityJpaSelectorTest {

    // Derived, not copied: the configuration keeps its constant package-private, and a duplicated
    // literal would keep passing after the real namespace changed.
    private static final String PROPERTY = PersistenceImplementationSelector.propertyName(
            "app-server", "identity-access-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, IdentityPersistenceSelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(IdentityPersistenceSelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(UserRepository.class)).isInstanceOf(UserPersistenceAdapter.class);
            assertThat(context.getBean(UserCredentialPersistencePort.class))
                    .isInstanceOf(UserCredentialPersistenceAdapter.class);
            assertThat(context.getBean(UserIdentityPersistencePort.class))
                    .isInstanceOf(UserIdentityPersistenceAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void allThreeAdaptersMoveTogether() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(UserRepository.class)).isInstanceOf(UserJpaPersistenceAdapter.class);
            assertThat(context.getBean(UserCredentialPersistencePort.class))
                    .isInstanceOf(UserCredentialJpaPersistenceAdapter.class);
            assertThat(context.getBean(UserIdentityPersistencePort.class))
                    .as("a slice that migrated the user but not its credentials would still boot, and a "
                            + "login would read an identity through one provider and its user through "
                            + "the other")
                    .isInstanceOf(UserIdentityJpaPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(UserPersistenceAdapter.class);
        });
    }

    static class Stubs {
        @Bean
        UserEntityMbgMapper userEntityMbgMapper() {
            return Mockito.mock(UserEntityMbgMapper.class);
        }

        @Bean
        UserCredentialsEntityMbgMapper userCredentialsEntityMbgMapper() {
            return Mockito.mock(UserCredentialsEntityMbgMapper.class);
        }

        @Bean
        UserIdentitiesEntityMbgMapper userIdentitiesEntityMbgMapper() {
            return Mockito.mock(UserIdentitiesEntityMbgMapper.class);
        }

        @Bean
        UserDomainMapper userDomainMapper() {
            return Mockito.mock(UserDomainMapper.class);
        }

        @Bean
        UserCredentialDomainMapper userCredentialDomainMapper() {
            return Mockito.mock(UserCredentialDomainMapper.class);
        }

        @Bean
        UserIdentityDomainMapper userIdentityDomainMapper() {
            return Mockito.mock(UserIdentityDomainMapper.class);
        }

        @Bean
        UserJpaRepository userJpaRepository() {
            return Mockito.mock(UserJpaRepository.class);
        }

        @Bean
        UserCredentialJpaRepository userCredentialJpaRepository() {
            return Mockito.mock(UserCredentialJpaRepository.class);
        }

        @Bean
        UserIdentityJpaRepository userIdentityJpaRepository() {
            return Mockito.mock(UserIdentityJpaRepository.class);
        }
    }
}
