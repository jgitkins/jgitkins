package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizeTest {

    @Test
    void shouldCreateOrganizeWhenNameIsValid() {

        Organize organize = Organize.create(OrganizeName.from("  dev_team  "),
                                            UserId.of(42L),
                                            "  Leading the way  ");

        assertThat(organize.getName().getValue()).isEqualTo("dev_team");
        assertThat(organize.getDescription()).isEqualTo("Leading the way");
        assertThat(organize.getOwnerId()).isNotNull();
        assertThat(organize.getOwnerId().getValue()).isEqualTo(42L);
        assertThat(organize.getCreatedAt()).isNotNull();
        assertThat(organize.getUpdatedAt()).isNotNull();
        assertThat(organize.getDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrganizeCreatedEvent.class);
    }

    @Test
    void shouldRejectWhenNameContainsSpaces() {
        assertThatThrownBy(() -> OrganizeName.from("team space"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organize name");
    }

    // Allowed characters: A–Z, a–z, 0–9, '_' and '-'
    @Test
    void shouldRejectWhenNameContainsNotAllowedCharacters() {
        assertThatThrownBy(() -> OrganizeName.from("team!space"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organize name");
    }
}
