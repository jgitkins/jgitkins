package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class InfrastructureOwnershipArchitectureTest {
    private static final Path PROJECT_ROOT = Files.exists(Path.of("app-server/src/main/java"))
            ? Path.of("app-server") : Path.of(".");
    private static final Path JAVA_ROOT = PROJECT_ROOT.resolve("src/main/java");
    private static final Path SERVER_ROOT = JAVA_ROOT.resolve("io/jgitkins/server");
    private static final Path TEST_JAVA_ROOT = PROJECT_ROOT.resolve("src/test/java");
    private static final Path TEST_SERVER_ROOT = TEST_JAVA_ROOT.resolve("io/jgitkins/server");
    private static final Path RESOURCE_ROOT = PROJECT_ROOT.resolve("src/main/resources");

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

    private static final Set<String> EXPECTED_MODELS = Set.of(
            "io/jgitkins/server/change/review/adapter/out/persistence/model/PullRequestEntity.java",
            "io/jgitkins/server/change/review/adapter/out/persistence/model/PullRequestEntityCondition.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/model/OrganizeEntity.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/model/OrganizeEntityCondition.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/model/OrganizeMemberEntity.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/model/OrganizeMemberEntityCondition.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/DispatchableJobRow.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/JobEntity.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/JobEntityCondition.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/JobHistoryEntity.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/JobHistoryEntityCondition.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/RunnerAssignmentEntity.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/RunnerAssignmentEntityCondition.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/RunnerEntity.java",
            "io/jgitkins/server/execution/adapter/out/persistence/model/RunnerEntityCondition.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserCredentialsEntity.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserCredentialsEntityCondition.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserEntity.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserEntityCondition.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserIdentitiesEntity.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/model/UserIdentitiesEntityCondition.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/BranchEntity.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/BranchEntityCondition.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/RepositoryEntity.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/RepositoryEntityCondition.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/RepositoryMemberEntity.java",
            "io/jgitkins/server/repository/adapter/out/persistence/model/RepositoryMemberEntityCondition.java"
    );
    private static final Set<String> EXPECTED_PERSISTENCE_MAPPER_INTERFACES = Set.of(
            "io/jgitkins/server/change/review/adapter/out/persistence/mapper/PullRequestEntityMbgMapper.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/mapper/OrganizeEntityMbgMapper.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/mapper/OrganizeMemberEntityMbgMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/mapper/JobDispatchQueryMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/mapper/JobEntityMbgMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/mapper/JobHistoryEntityMbgMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/mapper/RunnerAssignmentEntityMbgMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/mapper/RunnerEntityMbgMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/mapper/UserCredentialsEntityMbgMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/mapper/UserEntityMbgMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/mapper/UserIdentitiesEntityMbgMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/mapper/BranchEntityMbgMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/mapper/RepositoryEntityMbgMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/mapper/RepositoryMemberEntityMbgMapper.java"
    );
    private static final Set<String> EXPECTED_DOMAIN_MAPPER_SUPPORT = Set.of(
            "io/jgitkins/server/change/review/adapter/out/persistence/support/PullRequestDomainMapper.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/support/OrganizeDomainMapper.java",
            "io/jgitkins/server/collaboration/adapter/out/persistence/support/OrganizeMemberDomainMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/support/JobDomainMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/support/RunnerAssignmentDomainMapper.java",
            "io/jgitkins/server/execution/adapter/out/persistence/support/RunnerDomainMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/support/UserCredentialDomainMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/support/UserDomainMapper.java",
            "io/jgitkins/server/identity/access/adapter/out/persistence/support/UserIdentityDomainMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/support/BranchDomainMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/support/RepositoryDomainMapper.java",
            "io/jgitkins/server/repository/adapter/out/persistence/support/RepositoryMemberDomainMapper.java"
    );
    private static final Set<String> EXPECTED_MBG = Set.of(
            "mapper/mbg/BranchEntityMbgMapper.xml",
            "mapper/mbg/JobEntityMbgMapper.xml",
            "mapper/mbg/JobHistoryEntityMbgMapper.xml",
            "mapper/mbg/OrganizeEntityMbgMapper.xml",
            "mapper/mbg/OrganizeMemberEntityMbgMapper.xml",
            "mapper/mbg/PullRequestEntityMbgMapper.xml",
            "mapper/mbg/RepositoryEntityMbgMapper.xml",
            "mapper/mbg/RepositoryMemberEntityMbgMapper.xml",
            "mapper/mbg/RunnerAssignmentEntityMbgMapper.xml",
            "mapper/mbg/RunnerEntityMbgMapper.xml",
            "mapper/mbg/UserCredentialsEntityMbgMapper.xml",
            "mapper/mbg/UserEntityMbgMapper.xml",
            "mapper/mbg/UserIdentitiesEntityMbgMapper.xml"
    );
    private static final Set<String> EXPECTED_CUSTOM = Set.of(
            "mapper/custom/JobDispatchQueryMapper.xml"
    );
    private static final Map<String, Set<String>> ALLOWED_COMMON_IMPORTS = Map.of(
            "common/infrastructure/config/security/handler/OAuth2LoginSuccessHandler.java", Set.of(
                    "io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand",
                    "io.jgitkins.server.identity.access.application.dto.result.OAuthLoginResult",
                    "io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase"),
            "common/infrastructure/config/security/SecurityConfig.java", Set.of(
                    "io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase",
                    "io.jgitkins.server.identity.access.adapter.in.security.JwtAuthenticationFilter",
                    "io.jgitkins.server.identity.access.application.service.JwtAuthService"),
            "common/infrastructure/config/git/GitSmartHttpAuthorizer.java", Set.of(
                    "io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase"),
            "common/infrastructure/config/git/hook/push/PushHook.java", Set.of(
                    "io.jgitkins.server.execution.application.port.in.PushEventHandleUseCase"),
            "common/presentation/advice/GlobalExceptionHandlerTest.java", Set.of(
                    "io.jgitkins.server.repository.application.exception.RepositoryNotFoundException")
    );

    // Task 2.67 relocated all three sets from <context>/infrastructure/** into the owning context's
    // outbound persistence adapter. This test is the gate the plan called "the landmine": it pins the
    // exact inventory by path, so the move could not land without editing it, and editing it is how the
    // new boundary becomes the asserted one rather than an aspiration. The old leaves are now asserted
    // empty by PersistenceModelPlacementArchitectureTest.
    @Test
    void retainedTechnicalAssetsMatchExactCurrentAppServerInventory() throws IOException {
        assertEquals(EXPECTED_MODELS, pathsUnder("io/jgitkins/server", "adapter/out/persistence/model", ".java"));
        assertEquals(EXPECTED_PERSISTENCE_MAPPER_INTERFACES, pathsUnder("io/jgitkins/server", "adapter/out/persistence/mapper", ".java"));
        assertEquals(EXPECTED_DOMAIN_MAPPER_SUPPORT, pathsUnder("io/jgitkins/server", "adapter/out/persistence/support", ".java"));
        assertEquals(EXPECTED_MBG, pathsUnder("", "", ".xml", RESOURCE_ROOT.resolve("mapper/mbg")));
        assertEquals(EXPECTED_CUSTOM, pathsUnder("", "", ".xml", RESOURCE_ROOT.resolve("mapper/custom")));
    }

    @Test
    void commonForeignContextImportsAreExactlyAllowlisted() throws IOException {
        Set<String> violations = new HashSet<>();
        for (Path sourceRoot : List.of(SERVER_ROOT, TEST_SERVER_ROOT)) {
            Path commonRoot = sourceRoot.resolve("common");
            if (!Files.exists(commonRoot)) continue;
            try (Stream<Path> paths = Files.walk(commonRoot)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                String relativePath = sourceRoot.relativize(path).toString().replace('\\', '/');
                                Set<String> allowedImports = ALLOWED_COMMON_IMPORTS.getOrDefault(relativePath, Set.of());
                                Files.readAllLines(path).stream()
                                        .map(String::trim)
                                        .filter(line -> line.startsWith("import io.jgitkins.server."))
                                        .filter(line -> isForeignContextImport(line))
                                        .map(line -> line.substring("import ".length(), line.length() - 1))
                                        .filter(importName -> !allowedImports.contains(importName))
                                        .forEach(importName -> violations.add(relativePath + " -> " + importName));
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
        }
        assertTrue(violations.isEmpty(), () -> "Unallowlisted common foreign imports: " + violations);
    }

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
    void foreignImportPolicyRejectsEveryBoundedContext() {
        assertTrue(isForeignContextImport("import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;"));
        assertTrue(isForeignContextImport("import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;"));
        assertTrue(isForeignContextImport("import io.jgitkins.server.collaboration.application.port.in.SomeUseCase;"));
        assertTrue(isForeignContextImport("import io.jgitkins.server.execution.application.port.in.SomeUseCase;"));
        assertTrue(isForeignContextImport("import io.jgitkins.server.change.review.application.port.in.SomeUseCase;"));
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

    private boolean isForeignContextImport(String line) {
        return line.contains(".identity.") || line.contains(".repository.")
                || line.contains(".collaboration.") || line.contains(".execution.")
                || line.contains(".change.review.");
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

    private Set<String> pathsUnder(String serverPrefix, String suffix, String extension) throws IOException {
        return pathsUnder(serverPrefix, suffix, extension, JAVA_ROOT);
    }

    private Set<String> pathsUnder(String ignoredPrefix, String suffix, String extension, Path root) throws IOException {
        if (!Files.exists(root)) return Set.of();
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(extension))
                    .filter(path -> suffix.isEmpty() || path.toString().replace('\\', '/').contains("/" + suffix + "/"))
                    .map(path -> root.equals(JAVA_ROOT) ? JAVA_ROOT.relativize(path).toString().replace('\\', '/')
                            : RESOURCE_ROOT.relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
    }
}
