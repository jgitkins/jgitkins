package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.infrastructure.config.OrganizePersistenceSelectorConfiguration;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeMemberEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * The rollback half of the cutover contract.
 *
 * <p>A cutover nobody can undo is not a cutover. The plan's rollback procedure is "set the selector
 * back to mybatis and redeploy", and this asserts that doing so actually restores the MyBatis pair
 * rather than leaving the JPA adapters wired or producing a context that fails to build.
 *
 * <p>Both directions are exercised in one test on purpose: proving the selector reaches JPA and
 * proving it comes back are the same guarantee, and splitting them would let a half-working switch
 * pass one test and fail the other with no obvious link between them.
 */
class OrganizeJpaRollbackTest {

    // Derived rather than copied. The configuration keeps its constant package-private, and a
    // duplicated literal here would keep passing after the real namespace changed.
    private static final String PROPERTY = PersistenceImplementationSelector.propertyName(
            "app-server", "organize-organize-member-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, OrganizePersistenceSelectorConfiguration.class);

    @Test
    void rollsBackSelectorToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OrganizeRepository.class))
                    .isInstanceOf(OrganizeJpaPersistenceAdapter.class);
            assertThat(context.getBean(OrganizeMemberPersistencePort.class))
                    .isInstanceOf(OrganizeMemberJpaPersistenceAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OrganizeRepository.class))
                    .as("resetting the selector must restore the MyBatis pair, which is the entire "
                            + "rollback procedure the plan relies on")
                    .isInstanceOf(OrganizePersistenceAdapter.class);
            assertThat(context.getBean(OrganizeMemberPersistencePort.class))
                    .isInstanceOf(OrganizeMemberPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(OrganizeJpaPersistenceAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OrganizeRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(OrganizePersistenceAdapter.class);
        });
    }

    static class Stubs {
        @Bean
        OrganizeEntityMbgMapper organizeEntityMbgMapper() {
            return Mockito.mock(OrganizeEntityMbgMapper.class);
        }

        @Bean
        OrganizeMemberEntityMbgMapper organizeMemberEntityMbgMapper() {
            return Mockito.mock(OrganizeMemberEntityMbgMapper.class);
        }

        @Bean
        OrganizeDomainMapper organizeDomainMapper() {
            return Mockito.mock(OrganizeDomainMapper.class);
        }

        @Bean
        OrganizeMemberDomainMapper organizeMemberDomainMapper() {
            return Mockito.mock(OrganizeMemberDomainMapper.class);
        }

        @Bean
        OrganizeJpaRepository organizeJpaRepository() {
            return Mockito.mock(OrganizeJpaRepository.class);
        }

        @Bean
        OrganizeMemberJpaRepository organizeMemberJpaRepository() {
            return Mockito.mock(OrganizeMemberJpaRepository.class);
        }
    }
}
