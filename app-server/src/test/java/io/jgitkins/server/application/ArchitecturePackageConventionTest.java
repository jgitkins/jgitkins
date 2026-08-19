package io.jgitkins.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import io.jgitkins.server.shared.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.change.review.application.service.PullRequestCreateService;
import io.jgitkins.server.change.review.application.service.PullRequestQueryService;
import io.jgitkins.server.execution.application.support.ExecutionRequestService;
import io.jgitkins.server.execution.application.policy.EventPolicyResolver;
import io.jgitkins.server.execution.application.service.JobDispatchService;
import io.jgitkins.server.execution.application.service.JobResultReportService;
import io.jgitkins.server.execution.application.service.JobService;
import io.jgitkins.server.execution.application.service.RunnerManagementService;
import io.jgitkins.server.execution.application.service.RunnerReadService;
import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import io.jgitkins.server.identity.access.application.service.AdminUserService;
import io.jgitkins.server.repository.application.service.CommitService;
import io.jgitkins.server.change.review.application.service.MergeService;
import io.jgitkins.server.identity.access.application.service.OAuthLoginService;
import io.jgitkins.server.collaboration.application.service.OrganizeMemberService;
import io.jgitkins.server.collaboration.application.service.OrganizeService;
import io.jgitkins.server.collaboration.infrastructure.adapter.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.infrastructure.adapter.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntity;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntityCondition;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntityCondition;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.identity.access.application.service.PublicUserQueryService;
import io.jgitkins.server.execution.application.service.PushEventHandleService;
import io.jgitkins.server.repository.application.service.RepositoryFileService;
import io.jgitkins.server.identity.access.application.service.UserCredentialService;
import io.jgitkins.server.identity.access.application.service.UserProfileService;
import io.jgitkins.server.execution.presentation.api.rest.RunnerController;
import io.jgitkins.server.identity.access.presentation.api.rest.AdminUserController;
import io.jgitkins.server.change.review.presentation.api.rest.MergeController;
import io.jgitkins.server.identity.access.presentation.api.rest.OAuthController;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeController;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberController;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeUpdateRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeMemberRequestMapper;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeRequestMapper;
import io.jgitkins.server.identity.access.presentation.api.rest.SignupController;
import io.jgitkins.server.identity.access.presentation.api.rest.UserController;
import io.jgitkins.server.identity.access.presentation.api.rest.UserCredentialController;
import io.jgitkins.server.collaboration.adapter.in.web.WebOrganizeController;
import io.jgitkins.server.repository.presentation.api.web.WebRepositoryController;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.change.review.presentation.api.rest.PullRequestController;
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
import io.jgitkins.server.identity.access.application.support.UserService;
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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class ArchitecturePackageConventionTest {

    private static final String REPOSITORY_SERVICE_PACKAGE = "io.jgitkins.server.repository.application.service";
    private static final String CHANGE_REVIEW_SERVICE_PACKAGE = "io.jgitkins.server.change.review.application.service";
    private static final String EXECUTION_SERVICE_PACKAGE = "io.jgitkins.server.execution.application.service";
    private static final String COLLABORATION_APPLICATION_SERVICE_PACKAGE = "io.jgitkins.server.collaboration.application.service";
    private static final String COLLABORATION_INFRASTRUCTURE_ADAPTER_PACKAGE = "io.jgitkins.server.collaboration.infrastructure.adapter.persistence";
    private static final String COLLABORATION_INFRASTRUCTURE_MAPPER_PACKAGE = "io.jgitkins.server.collaboration.infrastructure.mapper";
    private static final String COLLABORATION_INFRASTRUCTURE_PERSISTENCE_MODEL_PACKAGE = "io.jgitkins.server.collaboration.infrastructure.persistence.model";
    private static final String COLLABORATION_INFRASTRUCTURE_PERSISTENCE_MAPPER_PACKAGE = "io.jgitkins.server.collaboration.infrastructure.persistence.mapper";
    private static final String IDENTITY_ACCESS_SERVICE_PACKAGE = "io.jgitkins.server.identity.access.application.service";
    private static final String IDENTITY_ACCESS_PORT_OUT_PACKAGE = "io.jgitkins.server.identity.access.application.port.out";
    @Test
    void repositoryServices_resideInRepositoryServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                CommitService.class,
                BranchLoadService.class,
                BranchManagementService.class,
                RepositoryFileService.class,
                RepositoryLoadService.class,
                RepositoryManagementService.class,
                RepositoryMemberService.class,
                RepositoryOverviewService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(REPOSITORY_SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void collaborationServices_resideInCollaborationServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                OrganizeMemberService.class,
                OrganizeService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(
                COLLABORATION_APPLICATION_SERVICE_PACKAGE,
                serviceClass.getPackageName()));
    }

    @Test
    void collaborationInfrastructureClasses_resideInCollaborationInfrastructurePackages() {
        List<Class<?>> classes = List.of(
                OrganizePersistenceAdapter.class,
                OrganizeMemberPersistenceAdapter.class,
                OrganizeDomainMapper.class,
                OrganizeMemberDomainMapper.class,
                OrganizeEntityMbgMapper.class,
                OrganizeMemberEntityMbgMapper.class,
                OrganizeEntity.class,
                OrganizeEntityCondition.class,
                OrganizeMemberEntity.class,
                OrganizeMemberEntityCondition.class);

        classes.forEach(clazz -> {
            String packageName = clazz.getPackageName();
            boolean adapterPackage = packageName.equals(COLLABORATION_INFRASTRUCTURE_ADAPTER_PACKAGE);
            boolean mapperPackage = packageName.equals(COLLABORATION_INFRASTRUCTURE_MAPPER_PACKAGE);
            boolean persistenceModelPackage = packageName.equals(COLLABORATION_INFRASTRUCTURE_PERSISTENCE_MODEL_PACKAGE);
            boolean persistenceMapperPackage = packageName.equals(COLLABORATION_INFRASTRUCTURE_PERSISTENCE_MAPPER_PACKAGE);
            assertTrue(adapterPackage || mapperPackage || persistenceModelPackage || persistenceMapperPackage);
        });
    }

    @Test
    void identityAccessServices_resideInIdentityAccessServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                AdminUserService.class,
                OAuthLoginService.class,
                PublicUserQueryService.class,
                UserCredentialService.class,
                UserProfileService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(IDENTITY_ACCESS_SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void identityAccessOutboundPorts_resideInIdentityAccessPortOutPackage() {
        List<Class<?>> portClasses = List.of(
                CurrentUserPort.class,
                TokenIssuerPort.class,
                UserCredentialPersistencePort.class,
                UserIdentityPersistencePort.class,
                UserPersistencePort.class);

        portClasses.forEach(portClass -> assertEquals(IDENTITY_ACCESS_PORT_OUT_PACKAGE, portClass.getPackageName()));
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
    void collaborationPersistenceContracts_haveExplicitOwnershipAndShape() {
        assertEquals("io.jgitkins.server.collaboration.domain.repository", OrganizeRepository.class.getPackageName());
        assertEquals("io.jgitkins.server.collaboration.application.port.out", OrganizeQueryPort.class.getPackageName());
        assertTrue(OrganizeRepository.class.isAssignableFrom(OrganizePersistenceAdapter.class));
        assertTrue(OrganizeQueryPort.class.isAssignableFrom(OrganizePersistenceAdapter.class));

        assertEquals(Set.of("save", "update", "findById", "findByName", "findAll", "deleteById"),
                Arrays.stream(OrganizeRepository.class.getDeclaredMethods())
                        .map(Method::getName)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("findById", "findByName"),
                Arrays.stream(OrganizeQueryPort.class.getDeclaredMethods())
                        .map(Method::getName)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void changeReviewServices_resideInChangeReviewServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                PullRequestCreateService.class,
                PullRequestQueryService.class,
                io.jgitkins.server.change.review.application.service.MergeService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(CHANGE_REVIEW_SERVICE_PACKAGE, serviceClass.getPackageName()));
    }

    @Test
    void executionServices_resideInExecutionServicePackage() {
        List<Class<?>> serviceClasses = List.of(
                JobDispatchService.class,
                JobResultReportService.class,
                JobService.class,
                PushEventHandleService.class,
                RunnerManagementService.class,
                RunnerReadService.class);

        serviceClasses.forEach(serviceClass -> assertEquals(EXECUTION_SERVICE_PACKAGE, serviceClass.getPackageName()));
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
                PullRequestController.class,
                io.jgitkins.server.change.review.presentation.api.rest.MergeController.class,
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
    void collaborationControllers_resideInInboundAdapterPackages() {
        List<Class<?>> controllerClasses = List.of(
                OrganizeController.class,
                OrganizeMemberController.class,
                WebOrganizeController.class);

        controllerClasses.forEach(controllerClass -> {
            String packageName = controllerClass.getPackageName();
            boolean matchesRest = packageName.equals("io.jgitkins.server.collaboration.adapter.in.rest");
            boolean matchesWeb = packageName.equals("io.jgitkins.server.collaboration.adapter.in.web");
            assertTrue(matchesRest || matchesWeb);
        });
    }

    @Test
    void collaborationInboundContracts_resideInExpectedPackages() {
        assertEquals("io.jgitkins.server.collaboration.adapter.in.rest.dto.request",
                OrganizeCreationRequest.class.getPackageName());
        assertEquals("io.jgitkins.server.collaboration.adapter.in.rest.dto.request",
                OrganizeMemberAddRequest.class.getPackageName());
        assertEquals("io.jgitkins.server.collaboration.adapter.in.rest.dto.request",
                OrganizeUpdateRequest.class.getPackageName());
        assertEquals("io.jgitkins.server.collaboration.adapter.in.rest.mapper",
                OrganizeRequestMapper.class.getPackageName());
        assertEquals("io.jgitkins.server.collaboration.adapter.in.rest.mapper",
                OrganizeMemberRequestMapper.class.getPackageName());
    }

    @Test
    void changeReviewControllers_resideInChangeReviewPresentationPackage() {
        List<Class<?>> controllerClasses = List.of(
                PullRequestController.class,
                io.jgitkins.server.change.review.presentation.api.rest.MergeController.class);

        controllerClasses.forEach(controllerClass -> assertEquals(
                "io.jgitkins.server.change.review.presentation.api.rest",
                controllerClass.getPackageName()));
    }

    @Test
    void applicationSources_areRemoved() throws IOException {
        assertNoJavaFiles(
                "src/main/java/io/jgitkins/server/application",
                "app-server/src/main/java/io/jgitkins/server/application");
    }

    @Test
    void collaborationPresentationSources_areRemoved() throws IOException {
        assertNoJavaFiles(
                "src/main/java/io/jgitkins/server/collaboration/presentation",
                "app-server/src/main/java/io/jgitkins/server/collaboration/presentation");
    }

    @Test
    void collaborationInboundAdapters_doNotImportInfrastructure() throws IOException {
        Path inboundRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/collaboration/adapter/in",
                "app-server/src/main/java/io/jgitkins/server/collaboration/adapter/in");
        assertNoImports(inboundRoot, "import io.jgitkins.server.collaboration.infrastructure.");
    }

    @Test
    void repositoryApplicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path repositoryApplicationRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/repository/application",
                "app-server/src/main/java/io/jgitkins/server/repository/application");
        assertNoInfrastructureImports(repositoryApplicationRoot);
    }

    @Test
    void collaborationInfrastructureSources_doNotImportTopLevelInfrastructurePersistencePackages() throws IOException {
        Path collaborationInfrastructureRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/collaboration/infrastructure",
                "app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure");
        assertNoImports(collaborationInfrastructureRoot, "import io.jgitkins.server.infrastructure.persistence.");
    }

    @Test
    void identityAccessApplicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path identityAccessApplicationRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/identity/access/application",
                "app-server/src/main/java/io/jgitkins/server/identity/access/application");
        assertNoInfrastructureImports(identityAccessApplicationRoot);
    }

    @Test
    void repositoryInfrastructureSources_doNotImportLegacyOrganizeInfrastructurePackages() throws IOException {
        Path repositoryInfrastructureRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/repository/infrastructure",
                "app-server/src/main/java/io/jgitkins/server/repository/infrastructure");
        assertNoImports(repositoryInfrastructureRoot, "import io.jgitkins.server.infrastructure.persistence.mapper.Organize");
        assertNoImports(repositoryInfrastructureRoot, "import io.jgitkins.server.infrastructure.persistence.model.Organize");
    }

    @Test
    void repositoryGitAdapters_doNotImportRepositoryApplicationExceptions() throws IOException {
        Path repositoryGitAdapterRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git",
                "app-server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/git");
        assertNoImports(repositoryGitAdapterRoot, "import io.jgitkins.server.repository.application.exception.");
    }

    @Test
    void coreModules_doNotImportApplicationModules() throws IOException {
        List<Path> coreRoots = Stream.of(
                        "core-common/src/main/java",
                        "core-web/src/main/java",
                        "core-security/src/main/java",
                        "core-persistence/src/main/java",
                        "core-grpc/src/main/java")
                .flatMap(candidate -> Stream.of(Path.of("../" + candidate), Path.of(candidate)))
                .filter(Files::exists)
                .toList();

        for (Path coreRoot : coreRoots) {
            assertNoImports(coreRoot, "import io.jgitkins.server.");
            assertNoImports(coreRoot, "import io.jgitkins.web.");
            assertNoImports(coreRoot, "import io.jgitkins.runner.");
        }
    }

    @Test
    void webMvcControllers_doNotUseCoreApiResponseAsViewModel() throws IOException {
        Path webPresentationRoot = resolveExistingPath(
                "../app-web/src/main/java/io/jgitkins/web/presentation",
                "app-web/src/main/java/io/jgitkins/web/presentation");
        assertNoImports(webPresentationRoot, "import io.jgitkins.core.web.api.response.ApiResponse;");
    }

    @Test
    void corePersistence_doesNotOwnBusinessPersistenceModels() throws IOException {
        Path corePersistenceRoot = resolveExistingPath("../core-persistence/src/main/java", "core-persistence/src/main/java");

        assertNoPath(corePersistenceRoot, "model");
        assertNoPath(corePersistenceRoot, "entity");
        assertNoPath(corePersistenceRoot, "adapter");
    }

    private Path resolveExistingPath(String... candidates) {
        return Arrays.stream(candidates)
                .map(Path::of)
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to resolve existing path from candidates: "
                        + String.join(", ", candidates)));
    }

    private void assertNoInfrastructureImports(Path root) throws IOException {
        assertNoImports(root, "import io.jgitkins.server.infrastructure.");
    }

    private void assertNoJavaFiles(String... candidates) throws IOException {
        for (String candidate : candidates) {
            Path root = Path.of(candidate);
            if (!Files.exists(root)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(root)) {
                assertFalse(files.anyMatch(path -> path.toString().endsWith(".java")),
                        () -> "Path must not contain Java sources: " + root);
            }
        }
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
