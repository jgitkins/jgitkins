package io.jgitkins.server.collaboration.infrastructure.config;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
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
 *            +-- "jpa" ---------------> not wired yet, fails startup             (Task 2.69 slice)
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
     * The resolved selection, exposed as a bean so a cutover and a rollback are distinguishable by
     * inspection rather than assumed from the deployment that was intended.
     */
    @Bean
    PersistenceImplementation organizePersistenceImplementation(Environment environment) {
        return PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME));
    }

    @Bean
    OrganizePersistenceAdapter organizePersistenceAdapter(
            PersistenceImplementation organizePersistenceImplementation,
            OrganizeEntityMbgMapper organizeEntityMbgMapper,
            OrganizeDomainMapper organizeDomainMapper) {
        requireWiredImplementation(organizePersistenceImplementation);
        return new OrganizePersistenceAdapter(organizeEntityMbgMapper, organizeDomainMapper);
    }

    @Bean
    OrganizeMemberPersistenceAdapter organizeMemberPersistenceAdapter(
            PersistenceImplementation organizePersistenceImplementation,
            OrganizeMemberEntityMbgMapper organizeMemberEntityMbgMapper,
            OrganizeMemberDomainMapper organizeMemberDomainMapper) {
        requireWiredImplementation(organizePersistenceImplementation);
        return new OrganizeMemberPersistenceAdapter(organizeMemberEntityMbgMapper, organizeMemberDomainMapper);
    }

    /**
     * {@code jpa} is a valid selector value but has no adapter pair yet, so it fails loudly here
     * rather than silently serving MyBatis. Serving MyBatis under a {@code jpa} selector would make
     * cutover evidence and rollback evidence identical, which is the exact failure this mechanism
     * exists to prevent.
     */
    private static void requireWiredImplementation(PersistenceImplementation implementation) {
        if (implementation != PersistenceImplementation.MYBATIS) {
            throw new InfrastructureException(InfrastructureErrorCode.INTERNAL_ERROR,
                    "Persistence selector " + PROPERTY_NAME + " requests '" + implementation.wireValue()
                            + "' but no " + implementation.wireValue()
                            + " adapter pair is wired for the Organize/OrganizeMember slice yet; "
                            + "set it to '" + PersistenceImplementation.MYBATIS.wireValue()
                            + "' or omit it until that slice is migrated");
        }
    }
}
