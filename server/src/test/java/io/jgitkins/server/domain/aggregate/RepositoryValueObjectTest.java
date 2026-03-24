package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.model.vo.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryValueObjectTest {

    @Test
    void shouldTrimStoreValueWhenContainSpaces() {
        RepositoryName name = RepositoryName.from("  demo repo  ");
        assertThat(name.getValue()).isEqualTo("demo repo");
    }

    @Test
    void shouldRejectWhenNameIsNullOrBlank() {
        assertThatThrownBy(() -> RepositoryName.from(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepositoryName.from("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLowercaseAndValidateRepositoryPath() {
        RepositoryPath path = RepositoryPath.from("  Demo-Repo  ");
        assertThat(path.getValue()).isEqualTo("demo-repo");
    }

    @Test
    void shouldRejectWhenPathContainsInvalidCharacters() {
        assertThatThrownBy(() -> RepositoryPath.from("Repo*&"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository path");
    }

    @Test
    void shouldRejectWhenBranchNameIsNullOrBlank() {
        assertThatThrownBy(() -> BranchName.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BranchName.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDefaultVisibilityToPrivateAndRejectUnknown() {
        assertThat(RepositoryVisibility.from(null)).isEqualTo(RepositoryVisibility.PRIVATE);
        assertThat(RepositoryVisibility.from("public")).isEqualTo(RepositoryVisibility.PUBLIC);
        assertThatThrownBy(() -> RepositoryVisibility.from("secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectWhenUserIdIsNonPositive() {
        assertThat(UserId.of(10L).getValue()).isEqualTo(10L);
        assertThatThrownBy(() -> UserId.of(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
