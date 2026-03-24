package io.jgitkins.server.domain.model.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitialCommitOptionsTest {

    @Test
    void shouldExposeCommitMetadataWhenInitializationRequested() {
        InitialCommitOptions options = InitialCommitOptions.of(true,
                                                              "  init repo  ",
                                                              "  Alice  ",
                                                              "alice@example.com  ");

        assertThat(options.requiresInitialContent()).isTrue();
        assertThat(options.commitMessage()).isEqualTo("init repo");
        assertThat(options.authorName()).isEqualTo("Alice");
        assertThat(options.authorEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldRejectMissingMessageWhenReadmeInitializationEnabled() {
        assertThatThrownBy(() -> InitialCommitOptions.of(true, null, "name", "email"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InitialCommitOptions.of(true, "   ", "name", "email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMessageWhenReadmeInitializationDisabled() {
        assertThatThrownBy(() -> InitialCommitOptions.of(false, "initial commit", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        InitialCommitOptions options = InitialCommitOptions.of(false, null, "name", "email");
        assertThat(options.requiresInitialContent()).isFalse();
        assertThat(options.commitMessage()).isNull();
        assertThat(options.authorName()).isEqualTo("name");
        assertThat(options.authorEmail()).isEqualTo("email");
    }
}
