package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The three repository-context entities must name the real columns, and must not claim ownership of
 * the columns the database fills in.
 *
 * <p>This runs without a database on purpose. The MariaDB test proves the mapping works against the
 * live schema; this one states which columns are deliberately read-only, so a later edit that makes
 * {@code REPOSITORY_TYPE} or {@code BRANCH.CREATED_AT} writable fails here with the reason attached
 * rather than as a {@code NOT NULL} violation somewhere downstream.
 */
class RepositoryJpaMappingTest {

    @Test
    void mapsReferenceSlice() {
        assertThat(RepositoryJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("REPOSITORY");
        assertThat(BranchJpaEntity.class.getAnnotation(Table.class).name()).isEqualTo("BRANCH");
        assertThat(RepositoryMemberJpaEntity.class.getAnnotation(Table.class).name())
                .isEqualTo("REPOSITORY_MEMBER");
        assertThat(RepositoryJpaEntity.class.getAnnotation(Entity.class)).isNotNull();

        assertThat(columns(RepositoryJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "NAME", "PATH", "OWNER_TYPE", "OWNER_ID", "CREDENTIAL_ID", "CLONE_PATH",
                "DESCRIPTION", "DEFAULT_BRANCH", "VISIBILITY", "STATUS", "LAST_SYNCED_AT",
                "CREATED_AT", "UPDATED_AT", "REPOSITORY_TYPE");

        assertThat(columns(BranchJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "REPOSITORY_ID", "NAME", "IS_LOCKED", "IS_CI", "IS_DEFAULT",
                "LOCKED_BY", "LOCKED_AT", "CREATED_AT", "UPDATED_AT");

        assertThat(columns(RepositoryMemberJpaEntity.class)).containsExactlyInAnyOrder(
                "ID", "REPOSITORY_ID", "USER_ID", "ROLE", "ADDED_AT");
    }

    @Test
    void leavesDatabaseOwnedColumnsReadOnly() {
        assertThat(readOnlyColumns(RepositoryJpaEntity.class))
                .as("REPOSITORY_TYPE has a NOT NULL DEFAULT 'GIT' and no domain field; making it "
                        + "writable would insert NULL and fail every repository create")
                .containsExactly("REPOSITORY_TYPE");

        assertThat(readOnlyColumns(BranchJpaEntity.class))
                .as("Branch carries no timestamps, and UPDATED_AT's ON UPDATE current_timestamp() "
                        + "stops firing the moment JPA writes the column itself")
                .containsExactlyInAnyOrder("CREATED_AT", "UPDATED_AT");

        assertThat(readOnlyColumns(RepositoryMemberJpaEntity.class))
                .as("ADDED_AT is app-owned: RepositoryMember.create already defaults it")
                .isEmpty();
    }

    @Test
    void mapsBranchFlagsAsPrimitivesSoTheyCannotBeNull() {
        Map<String, Class<?>> types = declaredFields(BranchJpaEntity.class).stream()
                .filter(f -> f.getAnnotation(Column.class) != null)
                .collect(Collectors.toMap(f -> f.getAnnotation(Column.class).name(), Field::getType));

        assertThat(types.get("IS_LOCKED")).isEqualTo(boolean.class);
        assertThat(types.get("IS_CI")).isEqualTo(boolean.class);
        assertThat(types.get("IS_DEFAULT")).isEqualTo(boolean.class);
    }

    private static Set<String> columns(Class<?> entity) {
        return declaredFields(entity).stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null)
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static Set<String> readOnlyColumns(Class<?> entity) {
        return declaredFields(entity).stream()
                .map(f -> f.getAnnotation(Column.class))
                .filter(c -> c != null && !c.insertable() && !c.updatable())
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    private static java.util.List<Field> declaredFields(Class<?> entity) {
        return java.util.Arrays.stream(entity.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .toList();
    }
}
