package io.jgitkins.server.identity.access.adapter.in.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The resolver's whole job is to separate three outcomes that are easy to conflate: no principal, a
 * principal that is not a usable id, and a usable id.
 *
 * <p>The fixture has no collaborators at all. That is the assertion about scope, made by construction
 * rather than by a {@code verifyNoInteractions}: a resolver that needed a repository to decide whether a
 * subject is well-formed would be doing authorization, not parsing.
 */
class RequesterUserIdResolverTest {

    private final RequesterUserIdResolver resolver = new RequesterUserIdResolver();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n", "\r", "   \t\n\r  "})
    void treatsAnAbsentPrincipalAsUnauthenticatedRatherThanMalformed(String absent) {
        assertThat(resolver.resolve(absent))
                .as("no principal was presented, so the caller decides the response; throwing here "
                        + "would make an anonymous request indistinguishable from a corrupted one")
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0", "00", "000", "0000000000",
            "-1", "+1", "-0", " 42", "42 ", "\t42",
            "abc", "4a2", "42.0", "42L", "4_2",
            "٥",
            "9999999999999999999999",
    })
    void rejectsMissingBlankAndMalformedSubjects(String malformed) {
        assertThatThrownBy(() -> resolver.resolve(malformed))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Authentication required")
                .extracting(thrown -> ((ApplicationException) thrown).getErrorCode())
                .isEqualTo(ApplicationErrorCode.UNAUTHENTICATED);
    }

    @Test
    void rejectsANonAsciiDigitThatWouldOtherwiseParse() {
        // Long.parseLong accepts Arabic-Indic and Devanagari digits, so "٥" parses to 5 and
        // "५" to 5 as well. An id whose value depends on the script it was written in is not an id,
        // and a token carrying one is not a token this service issued.
        assertThatThrownBy(() -> resolver.resolve("५"))
                .isInstanceOf(ApplicationException.class);
        assertThat(Long.parseLong("५"))
                .as("recorded so the reason for the explicit ASCII range is visible: the platform really "
                        + "does parse this, which is why Character.isDigit is not enough")
                .isEqualTo(5L);
    }

    @Test
    void resolvesNumericSubject() {
        assertThat(resolver.resolve("1")).contains(1L);
        assertThat(resolver.resolve("42")).contains(42L);
        assertThat(resolver.resolve("0000000009"))
                .as("leading zeros are a valid decimal id and must resolve to the parsed number, not to "
                        + "the string")
                .contains(9L);
        assertThat(resolver.resolve(String.valueOf(Long.MAX_VALUE))).contains(Long.MAX_VALUE);
    }
}
