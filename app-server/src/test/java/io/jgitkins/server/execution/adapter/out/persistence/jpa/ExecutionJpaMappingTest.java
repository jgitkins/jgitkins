package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Column and shape assertions for the execution-job entities, without a database.
 *
 * <p>The association assertion is the one that matters. {@code JOB_ID} must stay a plain column: the
 * reads this slice replaces never join, and turning it into a {@code @ManyToOne} would have Hibernate
 * load the parent job for every history row. That changes the query plan of the dispatch hot path,
 * which is exactly what a technology swap behind a selector is not allowed to do — and it would show
 * up as a latency regression under load, not as a test failure.
 */
class ExecutionJpaMappingTest {

    @Test
    void mapsJobAndHistory() {
        assertThat(JobJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("JOB");
        assertThat(JobJpaEntity.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(JobHistoryJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("JOB_HISTORY");
        assertThat(JobHistoryJpaEntity.class.getAnnotation(Entity.class)).isNotNull();

        assertThat(columns(JobJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "REPOSITORY_ID", "COMMIT_HASH", "BRANCH_NAME", "TRIGGERED_BY", "CREATED_AT");

        assertThat(columns(JobHistoryJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "JOB_ID", "RUNNER_ID", "STATUS", "LOG_PATH",
                "STARTED_AT", "FINISHED_AT", "CREATED_AT");
    }

    @Test
    void keepsJobIdAsAPlainColumnRatherThanAnAssociation() {
        assertThat(annotationsOf(JobHistoryJpaEntity.class, ManyToOne.class))
                .as("a @ManyToOne to JobJpaEntity would fetch the parent job on every history row and "
                        + "change the dispatch query plan")
                .isEmpty();
        assertThat(annotationsOf(JobHistoryJpaEntity.class, JoinColumn.class)).isEmpty();
        assertThat(annotationsOf(JobJpaEntity.class, OneToMany.class))
                .as("histories are loaded by an explicit ordered query, not by a collection mapping; "
                        + "the sequence number is positional and a collection would not preserve it")
                .isEmpty();
    }

    private static Set<String> columns(Class<?> entity) {
        return fields(entity).stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null)
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static Set<String> annotationsOf(Class<?> entity, Class<? extends java.lang.annotation.Annotation> a) {
        return fields(entity).stream()
                .filter(f -> f.getAnnotation(a) != null)
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private static java.util.List<Field> fields(Class<?> entity) {
        return Arrays.stream(entity.getDeclaredFields()).filter(f -> !f.isSynthetic()).toList();
    }
}
