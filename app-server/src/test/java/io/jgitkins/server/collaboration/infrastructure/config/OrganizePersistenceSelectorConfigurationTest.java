package io.jgitkins.server.collaboration.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Drives the selector configuration through a real Spring context so the failure modes are the ones
 * a deployment would actually hit, not simulated ones.
 *
 * <p>The MyBatis mappers and MapStruct mappers are stubbed: this test is about which adapter pair
 * gets bound and whether a bad selector stops the context, not about SQL behaviour. Persistence
 * behaviour is covered by the adapter tests and the MariaDB evidence run.
 */
class OrganizePersistenceSelectorConfigurationTest {

    private static final String PROPERTY = OrganizePersistenceSelectorConfiguration.PROPERTY_NAME;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(StubMappers.class, OrganizePersistenceSelectorConfiguration.class);

    @Test
    void absentPropertyBindsMybatisAdapterPair() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PersistenceImplementation.class))
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context).hasSingleBean(OrganizePersistenceAdapter.class);
            assertThat(context).hasSingleBean(OrganizeMemberPersistenceAdapter.class);
        });
    }

    @Test
    void explicitMybatisValueBindsTheSamePair() {
        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PersistenceImplementation.class))
                    .isEqualTo(PersistenceImplementation.MYBATIS);
        });
    }

    /**
     * {@code jpa} is a recognised value with no adapter pair yet. It must stop the context rather
     * than fall through to MyBatis, otherwise a deployment that believes it cut over would keep
     * writing through MyBatis and report success.
     */
    @Test
    void jpaValueFailsStartupUntilTheJpaAdapterPairExists() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("no jpa adapter pair is wired");
        });
    }

    @Test
    void unknownValueFailsApplicationStartup() {
        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void exactlyOneOrganizeRepositoryAndOneOrganizeQueryPortBeanExist() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(OrganizeRepository.class)).hasSize(1);
            assertThat(context.getBeansOfType(OrganizeQueryPort.class)).hasSize(1);
            assertThat(context.getBeansOfType(OrganizeMemberPersistencePort.class)).hasSize(1);
            assertThat(context.getBeansOfType(OrganizeMembershipQueryPort.class)).hasSize(1);
        });
    }

    /**
     * One selector governs both aggregates. A per-aggregate split would let the owner invariant hold
     * in one store and be violated in the other.
     */
    @Test
    void noDualWritePathExistsForEitherValue() {
        runner.run(context -> {
            assertThat(context.getBeansOfType(OrganizeRepository.class).values())
                    .allMatch(OrganizePersistenceAdapter.class::isInstance);
            assertThat(context.getBeansOfType(OrganizeMemberPersistencePort.class).values())
                    .allMatch(OrganizeMemberPersistenceAdapter.class::isInstance);
        });
    }

    /**
     * Deliberately NOT annotated {@code @Configuration}.
     *
     * <p>{@code JGitkinsServerApplication} declares a bare {@code @ComponentScan("io.jgitkins.server")},
     * which replaces the filters {@code @SpringBootApplication} would have contributed, including
     * Spring Boot's {@code TypeExcludeFilter}. A nested {@code @Configuration} in test sources under
     * that package therefore gets picked up by every {@code @SpringBootTest} that boots the real
     * application, and these stub {@code @Bean}s would collide with the MyBatis mapper scan
     * ({@code ConflictingBeanDefinitionException} on {@code organizeEntityMbgMapper}).
     *
     * <p>{@code ApplicationContextRunner.withUserConfiguration} registers the class explicitly and
     * processes its {@code @Bean} methods in lite mode, so no stereotype is needed here and the
     * class stays invisible to component scanning.
     */
    static class StubMappers {
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
    }
}
