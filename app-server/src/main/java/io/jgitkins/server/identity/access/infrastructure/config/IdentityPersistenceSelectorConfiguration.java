package io.jgitkins.server.identity.access.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserCredentialPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserIdentityPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistence;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserCredentialJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserCredentialJpaRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserIdentityJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserIdentityJpaRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaRepository;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserCredentialDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserIdentityDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserCredentialsEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserIdentitiesEntityMbgMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Binds the persistence implementation for the identity/access capability.
 *
 * <pre>
 *   jgitkins.persistence.app-server.identity-access-reference.implementation
 *            |
 *            +-- absent or "mybatis" --> UserPersistenceAdapter
 *            |                           UserCredentialPersistenceAdapter
 *            |                           UserIdentityPersistenceAdapter
 *            |
 *            +-- "jpa" ---------------> UserJpaPersistenceAdapter
 *            |                           UserCredentialJpaPersistenceAdapter
 *            |                           UserIdentityJpaPersistenceAdapter
 *            |
 *            +-- anything else -------> InvalidPersistenceSelectorException (fails startup)
 * </pre>
 *
 * <p>Three adapters, one selector. User, credentials and federated identities are not independent:
 * a credential row is meaningless without its user, and the OAuth login path reads an identity and
 * then loads the user in the same request. Migrating them separately could leave a login reading an
 * identity through JPA and its user through MyBatis, which is exactly the split the atomic-slice rule
 * exists to prevent.
 *
 * <p>Parsing and failure behaviour are reused from {@code PersistenceImplementationSelector} rather
 * than reimplemented, so every capability in the chain rejects the same values with the same message.
 * The switch is exhaustive over the enum, so a third implementation is a compile error here.
 */
@Configuration
public class IdentityPersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "identity-access-reference";
    static final String PROPERTY_NAME = PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    /** Capability-scoped so it never collides with another slice's selection bean. */
    public record IdentityPersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    IdentityPersistenceSelection identityPersistenceSelection(Environment environment) {
        return new IdentityPersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    UserPersistence userPersistence(
            IdentityPersistenceSelection identityPersistenceSelection,
            UserEntityMbgMapper userEntityMbgMapper,
            UserDomainMapper userDomainMapper,
            UserJpaRepository userJpaRepository) {
        return switch (identityPersistenceSelection.implementation()) {
            case MYBATIS -> new UserPersistenceAdapter(userEntityMbgMapper, userDomainMapper);
            case JPA -> new UserJpaPersistenceAdapter(userJpaRepository);
        };
    }

    @Bean
    UserCredentialPersistencePort userCredentialPersistence(
            IdentityPersistenceSelection identityPersistenceSelection,
            UserCredentialsEntityMbgMapper userCredentialsEntityMbgMapper,
            UserCredentialDomainMapper userCredentialDomainMapper,
            UserCredentialJpaRepository userCredentialJpaRepository) {
        return switch (identityPersistenceSelection.implementation()) {
            case MYBATIS -> new UserCredentialPersistenceAdapter(
                    userCredentialsEntityMbgMapper, userCredentialDomainMapper);
            case JPA -> new UserCredentialJpaPersistenceAdapter(userCredentialJpaRepository);
        };
    }

    @Bean
    UserIdentityPersistencePort userIdentityPersistence(
            IdentityPersistenceSelection identityPersistenceSelection,
            UserIdentitiesEntityMbgMapper userIdentitiesEntityMbgMapper,
            UserIdentityDomainMapper userIdentityDomainMapper,
            UserIdentityJpaRepository userIdentityJpaRepository) {
        return switch (identityPersistenceSelection.implementation()) {
            case MYBATIS -> new UserIdentityPersistenceAdapter(
                    userIdentitiesEntityMbgMapper, userIdentityDomainMapper);
            case JPA -> new UserIdentityJpaPersistenceAdapter(userIdentityJpaRepository);
        };
    }
}
