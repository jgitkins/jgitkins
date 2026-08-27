package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.adapter.out.persistence.model.DispatchableJobRow;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * The native dispatch query binds to {@link JobDispatchJpaProjection} by column alias, and nothing in
 * the compiler or the container checks that the two agree.
 *
 * <p>A renamed getter or a renamed alias produces a {@code null} field at runtime, not an error. For
 * this projection that is a quiet wrong answer rather than a crash: a null clone path hands the runner
 * nothing to clone, and a null owner type turns every repository into a user-owned one, dropping the
 * organization id. So the agreement is asserted here, statically, where it is cheap.
 *
 * <p>The second test pins the projection against {@code DispatchableJobRow}, the MyBatis projection.
 * The two are deliberately separate types, but they must describe the same row — if one gains a field
 * the other does not, the two providers return different information for the same job.
 */
class JobDispatchJpaMappingTest {

    private static final Pattern ALIAS = Pattern.compile("(?i)\\bAS\\s+([A-Za-z_][A-Za-z0-9_]*)");

    @Test
    void mapsDispatchableProjection() {
        Set<String> aliases = aliasesInQuery();
        Set<String> properties = projectionProperties();

        assertThat(aliases)
                .as("every alias in the native query must have a getter on the projection, or Spring "
                        + "Data silently leaves the field null")
                .containsAll(properties);
        assertThat(properties)
                .as("and every getter must have an alias, for the same reason in the other direction")
                .containsAll(aliases);
    }

    @Test
    void describesTheSameRowAsTheMybatisProjection() {
        Set<String> jpa = projectionProperties();
        Set<String> mybatis = Arrays.stream(DispatchableJobRow.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(jpa)
                .as("the two providers must return the same information for the same job; a field on "
                        + "one and not the other is a difference the selector is supposed to hide")
                .isEqualTo(mybatis);
    }

    @Test
    void doesNotInheritWriteOperations() {
        Set<String> methods = Arrays.stream(JobDispatchJpaRepository.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods)
                .as("the dispatch query is read-only and has no entity of its own; extending "
                        + "JpaRepository would expose save and deleteAll on a projection")
                .doesNotContain("save", "saveAll", "delete", "deleteAll", "deleteById", "flush");
    }

    private static Set<String> aliasesInQuery() {
        String sql = JobDispatchJpaRepository.SELECT_AND_FROM;
        Matcher matcher = ALIAS.matcher(sql);
        Set<String> found = new java.util.HashSet<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        // "FROM JOB JOB" and the JOIN use bare table aliases, not AS, so they do not appear here. Guard
        // against the regex silently matching nothing at all, which would make this test vacuous.
        assertThat(found).hasSizeGreaterThan(5);
        return found;
    }

    private static Set<String> projectionProperties() {
        return Arrays.stream(JobDispatchJpaProjection.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("get"))
                .map(name -> Character.toLowerCase(name.charAt(3)) + name.substring(4))
                .collect(Collectors.toSet());
    }
}
