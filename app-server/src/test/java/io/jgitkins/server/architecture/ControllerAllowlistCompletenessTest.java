package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.architecture.ArchitectureScanner.Violation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link ControllerInventory} must name every controller that exists on disk.
 *
 * <p>This guard replaces a hand-maintained {@code List.of} that nothing checked. The evidence it was
 * never read: {@code MergeController} appeared twice in it, short form and fully qualified, so
 * nineteen entries named eighteen classes and no test noticed. A controller added without touching
 * the list was simply never checked, and the suite stayed green -- which is the failure mode this
 * whole class exists to end.
 *
 * <p>Asserted against the source tree rather than a classpath scan so the failure names the missing
 * class, following {@code ErrorStatusMappingCompletenessTest}.
 *
 * <p><strong>Compared by fully qualified name, deliberately.</strong> The obvious shortcut is to
 * compare simple names, which is what the mapper guard does. This repository cannot afford that: two
 * contexts already declare types with the same simple name (task 2.132), so a {@code Set} of simple
 * names would silently fold two controllers into one and the sizes would agree while one of them had
 * never been checked.
 */
class ControllerAllowlistCompletenessTest {

    /**
     * Scanned once. {@code scanTree} walks every {@code .java} file under main -- 800-odd of them --
     * and re-walking per test method would multiply that by the number of tests here for no new
     * information.
     */
    private static List<Violation> controllerDeclarations;

    @BeforeAll
    static void scanTheSourceTreeOnce() throws IOException {
        controllerDeclarations = ArchitectureScanner.scanTree(
                List.of(ArchitectureScanner.mainRoot()), List.of(ArchitectureScanner.CONTROLLER));
    }

    @Test
    void theInventoryNamesEveryControllerOnDisk() {
        Set<String> onDisk = fullyQualifiedNamesOf(controllerDeclarations);

        assertThat(onDisk)
                .as("the scan found no controllers at all, which means it did not find the source "
                        + "tree. scanTree() skips a root that is not a directory without complaining, "
                        + "so an empty result reads as agreement rather than as a broken scan.")
                .isNotEmpty();

        Set<String> configured = new TreeSet<>();
        ControllerInventory.ALL.forEach(controller -> configured.add(controller.getName()));

        assertThat(configured)
                .as("ControllerInventory.ALL must name every controller that exists. A controller "
                        + "added to production and forgotten here is checked by nothing, and every "
                        + "convention test that iterates the inventory silently skips it.")
                .isEqualTo(onDisk);
    }

    @Test
    void theControllerCategoryDoesNotMatchControllerAdvice() throws IOException {
        Path fixture = ArchitectureScanner.negativeFixtures()
                .resolve("controller-advice-not-a-controller.java");

        List<Violation> found =
                ArchitectureScanner.scan(fixture, List.of(ArchitectureScanner.CONTROLLER));

        assertThat(found)
                .as("the scanner matches with find(), so an unanchored @RestController pattern also "
                        + "matches inside @RestControllerAdvice. GlobalExceptionHandler carries that "
                        + "annotation; if the category matched it, the disk set would never equal the "
                        + "inventory and the cheapest way to green would be to list the advice class "
                        + "as a controller.")
                .isEmpty();
    }

    @Test
    void theControllerCategoryActuallyFires() throws IOException {
        Path fixture = ArchitectureScanner.negativeFixtures()
                .resolve("controller-declaration.java");

        List<Violation> found =
                ArchitectureScanner.scan(fixture, List.of(ArchitectureScanner.CONTROLLER));

        assertThat(found)
                .as("if the category matches nothing, the disk set is empty, isNotEmpty above is the "
                        + "only thing standing between this suite and a vacuous pass, and both "
                        + "annotation spellings need to be proven -- not just the one in the tree today")
                .hasSize(2);
    }

    private static Set<String> fullyQualifiedNamesOf(List<Violation> violations) {
        Path root = ArchitectureScanner.mainRoot();
        Set<String> names = new TreeSet<>();
        for (Violation violation : violations) {
            String relative = root.relativize(violation.file()).toString();
            names.add("io.jgitkins.server." + relative.replace(".java", "").replace('/', '.'));
        }
        return names;
    }
}
