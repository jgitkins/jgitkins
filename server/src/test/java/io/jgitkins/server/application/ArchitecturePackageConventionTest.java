package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.application.service.AdminUserService;
import io.jgitkins.server.application.service.BranchService;
import io.jgitkins.server.application.service.CommitService;
import io.jgitkins.server.application.service.MergeService;
import io.jgitkins.server.application.service.OAuthLoginService;
import io.jgitkins.server.application.service.OrganizeMemberService;
import io.jgitkins.server.application.service.OrganizeService;
import io.jgitkins.server.application.service.PublicUserQueryService;
import io.jgitkins.server.application.service.PushEventHandleService;
import io.jgitkins.server.application.service.RepositoryFileService;
import io.jgitkins.server.application.service.RepositoryLifecycleService;
import io.jgitkins.server.application.service.RepositoryMemberService;
import io.jgitkins.server.application.service.RepositoryOverviewService;
import io.jgitkins.server.application.service.UserCredentialService;
import io.jgitkins.server.application.service.UserProfileService;
import io.jgitkins.server.application.support.RepositoryLookupService;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.application.support.UserService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class ArchitecturePackageConventionTest {

    private static final String SERVICE_PACKAGE = "io.jgitkins.server.application.service";

    @Test
    void applicationServices_resideInUnifiedServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                AdminUserService.class,
                BranchService.class,
                CommitService.class,
                MergeService.class,
                OAuthLoginService.class,
                OrganizeMemberService.class,
                OrganizeService.class,
                PublicUserQueryService.class,
                PushEventHandleService.class,
                RepositoryFileService.class,
                RepositoryLifecycleService.class,
                RepositoryMemberService.class,
                RepositoryOverviewService.class,
                UserCredentialService.class,
                UserProfileService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void supportCollaborators_useComponentInsteadOfService() {
        List<Class<?>> supportClasses = List.of(
                CloneUrlBuilder.class,
                UserService.class,
                RepositoryLookupService.class,
                RepositoryNamespaceResolver.class,
                RunnerRuntimeConfigProvider.class);

        supportClasses.forEach(supportClass -> {
            assertTrue(supportClass.isAnnotationPresent(Component.class));
            assertFalse(supportClass.isAnnotationPresent(Service.class));
        });
    }

    @Test
    void applicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path applicationRoot = Path.of("src/main/java/io/jgitkins/server/application");

        try (Stream<Path> files = Files.walk(applicationRoot)) {
            List<Path> javaFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);
                assertFalse(source.lines().anyMatch(line -> line.startsWith("import io.jgitkins.server.infrastructure.")),
                        () -> "Application source must not import infrastructure package: " + javaFile);
            }
        }
    }
}
