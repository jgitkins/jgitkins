package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Column and type assertions for {@code PULL_REQUEST}, without a database. */
class PullRequestJpaMappingTest {

    @Test
    void mapsPullRequestFieldsAndTimestamps() {
        assertThat(PullRequestJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("PULL_REQUEST");
        assertThat(PullRequestJpaEntity.class.getAnnotation(Entity.class)).isNotNull();

        assertThat(columns()).containsExactlyInAnyOrder(
                "ID", "REPOSITORY_ID", "SOURCE_BRANCH", "SOURCE_HEAD", "TARGET_BRANCH", "TARGET_HEAD",
                "STATUS", "TARGET_DRIFTED", "PREVIOUS_TARGET_HEAD", "CURRENT_TARGET_HEAD",
                "CREATED_AT", "UPDATED_AT");
    }

    @Test
    void keepsBothTimestampsWritableAndDriftFlagPrimitive() {
        assertThat(readOnly())
                .as("the adapter sets CREATED_AT and UPDATED_AT explicitly on every write, so neither "
                        + "may be database-owned; a read-only UPDATED_AT would silently start relying on "
                        + "ON UPDATE current_timestamp() instead")
                .isEmpty();

        assertThat(typeOf("TARGET_DRIFTED"))
                .as("NOT NULL DEFAULT 0 in the DDL and two states in TargetDrift; a Boolean would invent "
                        + "a third the column cannot store")
                .isEqualTo(boolean.class);
    }

    private static Set<String> columns() {
        return fields().stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null)
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static Set<String> readOnly() {
        return fields().stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null && (!c.insertable() || !c.updatable()))
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static Class<?> typeOf(String column) {
        return fields().stream()
                .filter(f -> f.getAnnotation(Column.class) != null
                        && f.getAnnotation(Column.class).name().equals(column))
                .map(Field::getType)
                .findFirst()
                .orElseThrow();
    }

    private static java.util.List<Field> fields() {
        return Arrays.stream(PullRequestJpaEntity.class.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .toList();
    }
}
