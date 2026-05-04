package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.application.support.execution.ExecutionRequestService;
import io.jgitkins.server.shared.application.policy.EventPolicyResolver;
import io.jgitkins.server.application.service.AdminUserService;
import io.jgitkins.server.application.service.CommitService;
import io.jgitkins.server.application.service.MergeService;
import io.jgitkins.server.application.service.OAuthLoginService;
import io.jgitkins.server.application.service.OrganizeMemberService;
import io.jgitkins.server.application.service.OrganizeService;
import io.jgitkins.server.application.service.PublicUserQueryService;
import io.jgitkins.server.application.service.PushEventHandleService;
import io.jgitkins.server.application.service.RepositoryFileService;
import io.jgitkins.server.application.service.RepositoryOverviewService;
import io.jgitkins.server.application.service.UserCredentialService;
import io.jgitkins.server.application.service.UserProfileService;
import io.jgitkins.server.repository.application.service.BranchLoadService;
import io.jgitkins.server.repository.application.service.BranchManagementService;
import io.jgitkins.server.repository.application.service.RepositoryLoadService;
import io.jgitkins.server.repository.application.service.RepositoryManagementService;
import io.jgitkins.server.repository.application.service.RepositoryMemberService;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryAccessibilityService;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.application.support.UserService;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class ArchitecturePackageConventionTest {

    private static final String APPLICATION_SERVICE_PACKAGE = "io.jgitkins.server.application.service";
    private static final String REPOSITORY_SERVICE_PACKAGE = "io.jgitkins.server.repository.application.service";

    @Test
    void applicationServices_resideInUnifiedServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                AdminUserService.class,
                CommitService.class,
                MergeService.class,
                OAuthLoginService.class,
                OrganizeMemberService.class,
                OrganizeService.class,
                PublicUserQueryService.class,
                PushEventHandleService.class,
                RepositoryFileService.class,
                RepositoryOverviewService.class,
                UserCredentialService.class,
                UserProfileService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(APPLICATION_SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void repositoryContextServices_resideInRepositoryServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                BranchLoadService.class,
                BranchManagementService.class,
                RepositoryLoadService.class,
                RepositoryManagementService.class,
                RepositoryMemberService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(REPOSITORY_SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void supportCollaborators_useComponentInsteadOfService() {
        List<Class<?>> supportClasses = List.of(
                CloneUrlBuilder.class,
                BranchChangeRecorder.class,
                MergeabilityAssessmentAssembler.class,
                EventPolicyResolver.class,
                ExecutionRequestService.class,
                GitRepositoryAccessService.class,
                BranchFactory.class,
                UserService.class,
                RepositoryAccessibilityService.class,
                RepositoryLookupService.class,
                RepositoryMembershipFactory.class,
                RepositoryNamespaceResolver.class,
                RepositoryOwnershipPolicy.class,
                RepositoryProvisioner.class,
                RunnerRuntimeConfigProvider.class);

        supportClasses.forEach(supportClass -> {
            assertTrue(supportClass.isAnnotationPresent(Component.class));
            assertFalse(supportClass.isAnnotationPresent(Service.class));
        });
    }

    @Test
    void applicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path applicationRoot = Path.of("src/main/java/io/jgitkins/server/application");
        assertNoInfrastructureImports(applicationRoot);
    }

    @Test
    void repositoryApplicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path repositoryApplicationRoot = Path.of("src/main/java/io/jgitkins/server/repository/application");
        assertNoInfrastructureImports(repositoryApplicationRoot);
    }

    private void assertNoInfrastructureImports(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
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
