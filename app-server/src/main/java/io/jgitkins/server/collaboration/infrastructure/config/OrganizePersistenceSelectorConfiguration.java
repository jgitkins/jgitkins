package io.jgitkins.server.collaboration.infrastructure.config;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistence;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistence;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Binds the persistence implementation for the Organize / OrganizeMember reference slice.
 *
 * <pre>
 *   jgitkins.persistence.app-server.organize-organize-member-reference.implementation
 *            |
 *            +-- absent or "mybatis" --> OrganizePersistenceAdapter
 *            |                           OrganizeMemberPersistenceAdapter        (wired here)
 *            |
 *            +-- "jpa" ---------------> OrganizeJpaPersistenceAdapter
 *            |                           OrganizeMemberJpaPersistenceAdapter    (wired here)
 *            |
 *            +-- anything else -------> InvalidPersistenceSelectorException      (fails startup)
 * </pre>
 *
 * <p>Why the adapters are {@code @Bean}s here instead of {@code @Component}s: the application
 * declares {@code @ComponentScan("io.jgitkins.server")}, so a component-annotated adapter is
 * registered unconditionally and no selector can displace it. Worse, adding a second
 * component-annotated implementation of {@code OrganizeRepository} would not produce a choice, it
 * would produce {@code NoUniqueBeanDefinitionException} at startup. Constructing them here is what
 * makes the selection actually possible.
 *
 * <p>The switch is exhaustive over the enum, so adding a third implementation is a compile error
 * here rather than a silent fallthrough to one of the existing two.
 *
 * <p>Both adapters resolve from one property on purpose. Organize and OrganizeMember share
 * membership invariants and a row lock, so a half-cutover slice could hold the owner invariant in
 * one store and violate it in the other.
 */
@Configuration
public class OrganizePersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "organize-organize-member-reference";
    static final String PROPERTY_NAME = PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    /**
     * The resolved selection for this capability slice, and only this one.
     *
     * <p>Wrapped in a capability-specific type rather than exposed as a bare
     * {@link PersistenceImplementation}. Every slice in the 2.70-2.77 chain will expose its own
     * resolved selection, and a bare enum bean would give the context N beans of one type. Injection
     * would then resolve only by parameter-name matching, which silently depends on compiling with
     * {@code -parameters} and breaks the moment anyone injects the type without matching the bean
     * name. A distinct record per slice makes each selection unambiguous by construction.
     *
     * <p>Exposed as a bean at all so a cutover and a rollback are distinguishable by inspection
     * rather than assumed from the deployment that was intended.
     */
    public record OrganizePersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    OrganizePersistenceSelection organizePersistenceSelection(Environment environment) {
        return new OrganizePersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    OrganizePersistence organizePersistence(
            OrganizePersistenceSelection organizePersistenceSelection,
            OrganizeEntityMbgMapper organizeEntityMbgMapper,
            OrganizeDomainMapper organizeDomainMapper,
            OrganizeJpaRepository organizeJpaRepository) {
        return switch (organizePersistenceSelection.implementation()) {
            case MYBATIS -> new OrganizePersistenceAdapter(organizeEntityMbgMapper, organizeDomainMapper);
            case JPA -> new OrganizeJpaPersistenceAdapter(organizeJpaRepository);
        };
    }

    @Bean
    OrganizeMemberPersistence organizeMemberPersistence(
            OrganizePersistenceSelection organizePersistenceSelection,
            OrganizeMemberEntityMbgMapper organizeMemberEntityMbgMapper,
            OrganizeMemberDomainMapper organizeMemberDomainMapper,
            OrganizeMemberJpaRepository organizeMemberJpaRepository) {
        return switch (organizePersistenceSelection.implementation()) {
            case MYBATIS -> new OrganizeMemberPersistenceAdapter(
                    organizeMemberEntityMbgMapper, organizeMemberDomainMapper);
            case JPA -> new OrganizeMemberJpaPersistenceAdapter(organizeMemberJpaRepository);
        };
    }
}
