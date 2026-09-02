package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryMemberPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.query.BranchQueryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.repository.BranchRepositoryAdapter;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.infrastructure.config.RepositoryPersistenceSelectorConfiguration;
import io.jgitkins.server.repository.infrastructure.config.RepositoryPersistenceSelectorConfiguration.RepositoryPersistenceSelection;
import io.jgitkins.server.repository.adapter.out.persistence.support.BranchDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryMemberDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.BranchEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryMemberEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Selection behaviour for the repository capability.
 *
 * <p>This context is the widest slice migrated so far — four adapters over three tables plus three
 * cross-context reads — so the assertion that matters most is that one selector moves all four. A
 * repository read through JPA whose branches were read through MyBatis would boot, pass every
 * per-adapter test, and still be the split state the atomic-slice rule exists to forbid.
 */
class RepositoryJpaSelectorTest {

    // Derived rather than duplicated: a copied literal keeps passing after the real namespace moves.
    private static final String PROPERTY =
            PersistenceImplementationSelector.propertyName("app-server", "repository-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, RepositoryPersistenceSelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RepositoryPersistenceSelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(RepositoryRepository.class))
                    .isInstanceOf(RepositoryPersistenceAdapter.class);
            assertThat(context.getBean(RepositoryQueryPort.class))
                    .as("the two repository ports must be the same object, or one port could be served "
                            + "by the other provider")
                    .isSameAs(context.getBean(RepositoryRepository.class));
            assertThat(context.getBean(RepositoryMemberPersistencePort.class))
                    .isInstanceOf(RepositoryMemberPersistenceAdapter.class);
            assertThat(context.getBean(BranchRepository.class)).isInstanceOf(BranchRepositoryAdapter.class);
            assertThat(context.getBean(BranchQueryPort.class)).isInstanceOf(BranchQueryAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void allFourAdaptersMoveTogether() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RepositoryRepository.class))
                    .isInstanceOf(RepositoryJpaPersistenceAdapter.class);
            assertThat(context.getBean(RepositoryQueryPort.class))
                    .isSameAs(context.getBean(RepositoryRepository.class));
            assertThat(context.getBean(RepositoryMemberPersistencePort.class))
                    .isInstanceOf(RepositoryMemberJpaPersistenceAdapter.class);
            assertThat(context.getBean(BranchRepository.class))
                    .isInstanceOf(BranchJpaRepositoryAdapter.class);
            assertThat(context.getBean(BranchQueryPort.class))
                    .as("branch reads must move with branch writes; a JPA write followed by a MyBatis "
                            + "read inside one request is the split this slice forbids")
                    .isInstanceOf(BranchJpaQueryAdapter.class);
            assertThat(context).doesNotHaveBean(RepositoryPersistenceAdapter.class);
        });
    }

    static class Stubs {
        @Bean
        RepositoryEntityMbgMapper repositoryEntityMbgMapper() {
            return Mockito.mock(RepositoryEntityMbgMapper.class);
        }

        @Bean
        RepositoryMemberEntityMbgMapper repositoryMemberEntityMbgMapper() {
            return Mockito.mock(RepositoryMemberEntityMbgMapper.class);
        }

        @Bean
        BranchEntityMbgMapper branchEntityMbgMapper() {
            return Mockito.mock(BranchEntityMbgMapper.class);
        }

        @Bean
        RepositoryDomainMapper repositoryDomainMapper() {
            return Mockito.mock(RepositoryDomainMapper.class);
        }

        @Bean
        RepositoryMemberDomainMapper repositoryMemberDomainMapper() {
            return Mockito.mock(RepositoryMemberDomainMapper.class);
        }

        @Bean
        BranchDomainMapper branchDomainMapper() {
            return Mockito.mock(BranchDomainMapper.class);
        }

        @Bean
        RepositoryJpaRepository repositoryJpaRepository() {
            return Mockito.mock(RepositoryJpaRepository.class);
        }

        @Bean
        RepositoryMemberJpaRepository repositoryMemberJpaRepository() {
            return Mockito.mock(RepositoryMemberJpaRepository.class);
        }

        @Bean
        BranchJpaRepository branchJpaRepository() {
            return Mockito.mock(BranchJpaRepository.class);
        }

        // Three ports where six of another context's mappers and repositories used to be. The slice no
        // longer has to know that identity and collaboration exist, let alone which provider they run
        // on, which is the observable half of the decoupling: this configuration used to fail to start
        // without OrganizeEntityMbgMapper on the classpath.
        @Bean
        UserNamespacePort userNamespacePort() {
            return Mockito.mock(UserNamespacePort.class);
        }

        @Bean
        OrganizationNamespacePort organizationNamespacePort() {
            return Mockito.mock(OrganizationNamespacePort.class);
        }

        @Bean
        OrganizationMembershipPort organizationMembershipPort() {
            return Mockito.mock(OrganizationMembershipPort.class);
        }

        @Bean
        CloneUrlBuilder cloneUrlBuilder() {
            return new CloneUrlBuilder(Mockito.mock(RepositoryEndpointPort.class));
        }
    }
}
