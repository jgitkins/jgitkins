package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.architecture.ArchitectureScanner.Category;
import io.jgitkins.server.architecture.ArchitectureScanner.Violation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * No bounded context's domain may name a framework or a persistence technology.
 *
 * <p>Per-context purity tests already exist for collaboration, repository, execution and change-review.
 * This one is deliberately not a fifth: it scans **every** context's domain with one rule set, so the
 * next context added is covered on the day it appears rather than on the day someone remembers to write
 * its guard. The existing four stay — they encode context-specific bans this one does not.
 *
 * <p>Each category is proven to actually fire by a negative-control fixture under
 * {@code src/test/resources/architecture/negative}. A purity test that has never been observed failing
 * is indistinguishable from a purity test whose regex matches nothing.
 */
class BoundedContextDomainPurityArchitectureTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "repository", "execution", "identity/access", "change/review");

    private static final List<Category> DOMAIN_CATEGORIES = List.of(
            ArchitectureScanner.FORBIDDEN_SPRING,
            ArchitectureScanner.FORBIDDEN_JPA,
            ArchitectureScanner.FORBIDDEN_SERVLET,
            ArchitectureScanner.FORBIDDEN_MYBATIS,
            ArchitectureScanner.FORBIDDEN_SPRING_DATA,
            ArchitectureScanner.FORBIDDEN_JGIT,
            ArchitectureScanner.FORBIDDEN_JWT,
            ArchitectureScanner.FORBIDDEN_PERSISTENCE_ADAPTER);

    @Test
    void forbidsFrameworkAndPersistenceImports() throws IOException {
        List<Path> domainRoots = new ArrayList<>();
        for (String context : CONTEXTS) {
            domainRoots.add(ArchitectureScanner.mainRoot().resolve(context).resolve("domain"));
        }

        assertThat(domainRoots.stream().filter(Files::isDirectory).toList())
                .as("no domain root was found; the scan is broken rather than the code")
                .isNotEmpty();

        List<Violation> violations = ArchitectureScanner.scanTree(domainRoots, DOMAIN_CATEGORIES);

        assertThat(violations)
                .as("a domain that names a framework or a persistence technology can no longer be "
                        + "reasoned about, tested, or migrated without that technology present. Each "
                        + "category here is proven to fire by its negative-control fixture.")
                .isEmpty();
    }

    @Test
    void everyDomainCategoryActuallyFires() throws IOException {
        // The control for the test above. Without this, a category whose regex silently stopped matching
        // would report a clean tree — the same output as a genuinely clean one.
        Path fixtures = ArchitectureScanner.negativeFixtures();
        assertThat(Files.isDirectory(fixtures)).as("fixtures at %s", fixtures).isTrue();

        Map<String, List<Violation>> found = ArchitectureScanner.byCategory(
                ArchitectureScanner.scanTree(List.of(fixtures), DOMAIN_CATEGORIES));

        for (Category category : DOMAIN_CATEGORIES) {
            assertThat(found.get(category.name()))
                    .as("category %s matched nothing in the negative fixtures, so the guard above proves "
                            + "nothing about it. Reason it exists: %s", category.name(), category.reason())
                    .isNotNull()
                    .isNotEmpty();
        }
    }
}
