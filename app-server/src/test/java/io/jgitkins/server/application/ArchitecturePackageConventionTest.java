package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.application.support.CloneUrlBuilder;
import io.jgitkins.server.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.execution.application.support.ExecutionRequestService;
import io.jgitkins.server.execution.application.policy.EventPolicyResolver;
import io.jgitkins.server.application.service.AdminUserService;
import io.jgitkins.server.application.service.CommitService;
import io.jgitkins.server.application.service.MergeService;
import io.jgitkins.server.application.service.OAuthLoginService;
import io.jgitkins.server.application.service.OrganizeMemberService;
import io.jgitkins.server.application.service.OrganizeService;
import io.jgitkins.server.application.service.PublicUserQueryService;
import io.jgitkins.server.application.service.PushEventHandleService;
import io.jgitkins.server.application.service.RepositoryFileService;
import io.jgitkins.server.application.service.UserCredentialService;
import io.jgitkins.server.application.service.UserProfileService;
import io.jgitkins.server.execution.presentation.api.rest.RunnerController;
import io.jgitkins.server.presentation.api.rest.AdminUserController;
import io.jgitkins.server.presentation.api.rest.MergeController;
import io.jgitkins.server.presentation.api.rest.OAuthController;
import io.jgitkins.server.presentation.api.rest.OrganizeController;
import io.jgitkins.server.presentation.api.rest.OrganizeMemberController;
import io.jgitkins.server.presentation.api.rest.SignupController;
import io.jgitkins.server.presentation.api.rest.UserController;
import io.jgitkins.server.presentation.api.rest.UserCredentialController;
import io.jgitkins.server.presentation.api.web.WebOrganizeController;
import io.jgitkins.server.presentation.api.web.WebRepositoryController;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.repository.application.service.BranchLoadService;
import io.jgitkins.server.repository.application.service.BranchManagementService;
import io.jgitkins.server.repository.application.service.RepositoryLoadService;
import io.jgitkins.server.repository.application.service.RepositoryManagementService;
import io.jgitkins.server.repository.application.service.RepositoryMemberService;
import io.jgitkins.server.repository.application.service.RepositoryOverviewService;
import io.jgitkins.server.repository.presentation.api.rest.BranchController;
import io.jgitkins.server.repository.presentation.api.rest.RepositoryCommitController;
import io.jgitkins.server.repository.presentation.api.rest.RepositoryContentController;
import io.jgitkins.server.repository.presentation.api.rest.RepositoryFileController;
import io.jgitkins.server.repository.presentation.api.rest.RepositoryManagementController;
import io.jgitkins.server.repository.presentation.api.rest.RepositoryMemberController;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryAccessibilityService;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.execution.application.support.RunnerRuntimeConfigProvider;
import io.jgitkins.server.application.support.UserService;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
                RepositoryMemberService.class,
                RepositoryOverviewService.class);

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
    void restAndWebApiControllers_returnApiResponseEnvelope() {
        List<Class<?>> controllerClasses = List.of(
                AdminUserController.class,
                MergeController.class,
                OAuthController.class,
                OrganizeController.class,
                OrganizeMemberController.class,
                SignupController.class,
                UserController.class,
                UserCredentialController.class,
                WebOrganizeController.class,
                WebRepositoryController.class,
                RunnerController.class,
                BranchController.class,
                RepositoryCommitController.class,
                RepositoryContentController.class,
                RepositoryFileController.class,
                RepositoryManagementController.class,
                RepositoryMemberController.class);

        controllerClasses.stream()
                .flatMap(controllerClass -> Stream.of(controllerClass.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .forEach(this::assertReturnsApiResponseEntity);
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

    @Test
    void repositoryGitAdapters_doNotImportRepositoryApplicationExceptions() throws IOException {
        Path repositoryGitAdapterRoot = Path.of("src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git");
        assertNoImports(repositoryGitAdapterRoot, "import io.jgitkins.server.repository.application.exception.");
    }

    @Test
    void coreModules_doNotImportApplicationModules() throws IOException {
        List<Path> coreRoots = List.of(
                Path.of("../core-common/src/main/java"),
                Path.of("../core-web/src/main/java"),
                Path.of("../core-security/src/main/java"),
                Path.of("../core-persistence/src/main/java"),
                Path.of("../core-grpc/src/main/java"));

        for (Path coreRoot : coreRoots) {
            assertNoImports(coreRoot, "import io.jgitkins.server.");
            assertNoImports(coreRoot, "import io.jgitkins.web.");
            assertNoImports(coreRoot, "import io.jgitkins.runner.");
        }
    }

    @Test
    void webMvcControllers_doNotUseCoreApiResponseAsViewModel() throws IOException {
        Path webPresentationRoot = Path.of("../web/src/main/java/io/jgitkins/web/presentation");
        assertNoImports(webPresentationRoot, "import io.jgitkins.core.web.api.response.ApiResponse;");
    }

    @Test
    void corePersistence_doesNotOwnBusinessPersistenceModels() throws IOException {
        Path corePersistenceRoot = Path.of("../core-persistence/src/main/java");

        assertNoPath(corePersistenceRoot, "model");
        assertNoPath(corePersistenceRoot, "entity");
        assertNoPath(corePersistenceRoot, "adapter");
    }

    private void assertNoInfrastructureImports(Path root) throws IOException {
        assertNoImports(root, "import io.jgitkins.server.infrastructure.");
    }

    private void assertReturnsApiResponseEntity(Method method) {
        assertEquals(ResponseEntity.class, method.getReturnType(),
                () -> "API method must return ResponseEntity<ApiResponse<...>>: " + method);

        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType,
                () -> "API method must declare generic response body: " + method);

        Type responseBodyType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        assertTrue(responseBodyType instanceof ParameterizedType,
                () -> "API method response body must be ApiResponse<...>: " + method);
        assertEquals(ApiResponse.class, ((ParameterizedType) responseBodyType).getRawType(),
                () -> "API method response body must be ApiResponse<...>: " + method);
    }

    private void assertNoPath(Path root, String disallowedPathSegment) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            boolean hasDisallowedPath = files
                    .anyMatch(path -> path.getNameCount() > 0
                            && path.toString().contains("/" + disallowedPathSegment + "/"));

            assertFalse(hasDisallowedPath,
                    () -> "Path must not contain segment " + disallowedPathSegment + ": " + root);
        }
    }

    private void assertNoImports(Path root, String disallowedImportPrefix) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> javaFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);
                assertFalse(source.lines().anyMatch(line -> line.startsWith(disallowedImportPrefix)),
                        () -> "Source must not import " + disallowedImportPrefix + ": " + javaFile);
            }
        }
    }
}
