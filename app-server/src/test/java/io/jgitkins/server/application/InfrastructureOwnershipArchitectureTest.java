package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Where an outbound adapter may live, and which way a dependency may point.
 *
 * <p>Three of the original nine assertions were removed because they were a second copy of the file
 * tree rather than a rule about it:
 *
 * <ul>
 *   <li>{@code retainedTechnicalAssetsMatchExactCurrentAppServerInventory} pinned the exact set of
 *       model, mapper and MBG paths. That was deliberate for task 2.67 -- the plan called it "the
 *       landmine", a gate the move could not pass without editing. The move has landed, so what is
 *       left is a list that has to be edited every time a file is added or renamed, and it was
 *       edited by hand twice in one day.
 *   <li>{@code commonForeignContextImportsAreExactlyAllowlisted} carried twenty-one fully qualified
 *       type names, which is where most of this file's rename cost sat. The rule it expressed --
 *       common may only reach into a context through a named exception -- is worth having again, but
 *       not as an FQN list; it belongs on a form that survives a package move.
 *   <li>{@code foreignImportPolicyRejectsEveryBoundedContext} went with it: it was the negative
 *       control proving that allowlist's predicate fired, and a control for a deleted policy tests
 *       nothing.
 * </ul>
 *
 * <p>What stays is placement keyed off ten short adapter roots and the implements clause, plus the
 * two negative controls that prove those policies actually fire. This repo has three recorded cases
 * of a guard passing because it examined nothing, so the controls are the load-bearing half.
 *
 * <p>The eight import-direction rules that used to live in the deleted
 * {@code ArchitecturePackageConventionTest} are now in
 * {@code io.jgitkins.server.architecture.LayerDependencyDirectionTest}.
 */
public class InfrastructureOwnershipArchitectureTest {
    private static final Path PROJECT_ROOT = Files.exists(Path.of("app-server/src/main/java"))
            ? Path.of("app-server") : Path.of(".");
    private static final Path JAVA_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Path SERVER_ROOT = JAVA_ROOT.resolve("io/jgitkins/server");
    private static final Path TEST_JAVA_ROOT = PROJECT_ROOT.resolve("src/test/java");
    private static final Path TEST_SERVER_ROOT = TEST_JAVA_ROOT.resolve("io/jgitkins/server");

    // Every bounded context owns its inbound/outbound adapter tree. The only
    // retained common adapter is the documented push-event bridge below.
    private static final Set<String> OWNED_ADAPTER_ROOTS = Set.of(
            "change/review/adapter/in", "change/review/adapter/out",
            "collaboration/adapter/in", "collaboration/adapter/out",
            "execution/adapter/in", "execution/adapter/out",
            "identity/access/adapter/in", "identity/access/adapter/out",
            "repository/adapter/in", "repository/adapter/out");
    private static final Set<String> DOCUMENTED_COMMON_ADAPTERS = Set.of(
            "common/infrastructure/adapter/PushEventRequestAdapter.java");

    @Test
    void applicationSourcesDoNotDependOnInboundAdapters() throws IOException {
        Path applicationRoot = SERVER_ROOT.resolve("repository/application");
        Set<String> violations = new HashSet<>();
        try (Stream<Path> paths = Files.walk(applicationRoot)) {
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            Files.readAllLines(path).stream()
                                    .filter(line -> line.trim().startsWith("import "))
                                    .filter(line -> line.contains("io.jgitkins.server.repository.adapter.in."))
                                    .forEach(line -> violations.add(path + " -> " + line.trim()));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }
        assertTrue(violations.isEmpty(), () -> "Application imports inbound adapters: " + violations);
    }

    @Test
    void handwrittenAdapterPolicyRejectsMisplacedAndAllowsDocumentedCommonAdapters() {
        assertTrue(!isAllowedInfrastructureAdapterPath("repository/infrastructure/adapter/UnexpectedAdapter.java"));
        assertTrue(!isAllowedInfrastructureAdapterPath("repository/application/adapter/UnexpectedAdapter.java"));
        assertTrue(!isAllowedInfrastructureAdapterPath("repository/infrastructure/foo/UnexpectedAdapter.java"));
        assertTrue(!isAllowedInfrastructureAdapterPath("repository/adapter/outside/UnexpectedAdapter.java"));
        assertTrue(!isAllowedInfrastructureAdapterPath("/repository/adapter/out/UnexpectedAdapter.java.bak"));
        assertTrue(isAllowedInfrastructureAdapterPath("./repository/adapter/out/acl/RepositoryActorAclAdapter.java"));
        assertTrue(isAllowedInfrastructureAdapterPath("common/infrastructure/adapter/PushEventRequestAdapter.java"));
        assertTrue(!isAllowedInfrastructureAdapterPath("common/infrastructure/adapter/security/JwtService.java"));
    }

