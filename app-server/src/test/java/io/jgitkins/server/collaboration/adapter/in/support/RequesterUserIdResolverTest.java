package io.jgitkins.server.collaboration.adapter.in.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequesterUserIdResolverTest {

    private final RequesterUserIdResolver resolver = new RequesterUserIdResolver();

    @Test
    void resolvesNumericSubject() {
        assertThat(resolver.resolve("7")).contains(7L);
    }

    @Test
    void rejectsMissingBlankAndMalformedSubjects() {
        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve(" ")).isEmpty();
        assertThat(resolver.resolve("not-a-number")).isEmpty();
    }
}