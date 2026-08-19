package io.jgitkins.server.repository.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RepositoryMemberUserIdTest {

    @Test
    void preservesValueAndEquality() {
        RepositoryMemberUserId first = RepositoryMemberUserId.of(7L);
        RepositoryMemberUserId second = RepositoryMemberUserId.of(7L);

        assertThat(first.value()).isEqualTo(7L);
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
