package io.jgitkins.server.change.review.domain.model.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class ReviewRepositoryIdTest {
    @Test void acceptsPositiveValuesAndExposesScalar() { assertThat(ReviewRepositoryId.of(1L).value()).isEqualTo(1L); }
    @Test void rejectsNullAndNonPositiveValues() {
        assertThatThrownBy(() -> ReviewRepositoryId.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewRepositoryId.of(0L)).isInstanceOf(IllegalArgumentException.class);
    }
}
