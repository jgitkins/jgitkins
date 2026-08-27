package io.jgitkins.server.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.77 closure audit: every persistence capability has exactly one selector, and exactly one
 * composition root owns it.
 *
 * <p><strong>This test discovers the selectors from the source, it does not carry a list.</strong> That
 * is the whole point. Task 2.77's own plan enumerated six selectors when seven exist — it was written
 * before task 2.73 added the execution-job write selector — and it named the collaboration capability
 * {@code collaboration-organize-member-reference} when the code says
 * {@code organize-organize-member-reference}. A test that copied either list would have passed while
 * being wrong about the thing it claims to audit. A discovered list cannot go stale.
 *
 * <p>What "exactly one owner" protects: two configurations declaring the same capability slug would both
 * define a bean for the same port. One would win by bean-definition order, and the property that appears
 * to control the choice would control only one of them. The failure is a deployment where the selector
 * reads {@code mybatis} and half the adapters are JPA.
 */
class FinalSelectorClosureTest {

    private static final Path MAIN = Paths.get("src/main/java/io/jgitkins/server");

    private static final Pattern CAPABILITY = Pattern.compile(
            "CAPABILITY_SLUG\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern MODULE = Pattern.compile(
            "MODULE_SLUG\\s*=\\s*\"([^\"]+)\"");

    @Test
    void allSelectorsResolveToOneOwner() throws IOException {
        Map<String, List<Path>> ownersByCapability = new LinkedHashMap<>();
        Map<String, String> moduleByCapability = new LinkedHashMap<>();

        for (Path file : selectorConfigurations()) {
            String source = Files.readString(file);
            Matcher capability = CAPABILITY.matcher(source);
            Matcher module = MODULE.matcher(source);
            assertThat(capability.find())
                    .as(file + " reads a persistence selector property but declares no CAPABILITY_SLUG; "
                            + "an undiscoverable selector cannot be audited")
                    .isTrue();
            assertThat(module.find()).as(file + " declares no MODULE_SLUG").isTrue();

            ownersByCapability.computeIfAbsent(capability.group(1), key -> new ArrayList<>()).add(file);
            moduleByCapability.put(capability.group(1), module.group(1));

            assertThat(capability.find())
                    .as(file + " declares more than one CAPABILITY_SLUG; one configuration must own "
                            + "exactly one capability or the property namespace stops being a key")
                    .isFalse();
        }

        assertThat(ownersByCapability)
                .as("no selector configuration was discovered at all — the scan is broken, not the code")
                .isNotEmpty();

        ownersByCapability.forEach((capability, owners) ->
                assertThat(owners)
                        .as("capability '%s' is declared by %s. Two configurations would both define a "
                                + "bean for the same port; one wins by definition order, and the property "
                                + "would control only half the adapters", capability, owners)
                        .hasSize(1));

        // Every discovered capability must produce a well-formed property in the frozen namespace, and
        // must default to MyBatis when unset. The default is the rollback of last resort: an operator who
        // deletes the property has to land somewhere safe.
        moduleByCapability.forEach((capability, module) -> {
            String property = PersistenceImplementationSelector.propertyName(module, capability);
            assertThat(property)
                    .as("the property namespace is frozen; a selector outside it is invisible to the "
                            + "rollback commands recorded in every task's plan")
                    .startsWith("jgitkins.persistence.")
                    .endsWith(".implementation");
            assertThat(PersistenceImplementationSelector.resolve(property, null))
                    .as("capability '%s' must default to MyBatis when the property is absent", capability)
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(PersistenceImplementationSelector.resolve(property, "  "))
                    .as("and when it is blank, which is what an empty environment variable looks like")
                    .isEqualTo(PersistenceImplementation.MYBATIS);
        });
    }

    @Test
    void everySelectorAdmitsExactlyTheTwoDocumentedValues() {
        assertThat(PersistenceImplementation.values())
                .as("the manifest and every rollback command in tasks 2.68-2.76 are written against "
                        + "mybatis|jpa; a third value would make all of them incomplete")
                .hasSize(2);
        assertThat(Stream.of(PersistenceImplementation.values())
                .map(PersistenceImplementation::wireValue)
                .toList())
                .containsExactlyInAnyOrder("mybatis", "jpa");
    }

    private static List<Path> selectorConfigurations() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith("SelectorConfiguration.java"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }
}
