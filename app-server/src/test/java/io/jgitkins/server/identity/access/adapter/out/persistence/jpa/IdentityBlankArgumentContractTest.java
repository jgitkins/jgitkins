package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A blank or null argument is refused without reaching the store.
 *
 * <p>Was {@code IdentityProviderParityTest}, which asserted the same things about the MyBatis and JPA
 * adapters at once. The comparison is gone with the MyBatis adapter; what it found before it went is
 * why these cases are kept rather than deleted with it. Reading the pair side by side surfaced two
 * real disagreements: the JPA credential adapter had dropped a blank-provider guard, and the MyBatis
 * identity adapter dereferenced a null provider name and reported the resulting NPE as
 * {@code PERSISTENCE_OPERATION_FAILED} -- blaming the database for a caller's null. Both were fixed.
 * The guards that fixed them live in the JPA adapter now, and nothing else asserts they are there.
 *
 * <p>Each case also asserts the store was never touched. Returning empty by coincidence -- after
 * issuing a query that happened to match nothing -- is not the same contract as refusing to query,
 * and only the second one keeps a null out of the generated SQL.
 */
class IdentityBlankArgumentContractTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void refusesToQueryForABlankCredentialProvider(String provider) {
        UserCredentialJpaRepository repository = mock(UserCredentialJpaRepository.class);
        var adapter = new UserCredentialJpaPersistenceAdapter(repository);

        assertThat(adapter.findByUserIdAndProvider(1L, provider)).isEmpty();
        assertThat(adapter.findAllByUserIdAndProvider(1L, provider)).isEmpty();

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"null, sub", "'', sub", "'   ', sub", "github, null", "github, ''"})
    void refusesToQueryForABlankIdentityProvider(String providerName, String providerSub) {
        UserIdentityJpaRepository repository = mock(UserIdentityJpaRepository.class);
        var adapter = new UserIdentityJpaPersistenceAdapter(repository);

        assertThat(adapter.findByProvider(providerName, providerSub))
                .as("a null provider name is a caller error, not a database failure")
                .isEmpty();

        verifyNoInteractions(repository);
    }

    @Test
    void returnsAnEmptyListForANullUserId() {
        UserIdentityJpaRepository repository = mock(UserIdentityJpaRepository.class);

        assertThat(new UserIdentityJpaPersistenceAdapter(repository).findAllByUserId(null)).isEmpty();

        verifyNoInteractions(repository);
    }
}
