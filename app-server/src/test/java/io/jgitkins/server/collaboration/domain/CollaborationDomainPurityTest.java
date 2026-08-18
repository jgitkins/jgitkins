package io.jgitkins.server.collaboration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CollaborationDomainPurityTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "org.springframework",
            "org.mybatis",
            "org.eclipse.jgit",
            "jakarta.persistence",
            "io.jgitkins.server.identity.access.domain");

    @Test
    void collaborationDomainDoesNotImportExternalTechnologyOrIdentityDomain() throws IOException {
        Path domainSource = Path.of("src/main/java/io/jgitkins/server/collaboration/domain");

        try (Stream<Path> files = Files.walk(domainSource)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::assertNoForbiddenImport);
        }
    }

    private void assertNoForbiddenImport(Path source) {
        try {
            String content = Files.readString(source);
            FORBIDDEN_IMPORTS.forEach(forbidden -> assertThat(content)
                    .as("%s must not import %s", source, forbidden)
                    .doesNotContain(forbidden));
        } catch (IOException exception) {
            throw new AssertionError("Could not read domain source: " + source, exception);
        }
    }
}
