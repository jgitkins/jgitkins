package io.jgitkins.server.persistence.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.67 gate: every mapper interface is inside a scanned package, and every scanned package exists.
 *
 * <p>{@code @MapperScan} takes package names as strings. After the move, a stale entry names a package
 * that no longer exists — MyBatis registers no mapper beans for that context, and the application fails
 * at wiring time. A missing entry is the same failure for a context that was never listed. Neither is a
 * compile error.
 *
 * <p>The plan recorded ten entries across two files, because {@code MybatisConfig} in
 * {@code core-persistence} duplicated app-server's five. That duplicate has since been removed, so this
 * test asserts the count it actually finds and asserts the duplicate has not come back: two
 * {@code MapperScannerConfigurer}s over the same packages are harmless until one of those mappers is
 * injected into an explicitly declared bean, which is what every selector configuration now does.
 */
class MapperScanCoverageTest {

    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path SERVER_JAVA = Path.of("src/main/java");
    private static final Path SERVER_ROOT = SERVER_JAVA.resolve("io/jgitkins/server");

    private static final Pattern PACKAGE_LITERAL =
            Pattern.compile("\"(io\\.jgitkins\\.server\\.[A-Za-z0-9_.]+)\"");

    @Test
    void mapperScanBasePackagesCoverEveryMapperInterface() throws IOException {
        Set<String> scanned = scannedPackages();
        assertThat(scanned)
                .as("no @MapperScan base package was found; without one, MyBatis registers no mapper "
                        + "beans at all and the scan in this test is the thing that is broken")
                .isNotEmpty();

        List<String> unscanned = new ArrayList<>();
        List<String> mapperPackages = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SERVER_ROOT)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("Mapper.java"))
                    .sorted()
                    .toList()) {
                String source = Files.readString(file);
                // MapStruct domain mappers are Spring beans, not MyBatis mappers, and are not scanned.
                if (!source.contains("@org.apache.ibatis.annotations.Mapper")
                        && !source.contains("@Mapper\n")
                        && !source.contains("import org.apache.ibatis.annotations.Mapper;")) {
                    continue;
                }
                if (source.contains("org.mapstruct.Mapper")) {
                    continue;
                }
                String packageName = source.substring(source.indexOf("package ") + 8,
                        source.indexOf(';')).trim();
                mapperPackages.add(packageName);
                if (scanned.stream().noneMatch(packageName::startsWith)) {
                    unscanned.add(file + " is in " + packageName);
                }
            }
        }

        assertThat(mapperPackages)
                .as("no MyBatis mapper interface was discovered; the filter is wrong, because the tree "
                        + "still contains fourteen mapper XMLs bound to interfaces")
                .isNotEmpty();

        assertThat(unscanned)
                .as("these MyBatis mapper interfaces are outside every @MapperScan base package. They "
                        + "get no bean, and the failure arrives at wiring time — not at compile time.")
                .isEmpty();
    }

    @Test
    void everyScannedPackageStillExists() throws IOException {
        List<String> stale = new ArrayList<>();
        for (String scannedPackage : scannedPackages()) {
            Path asPath = SERVER_JAVA.resolve(scannedPackage.replace('.', '/'));
            if (!Files.isDirectory(asPath)) {
                stale.add(scannedPackage);
            }
        }
        assertThat(stale)
                .as("these @MapperScan entries name packages that do not exist. After a package move "
                        + "this is the residue: MyBatis registers nothing for that context and the "
                        + "application fails at wiring, with a message about a missing bean rather than "
                        + "about a stale annotation.")
                .isEmpty();
    }

    @Test
    void theCorePersistenceDuplicateMapperScanHasNotComeBack() throws IOException {
        Path mybatisConfig = REPOSITORY_ROOT.resolve(
                "core-persistence/src/main/java/io/jgitkins/core/persistence/MybatisConfig.java");
        assertThat(Files.exists(mybatisConfig))
                .as("MybatisConfig is shared infrastructure this task retains; it should still be here")
                .isTrue();

        String source = Files.readString(mybatisConfig);
        List<String> annotationLines = source.lines()
                .map(String::strip)
                .filter(line -> line.startsWith("@MapperScan"))
                .toList();

        assertThat(annotationLines)
                .as("core-persistence must not re-declare @MapperScan. A second MapperScannerConfigurer "
                        + "over the same packages is harmless only while every mapper is consumed by "
                        + "component-scanned classes; it becomes ConflictingBeanDefinitionException as "
                        + "soon as one is injected into an explicitly declared bean, which is what every "
                        + "persistence selector configuration now does. It also had a core module "
                        + "hard-coding io.jgitkins.server.* package names as annotation strings, which no "
                        + "import-based guard can see.")
                .isEmpty();
    }

    private static Set<String> scannedPackages() throws IOException {
        Set<String> scanned = new LinkedHashSet<>();
        List<Path> candidates = List.of(
                SERVER_ROOT.resolve("JGitkinsServerApplication.java"),
                REPOSITORY_ROOT.resolve(
                        "core-persistence/src/main/java/io/jgitkins/core/persistence/MybatisConfig.java"));

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }
            String source = Files.readString(candidate);
            int at = source.indexOf("@MapperScan");
            while (at >= 0) {
                int close = source.indexOf(')', at);
                if (close < 0) {
                    break;
                }
                Matcher matcher = PACKAGE_LITERAL.matcher(source.substring(at, close));
                while (matcher.find()) {
                    scanned.add(matcher.group(1));
                }
                at = source.indexOf("@MapperScan", close);
            }
        }
        return scanned;
    }
}
