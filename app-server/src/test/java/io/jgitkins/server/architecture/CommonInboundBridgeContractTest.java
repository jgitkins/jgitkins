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
 * Transport shapes stop at the inbound adapter.
 *
 * <p>Tasks 2.63 through 2.65 pushed actor derivation out to the adapters. The mirror-image risk is the
 * transport following it inward: a {@code Principal}, an {@code HttpServletRequest} or a
 * {@code SecurityContextHolder} appearing in an application service. Each one individually looks like a
 * convenience; together they undo the whole point, because a use case that can reach the request can
 * decide who the caller is again.
 *
 * <p>{@code MultipartFile} is the deliberate exception, and it is allowlisted by exact file rather than
 * by package. {@code FileUploadUseCase} takes one because streaming an upload through a copy would mean
 * buffering the whole file to authorize it. Naming the file keeps that a decision rather than a pattern.
 */
class CommonInboundBridgeContractTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "repository", "execution", "identity/access", "change/review");

    private static final List<Category> TRANSPORT_CATEGORIES = List.of(
            ArchitectureScanner.FORBIDDEN_SERVLET,
            ArchitectureScanner.FORBIDDEN_PRINCIPAL,
            ArchitectureScanner.FORBIDDEN_SECURITY_CONTEXT,
            ArchitectureScanner.FORBIDDEN_JWT,
            ArchitectureScanner.FORBIDDEN_MULTIPART);

    /** Files permitted to name a transport shape past the adapter, each with its reason. */
    private static final Map<String, String> ALLOWLIST = Map.of(
            "FileUploadUseCase.java",
            "the upload port takes a MultipartFile because copying the stream to hide the type would "
                    + "mean buffering the whole file just to authorize it",
            "RepositoryFileService.java",
            "the upload use case's implementation, for the same reason as its port",
            "CommitFilePreparer.java",
            "reads the multipart into commit files; the conversion has to happen somewhere and this is "
                    + "the last point before the git port");

    @Test
    void forbidsTransportLeakBeyondAdapter() throws IOException {
        List<Path> roots = new ArrayList<>();
        for (String context : CONTEXTS) {
            roots.add(ArchitectureScanner.mainRoot().resolve(context).resolve("application"));
            roots.add(ArchitectureScanner.mainRoot().resolve(context).resolve("domain"));
        }

        assertThat(roots.stream().filter(Files::isDirectory).toList()).isNotEmpty();

        List<Violation> violations = ArchitectureScanner.scanTree(roots, TRANSPORT_CATEGORIES).stream()
                .filter(v -> !ALLOWLIST.containsKey(v.file().getFileName().toString()))
                .toList();

        assertThat(violations)
                .as("transport shapes must stop at the inbound adapter. A use case that can reach the "
                        + "request can decide who the caller is again, which is exactly what tasks "
                        + "2.63-2.65 removed. Allowlisted exceptions: %s", ALLOWLIST.keySet())
                .isEmpty();
    }

    @Test
    void everyTransportCategoryActuallyFires() throws IOException {
        Map<String, List<Violation>> found = ArchitectureScanner.byCategory(
                ArchitectureScanner.scanTree(
                        List.of(ArchitectureScanner.negativeFixtures()), TRANSPORT_CATEGORIES));

        for (Category category : TRANSPORT_CATEGORIES) {
            assertThat(found.get(category.name()))
                    .as("category %s matched nothing in the negative fixtures. Reason it exists: %s",
                            category.name(), category.reason())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Test
    void theAllowlistIsNotStale() {
        // An allowlist entry for a file that no longer exists reads as a live exception and is not one.
        // It also hides the next real violation if that file name is ever reused.
        List<String> missing = new ArrayList<>();
        for (String fileName : ALLOWLIST.keySet()) {
            boolean found = false;
            try (var files = Files.walk(ArchitectureScanner.mainRoot())) {
                found = files.anyMatch(p -> p.getFileName().toString().equals(fileName));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            if (!found) {
                missing.add(fileName);
            }
        }
        assertThat(missing)
                .as("these allowlist entries name files that no longer exist; remove them, or the "
                        + "exception silently applies to whatever takes that name next")
                .isEmpty();
    }
}
