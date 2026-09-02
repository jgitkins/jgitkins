package io.jgitkins.server.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RepositoryBoundedContextArchitectureTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java");
    private static final String REPOSITORY_ROOT = "io/jgitkins/server/repository/";
    private static final Set<String> DEFERRED_FILES = Set.of(
            "application/validate/RepositoryValidator.java",
            "application/service/RepositoryLoadService.java");

    @Test
    void repositoryDomain_hasNoForeignOrTechnologyImports() throws IOException {
        List<String> forbidden = List.of(
                "io.jgitkins.server.identity.access",
                "io.jgitkins.server.collaboration",
                "org.springframework",
                "org.mybatis",
                "jakarta.persistence",
                "java.sql",
                "org.eclipse.jgit",
                "docker",
                "runner");

        for (Path file : javaFiles(REPOSITORY_ROOT + "domain")) {
            String source = Files.readString(file);
            forbidden.forEach(importName -> assertFalse(
                    source.contains(importName),
                    () -> file + " imports forbidden dependency: " + importName));
        }
    }

    @Test
    void migratedRepositoryClasses_haveNoForeignApplicationBoundaryImports() throws IOException {
        List<String> forbidden = List.of(
                "identity.access.domain.vo.UserId",
                "collaboration.domain",
                "OrganizeMemberPersistencePort",
                "CurrentUserPort",
                "UserQueryPort",
                "OrganizeQueryPort",
                "OrganizeId",
                "getOrganizeId");

        for (Path file : javaFiles(REPOSITORY_ROOT + "application")) {
            String relative = file.toString().replace(JAVA_ROOT + "/" + REPOSITORY_ROOT, "");
            if (DEFERRED_FILES.contains(relative)
                    || relative.startsWith("support/RepositoryNamespaceResolver.java")
                    || relative.startsWith("support/RepositoryAccessibilityService.java")) {
                continue;
            }
            String source = Files.readString(file);
            forbidden.forEach(importName -> assertFalse(
                    source.contains(importName),
                    () -> file + " retains forbidden migrated dependency: " + importName));
        }
    }

    @Test
    void repositoryMemberOwnership_hasNoIdentityUserIdType() throws IOException {
        List<String> ownedPaths = List.of(
                "domain/model/RepositoryMember.java",
                "domain/vo/RepositoryMemberUserId.java",
                "application/port/out/RepositoryMemberPersistencePort.java",
                "application/service/RepositoryMemberService.java",
                "application/validate/RepositoryMemberValidator.java",
                "application/service/internal/membership/RepositoryMembershipFactory.java",
                // Was the MyBatis adapter plus its MapStruct mapper. Both are deleted; the JPA adapter
                // is the file that now holds repository's own member id at the persistence edge, and it
                // is the one that has to keep holding it rather than reaching for identity's UserId.
                "adapter/out/persistence/jpa/RepositoryMemberJpaPersistenceAdapter.java");

        for (String ownedPath : ownedPaths) {
            Path file = JAVA_ROOT.resolve(REPOSITORY_ROOT + ownedPath);
            assertTrue(Files.exists(file), "Missing repository-owned member file: " + file);
            assertFalse(Files.readString(file).contains("identity.access.domain.vo.UserId"),
                    () -> file + " imports identity UserId");
        }
    }

    private static List<Path> javaFiles(String relativeRoot) throws IOException {
        Path root = JAVA_ROOT.resolve(relativeRoot);
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