    @Test
    void handwrittenAdaptersRemainOnlyInOwnedAdapterRootsAcrossMainAndTestSources() throws IOException {
        Set<String> violations = new HashSet<>();
        for (Path sourceRoot : List.of(SERVER_ROOT, TEST_SERVER_ROOT)) {
            if (!Files.exists(sourceRoot)) continue;
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith("Adapter.java"))
                        .map(path -> sourceRoot.relativize(path).toString().replace('\\', '/'))
                        .filter(path -> !isAllowedInfrastructureAdapterPath(path))
                        .forEach(violations::add);
            }
        }
        assertTrue(violations.isEmpty(), () -> "Handwritten adapters outside owned adapter roots: " + violations);
    }

    @Test
    void legacyHandwrittenAdapterRootsAreAbsent() {
        List<Path> forbidden = List.of(
                SERVER_ROOT.resolve("common/factory"),
                SERVER_ROOT.resolve("common/infrastructure/config/security/auth/PatAuthenticationProvider.java")
        );
        assertTrue(forbidden.stream().noneMatch(Files::exists), () -> "Legacy roots remain: " + forbidden);
    }

    // The rule above keys off the file NAME, which is why it never saw
    // collaboration/adapter/out/event/CollaborationSpringDomainEventPublisher.java: an outbound
    // adapter whose name does not end in "Adapter". Naming is the weaker half of the boundary --
    // what actually makes a class an outbound adapter is that it implements one of the owning
    // context's out-ports. This scan keys off that instead, so a ...Publisher, ...Gateway or
    // ...Client cannot land outside an owned adapter root just by avoiding the suffix.
    //
    // Comments AND string literals are stripped before matching. A scanner that matches raw source
    // text flags the javadoc explaining a rule as a violation of it, which makes deleting the
    // explanation the cheapest way back to green; that has happened three times in this repo. The
    // string-literal half is the same lesson one level deeper -- without it this very file fails
    // its own rule, because the fixtures below contain the word "implements" next to a port name.
    private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\".*?\"\"\"", Pattern.DOTALL);
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern OUT_PORT_IMPORT =
            Pattern.compile("import\\s+[\\w.]+\\.application\\.port\\.out\\.(\\w+)\\s*;");
    private static final Pattern IMPLEMENTS_CLAUSE =
            Pattern.compile("\\b(?:class|record|enum)\\s+\\w+[^{;]*?\\bimplements\\b([^{]+)\\{", Pattern.DOTALL);

    @Test
    void outboundPortImplementationsRemainOnlyInOwnedAdapterRootsAcrossMainAndTestSources() throws IOException {
        Set<String> violations = new HashSet<>();
        for (Path sourceRoot : List.of(SERVER_ROOT, TEST_SERVER_ROOT)) {
            if (!Files.exists(sourceRoot)) continue;
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                Set<String> ports = implementedOutboundPorts(Files.readString(path));
                                if (ports.isEmpty()) return;
                                String relativePath = sourceRoot.relativize(path).toString().replace('\\', '/');
                                if (!isAllowedInfrastructureAdapterPath(relativePath)) {
                                    violations.add(relativePath + " implements " + new TreeSet<>(ports));
                                }
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
        }
        assertTrue(violations.isEmpty(), () -> "Outbound port implementations outside owned adapter roots: "
                + violations + ". An out-port implementation is an outbound adapter regardless of what the "
                + "file is called; move it under <context>/adapter/out/, or add it to DOCUMENTED_COMMON_ADAPTERS "
                + "with the reason it is an exception.");
    }

    @Test
    void outboundPortDetectionReadsTheImplementsClauseAndIgnoresCommentsAndLiterals() {
        String realAdapter = """
                package io.jgitkins.server.collaboration.adapter.out.event;
                import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
                public class SomePublisher
                        implements DomainEventPublisher {
                }
                """;
        assertEquals(Set.of("DomainEventPublisher"), implementedOutboundPorts(realAdapter));

        // Injecting a port is not implementing one. Application services must not be flagged.
        String consumer = """
                package io.jgitkins.server.collaboration.application.service;
                import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
                public class OrganizeService implements OrganizeCreationUseCase {
                    private final DomainEventPublisher domainEventPublisher;
                }
                """;
        assertTrue(implementedOutboundPorts(consumer).isEmpty());

        String commentedOut = """
                package io.jgitkins.server.common.infrastructure.event;
                import io.jgitkins.server.shared.application.port.out.SomePort;
                /* class Old implements SomePort { } */
                // class Older implements SomePort { }
                public class Notes {
                }
                """;
        assertTrue(implementedOutboundPorts(commentedOut).isEmpty());

        // A scanner that skips this case flags itself: its own fixtures are source text about
        // adapters. Written as concatenation rather than a text block so the string-literal
        // stripping is what has to do the work here, not the text-block stripping.
        String insideAStringLiteral =
                "package io.jgitkins.server.application;\n"
                        + "import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;\n"
                        + "public class SomeScanner {\n"
                        + "    private final String fixture = \"class X implements DomainEventPublisher {\";\n"
                        + "}\n";
        assertTrue(implementedOutboundPorts(insideAStringLiteral).isEmpty());
    }

    static Set<String> implementedOutboundPorts(String source) {
        // Order matters: text blocks may contain "//" and "/*", so they go first.
        String code = TEXT_BLOCK.matcher(source).replaceAll("\"\"");
        code = BLOCK_COMMENT.matcher(code).replaceAll("");
        code = LINE_COMMENT.matcher(code).replaceAll("");
        code = STRING_LITERAL.matcher(code).replaceAll("\"\"");
        Set<String> importedPorts = new HashSet<>();
        Matcher imports = OUT_PORT_IMPORT.matcher(code);
        while (imports.find()) {
            importedPorts.add(imports.group(1));
        }
        if (importedPorts.isEmpty()) return Set.of();

        Set<String> implemented = new HashSet<>();
        Matcher declarations = IMPLEMENTS_CLAUSE.matcher(code);
        while (declarations.find()) {
            for (String type : declarations.group(1).split(",")) {
                implemented.add(type.replaceAll("<.*", "").trim());
            }
        }
        implemented.retainAll(importedPorts);
        return implemented;
    }

    private boolean isAllowedInfrastructureAdapterPath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!normalized.endsWith(".java")) return false;
        final String normalizedPath = normalized;
        if (DOCUMENTED_COMMON_ADAPTERS.contains(normalizedPath)) return true;
        return OWNED_ADAPTER_ROOTS.stream()
                .anyMatch(root -> normalizedPath.startsWith(root + "/"));
    }

}
