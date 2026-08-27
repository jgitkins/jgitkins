package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Column and shape assertions for the runner entities.
 *
 * <p>The one-to-one assertion is the point. {@code RUNNER_ASSIGNMENT} has no unique key on
 * {@code RUNNER_ID}, so a runner accumulates assignment rows and the effective scope is the newest by
 * {@code ASSIGNED_AT}. A {@code @OneToOne} would encode a uniqueness claim the schema does not make,
 * and Hibernate would then return an arbitrary row where the MyBatis read deliberately returned the
 * latest — silently changing which dispatch scope a runner has.
 */
class RunnerJpaMappingTest {

    @Test
    void mapsRunnerAndAssignments() {
        assertThat(RunnerJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("RUNNER");
        assertThat(RunnerJpaEntity.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(RunnerAssignmentJpaEntity.class.getAnnotation(Table.class).name())
                .isEqualTo("RUNNER_ASSIGNMENT");
        assertThat(RunnerAssignmentJpaEntity.class.getAnnotation(Entity.class)).isNotNull();

        assertThat(columns(RunnerJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "TOKEN", "DESCRIPTION", "STATUS", "IP_ADDRESS", "LAST_HEARTBEAT_AT", "CREATED_AT");

        assertThat(columns(RunnerAssignmentJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "RUNNER_ID", "TARGET_TYPE", "TARGET_ID", "ASSIGNED_AT");
    }

    @Test
    void doesNotClaimRunnerToAssignmentIsOneToOne() {
        assertThat(annotated(RunnerJpaEntity.class, OneToOne.class))
                .as("RUNNER_ASSIGNMENT has no unique key on RUNNER_ID; a one-to-one would make Hibernate "
                        + "return an arbitrary row where the read must return the newest")
                .isEmpty();
        assertThat(annotated(RunnerAssignmentJpaEntity.class, OneToOne.class)).isEmpty();
        assertThat(annotated(RunnerAssignmentJpaEntity.class, JoinColumn.class)).isEmpty();
    }

    private static Set<String> columns(Class<?> entity) {
        return fields(entity).stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null)
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static Set<String> annotated(Class<?> entity, Class<? extends java.lang.annotation.Annotation> a) {
        return fields(entity).stream()
                .filter(f -> f.getAnnotation(a) != null)
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private static java.util.List<Field> fields(Class<?> entity) {
        return Arrays.stream(entity.getDeclaredFields()).filter(f -> !f.isSynthetic()).toList();
    }
}
