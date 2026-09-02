package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.jgitkins.server.identity.access.adapter.out.persistence.UserCredentialPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserIdentityPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserCredentialDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserIdentityDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserCredentialsEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserIdentitiesEntityMbgMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The two providers must agree on degenerate arguments, not just on happy paths.
 *
 * <p>The selector's whole premise is that flipping it changes nothing observable. Per-provider tests
 * cannot see a disagreement, because each one passes against its own behaviour. This test compares
 * them directly, and it exists because two real disagreements were found by reading the pair
 * side by side: the JPA credential adapter had dropped a blank-provider guard, and the MyBatis
 * identity adapter dereferenced a null provider name and reported the resulting NPE as
 * {@code PERSISTENCE_OPERATION_FAILED} — blaming the database for a caller's null.
 *
 * <p>Each case also asserts the store was never touched. Returning empty by coincidence — after
 * issuing a query that happened to match nothing — is not the same contract as refusing to query.
 */
class IdentityProviderParityTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void bothProvidersRefuseToQueryForABlankCredentialProvider(String provider) {
        UserCredentialsEntityMbgMapper mbgMapper = mock(UserCredentialsEntityMbgMapper.class);
        UserCredentialJpaRepository jpaRepository = mock(UserCredentialJpaRepository.class);

        var mybatis = new UserCredentialPersistenceAdapter(mbgMapper, mock(UserCredentialDomainMapper.class));
        var jpa = new UserCredentialJpaPersistenceAdapter(jpaRepository);

        assertThat(mybatis.findByUserIdAndProvider(1L, provider)).isEmpty();
        assertThat(jpa.findByUserIdAndProvider(1L, provider)).isEmpty();
        assertThat(mybatis.findAllByUserIdAndProvider(1L, provider)).isEmpty();
        assertThat(jpa.findAllByUserIdAndProvider(1L, provider)).isEmpty();

        verifyNoInteractions(mbgMapper, jpaRepository);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"null, sub", "'', sub", "'   ', sub", "github, null", "github, ''"})
    void bothProvidersRefuseToQueryForABlankIdentityProvider(String providerName, String providerSub) {
        UserIdentitiesEntityMbgMapper mbgMapper = mock(UserIdentitiesEntityMbgMapper.class);
        UserIdentityJpaRepository jpaRepository = mock(UserIdentityJpaRepository.class);

        var mybatis = new UserIdentityPersistenceAdapter(mbgMapper, mock(UserIdentityDomainMapper.class));
        var jpa = new UserIdentityJpaPersistenceAdapter(jpaRepository);

        assertThat(mybatis.findByProvider(providerName, providerSub))
                .as("a null provider name is a caller error, not a database failure")
                .isEmpty();
        assertThat(jpa.findByProvider(providerName, providerSub)).isEmpty();

        verifyNoInteractions(mbgMapper, jpaRepository);
    }

    @Test
    void bothProvidersReturnAnEmptyListForANullUserId() {
        UserIdentitiesEntityMbgMapper mbgMapper = mock(UserIdentitiesEntityMbgMapper.class);
        UserIdentityJpaRepository jpaRepository = mock(UserIdentityJpaRepository.class);

        assertThat(new UserIdentityPersistenceAdapter(mbgMapper, mock(UserIdentityDomainMapper.class))
                .findAllByUserId(null)).isEmpty();
        assertThat(new UserIdentityJpaPersistenceAdapter(jpaRepository).findAllByUserId(null)).isEmpty();

        verifyNoInteractions(mbgMapper, jpaRepository);
    }
}
