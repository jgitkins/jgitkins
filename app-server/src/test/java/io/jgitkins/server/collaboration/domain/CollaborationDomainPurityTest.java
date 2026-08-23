package io.jgitkins.server.collaboration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CollaborationDomainPurityTest {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "io.jgitkins.server.collaboration.application.",
            "io.jgitkins.server.collaboration.adapter.",
            "io.jgitkins.server.collaboration.infrastructure.",
            "io.jsonwebtoken.",
            "jakarta.servlet.",
            "javax.servlet.",
            "org.springframework.",
            "org.mybatis.",
            "jakarta.persistence.",
            "javax.persistence.",
            "org.eclipse.jgit.");
    private static final Pattern SECURITY_IMPORT = Pattern.compile(
            "io\\.jgitkins\\.server\\..*\\.security\\.");

    @Test
    void collaborationDomainDoesNotImportForbiddenDependencies() throws IOException {
        Path domainSource = Path.of("src/main/java/io/jgitkins/server/collaboration/domain");

        try (Stream<Path> files = Files.walk(domainSource)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::assertNoForbiddenImport);
        }
    }

    @Test
    void forbiddenImportPolicyRecognizesEveryCategoryAndConcreteSecurityImport() {
        assertThat(SECURITY_IMPORT.matcher(
                "import io.jgitkins.server.identity.access.adapter.in.security.JwtAuthenticationFilter;")
                .find()).isTrue();

        FORBIDDEN_IMPORT_PREFIXES.forEach(prefix -> assertThat(isForbiddenImport(
                "import " + prefix + "SyntheticDependency;")).as("synthetic forbidden category %s", prefix)
                .isTrue());
        assertThat(isForbiddenImport(
                "import io.jgitkins.server.identity.access.adapter.in.security.JwtAuthenticationFilter;"))
                .as("synthetic security category")
                .isTrue();
    }

    private void assertNoForbiddenImport(Path source) {
        try {
            Files.readAllLines(source).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .forEach(importLine -> assertThat(isForbiddenImport(importLine))
                            .as("%s must not contain forbidden import %s", source, importLine)
                            .isFalse());
        } catch (IOException exception) {
            throw new AssertionError("Could not read domain source: " + source, exception);
        }
    }

    private static boolean isForbiddenImport(String importLine) {
        String importedType = importLine.substring("import ".length());
        return FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(importedType::startsWith)
                || SECURITY_IMPORT.matcher(importedType).find();
    }
}
