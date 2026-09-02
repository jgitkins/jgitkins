package io.jgitkins.server.repository.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryMemberPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistence;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.BranchJpaQueryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.BranchJpaRepository;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.BranchJpaRepositoryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaRepository;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryMemberJpaPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryMemberJpaRepository;
import io.jgitkins.server.repository.adapter.out.persistence.query.BranchQueryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.repository.BranchRepositoryAdapter;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.repository.adapter.out.persistence.support.BranchDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryMemberDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.BranchEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryMemberEntityMbgMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Composition root for the repository context's persistence technology.
 *
 * <p>Task 2.72. The slice is the whole context — repository, repository member, branch writes and
 * branch reads — because the four adapters share tables and a request path. Migrating them one at a
 * time would leave a state where a repository was read through JPA and its branches through MyBatis
 * inside a single transaction, which is exactly the split the selector exists to prevent.
 *
 * <p>The four MyBatis adapters lost their {@code @Component} annotations: with both providers on the
 * classpath, component scanning would register two beans for each port and the injection point would
 * be ambiguous. Constructing both here makes the choice one exhaustive switch per port, so adding a
 * third implementation would fail to compile rather than silently pick one.
 */
@Configuration
public class RepositoryPersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "repository-reference";
    static final String PROPERTY_NAME =
            PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    /**
     * Capability-scoped so the container never holds a bare {@link PersistenceImplementation} bean.
     * Every migrated capability would otherwise contribute another bean of that same type and the
     * injection points would start colliding on the fifth one.
     */
    public record RepositoryPersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    RepositoryPersistenceSelection repositoryPersistenceSelection(Environment environment) {
        return new RepositoryPersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    /**
     * Both adapters take the same three cross-context ports and neither takes another context's
     * repositories. This used to name six of identity's and collaboration's mappers and repositories,
     * which made this composition root depend on which provider those contexts run on. The ports
     * resolve to whichever provider each owning context selected.
     */
    @Bean
    RepositoryPersistence repositoryPersistence(
            RepositoryPersistenceSelection selection,
            RepositoryEntityMbgMapper repositoryEntityMbgMapper,
            RepositoryDomainMapper repositoryDomainMapper,
            RepositoryJpaRepository repositoryJpaRepository,
            UserNamespacePort userNamespacePort,
            OrganizationNamespacePort organizationNamespacePort,
            OrganizationMembershipPort organizationMembershipPort,
            CloneUrlBuilder cloneUrlBuilder) {
        return switch (selection.implementation()) {
            case MYBATIS -> new RepositoryPersistenceAdapter(userNamespacePort,
                    organizationNamespacePort, organizationMembershipPort, repositoryEntityMbgMapper,
                    cloneUrlBuilder, repositoryDomainMapper);
            case JPA -> new RepositoryJpaPersistenceAdapter(repositoryJpaRepository, userNamespacePort,
                    organizationNamespacePort, organizationMembershipPort, cloneUrlBuilder);
        };
    }

    // No separate beans re-expose RepositoryRepository and RepositoryQueryPort. The single
    // RepositoryPersistence bean already satisfies both by type, and adding the two forwarding beans
    // made three candidates for RepositoryRepository, so every injection point for it became
    // ambiguous -- in production, not only in tests. RepositoryJpaSelectorTest caught it and now
    // asserts the two ports resolve to the same object.

    @Bean
    RepositoryMemberPersistencePort repositoryMemberPersistencePort(
            RepositoryPersistenceSelection selection,
            RepositoryMemberEntityMbgMapper repositoryMemberEntityMbgMapper,
            RepositoryMemberDomainMapper repositoryMemberDomainMapper,
            RepositoryMemberJpaRepository repositoryMemberJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new RepositoryMemberPersistenceAdapter(
                    repositoryMemberEntityMbgMapper, repositoryMemberDomainMapper);
            case JPA -> new RepositoryMemberJpaPersistenceAdapter(repositoryMemberJpaRepository);
        };
    }

    @Bean
    BranchRepository branchRepository(
            RepositoryPersistenceSelection selection,
            BranchDomainMapper branchDomainMapper,
            BranchEntityMbgMapper branchEntityMbgMapper,
            BranchJpaRepository branchJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new BranchRepositoryAdapter(branchDomainMapper, branchEntityMbgMapper);
            case JPA -> new BranchJpaRepositoryAdapter(branchJpaRepository);
        };
    }

    @Bean
    BranchQueryPort branchQueryPort(
            RepositoryPersistenceSelection selection,
            BranchEntityMbgMapper branchEntityMbgMapper,
            BranchJpaRepository branchJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new BranchQueryAdapter(branchEntityMbgMapper);
            case JPA -> new BranchJpaQueryAdapter(branchJpaRepository);
        };
    }
}
