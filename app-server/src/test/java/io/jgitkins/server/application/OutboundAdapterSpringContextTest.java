package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.JGitkinsServerApplication;
import io.jgitkins.server.identity.access.adapter.in.security.PatAuthenticationProvider;
import io.jgitkins.server.change.review.adapter.out.persistence.jpa.PullRequestJpaPersistenceAdapter;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistence;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobDispatchJpaQueryAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobJpaRepositoryAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.RunnerJpaPersistenceAdapter;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistence;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserCredentialJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserIdentityJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistence;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.BranchJpaQueryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.BranchJpaRepositoryAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryMemberJpaPersistenceAdapter;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistence;
import io.jgitkins.server.execution.application.service.JobDispatchService;
import io.jgitkins.server.repository.application.service.RepositoryOverviewService;
import io.jgitkins.server.repository.application.service.internal.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.service.internal.RepositoryLookupService;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationMembershipAclAdapter;
import io.jgitkins.server.execution.adapter.out.acl.RepositoryCloneUrlAclAdapter;
import io.jgitkins.server.identity.access.adapter.out.security.PatTokenAuthenticationService;
import io.jgitkins.server.identity.access.adapter.in.security.JwtAuthenticationFilter;
import io.jgitkins.server.identity.access.adapter.out.security.JwtTokenIssuerAdapter;
import io.jgitkins.server.identity.access.adapter.out.security.JwtTokenVerifierAdapter;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import io.jgitkins.server.identity.access.infrastructure.config.security.JwtProperties;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;

@SpringBootTest(
        classes = JGitkinsServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=false",
                "spring.sql.init.mode=never",
                "grpc.server.port=0",
                "server.port=0",
                "jgitkins.server.runtime.rest-port=0",
                "jgitkins.server.runtime.grpc-port=0",
                "spring.security.oauth2.client.registration.google.client-id=test-client",
                "spring.security.oauth2.client.registration.google.client-secret=test-secret",
                "spring.autoconfigure.exclude=net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration"
        })
@ActiveProfiles("test")
@ResourceLock("outbound-adapter-spring-context")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OutboundAdapterSpringContextTest {
    @TempDir
    static Path runtimeVolume;

    @LocalServerPort
    int restPort;
    static int grpcPort;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        grpcPort = freePort();
        registry.add("JDBC_URL", () -> "jdbc:h2:mem:outbound_adapter_context;MODE=MariaDB;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("JDBC_USERNAME", () -> "sa");
        registry.add("JDBC_PASSWORD", () -> "");
        registry.add("BARE_PATH", () -> runtimeVolume.toString());
        registry.add("JGITKINS_JWT_SECRET", () -> "outbound-adapter-test-jwt-secret");
        registry.add("spring.datasource.hikari.jdbc-url", () -> "jdbc:h2:mem:outbound_adapter_context;MODE=MariaDB;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.hikari.username", () -> "sa");
        registry.add("spring.datasource.hikari.password", () -> "");
        registry.add("jgitkins.server.runtime.volume", () -> runtimeVolume.toString());
        registry.add("grpc.server.port", () -> grpcPort);
    }

    @Test
    void movedOutboundAdaptersAreSpringManagedAndRandomPortIsUsed() {
        assertThat(restPort).isPositive().isNotEqualTo(18080);
        assertThat(grpcPort).isPositive().isNotEqualTo(19090);
        assertThat(environment.getProperty("grpc.server.port", Integer.class)).isEqualTo(grpcPort);
        assertThat(applicationContext.getBean(PatAuthenticationProvider.class)).isNotNull();
        assertThat(applicationContext.getBean(PatTokenAuthenticationService.class)).isNotNull();
        assertThat(applicationContext.getBean(GitRepositoryAccessService.class)).isNotNull();
        assertThat(applicationContext.getBean(RepositoryLookupService.class)).isNotNull();
        assertThat(applicationContext.getBean(RepositoryOverviewService.class)).isNotNull();
        assertThat(applicationContext.getBean(RepositoryAccessValidator.class)).isNotNull();
        assertThat(applicationContext.getBean(JobDispatchService.class)).isNotNull();
        // The port, not either implementation. application.yml selects JPA for this slice, so
        // asserting OrganizePersistenceAdapter asserted a bean the selector no longer builds --
        // and asserting OrganizeJpaPersistenceAdapter would break again on a rollback to mybatis.
        // What this test is for is that the moved outbound adapter is wired at all.
        assertThat(applicationContext.getBean(OrganizePersistence.class)).isNotNull();
        assertThat(applicationContext.getBean(OrganizationMembershipAclAdapter.class)).isNotNull();

        assertThat(applicationContext.getBean(RepositoryCloneUrlAclAdapter.class)).isNotNull();
        assertThat(applicationContext.getBeansOfType(JwtAuthenticationFilter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtAuthService.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtTokenIssuerAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtTokenVerifierAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtProperties.class)).hasSize(1);
        assertThat(environment.getProperty("jgitkins.security.jwt.secret")).isEqualTo("jgitkins-test-jwt-secret-256-bit-minimum");
        assertThat(environment.getProperty("jgitkins.security.jwt.ttl-seconds", Long.class)).isEqualTo(900L);
    }

    /**
     * Every outbound persistence port resolves to exactly one bean, and it is the JPA adapter.
     *
     * <p>Replaces the surviving half of eight {@code *SelectorTest} classes. Their subject was which of
     * two adapters a property selects, and that question is gone: the selector configurations are
     * deleted and the JPA adapters are {@code @Component}s again. What is not gone is the failure the
     * selector configurations were written to avoid in the first place -- two implementations of one
     * port in the context, which is {@code NoUniqueBeanDefinitionException} at startup rather than a
     * choice. Moving to a component scan is exactly the change that could reintroduce it, by an
     * annotation on a class nobody meant to register.
     *
     * <p>Asserted on the domain and application ports rather than on the aggregate persistence
     * interfaces, because the ports are what services inject: {@code OrganizePersistence} having one
     * bean says nothing about {@code OrganizeRepository} being unambiguous if something else also
     * implements it.
     *
     * <p>{@code hasSize(1)} and not {@code isNotNull}: {@code getBean} on an ambiguous type throws, but
     * {@code @Primary} or a bean-name match would resolve it and hide a second implementation that no
     * longer belongs in the context at all.
     */
    @Test
    void everyPersistencePortHasOneImplementationAndItIsTheJpaAdapter() {
        Map<Class<?>, Class<?>> expected = new LinkedHashMap<>();
        expected.put(PullRequestRepository.class, PullRequestJpaPersistenceAdapter.class);
        expected.put(OrganizeRepository.class, OrganizeJpaPersistenceAdapter.class);
        expected.put(OrganizeQueryPort.class, OrganizeJpaPersistenceAdapter.class);
        expected.put(OrganizeMemberPersistence.class, OrganizeMemberJpaPersistenceAdapter.class);
        expected.put(JobRepository.class, JobJpaRepositoryAdapter.class);
        expected.put(JobDispatchQueryPort.class, JobDispatchJpaQueryAdapter.class);
        expected.put(RunnerRepository.class, RunnerJpaPersistenceAdapter.class);
        expected.put(UserRepository.class, UserJpaPersistenceAdapter.class);
        expected.put(UserQueryPort.class, UserJpaPersistenceAdapter.class);
        expected.put(UserCredentialPersistencePort.class, UserCredentialJpaPersistenceAdapter.class);
        expected.put(UserIdentityPersistencePort.class, UserIdentityJpaPersistenceAdapter.class);
        expected.put(RepositoryRepository.class, RepositoryJpaPersistenceAdapter.class);
        expected.put(RepositoryQueryPort.class, RepositoryJpaPersistenceAdapter.class);
        expected.put(RepositoryMemberPersistencePort.class, RepositoryMemberJpaPersistenceAdapter.class);
        expected.put(BranchRepository.class, BranchJpaRepositoryAdapter.class);
        expected.put(BranchQueryPort.class, BranchJpaQueryAdapter.class);

        expected.forEach((port, adapter) -> {
            assertThat(applicationContext.getBeansOfType(port))
                    .as("%s must have exactly one implementation in the context", port.getSimpleName())
                    .hasSize(1);
            assertThat(applicationContext.getBean(port)).isInstanceOf(adapter);
        });

        // One object behind both halves of a split interface, not two beans that happen to agree. Three
        // candidates for RepositoryRepository once existed here, from forwarding beans added to
        // re-expose the ports, and every injection point for it became ambiguous in production.
        assertThat(applicationContext.getBean(RepositoryQueryPort.class))
                .isSameAs(applicationContext.getBean(RepositoryRepository.class));
        assertThat(applicationContext.getBean(OrganizeQueryPort.class))
                .isSameAs(applicationContext.getBean(OrganizeRepository.class));
        assertThat(applicationContext.getBean(UserQueryPort.class))
                .isSameAs(applicationContext.getBean(UserRepository.class));
        assertThat(applicationContext.getBeansOfType(RepositoryPersistence.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(UserPersistence.class)).hasSize(1);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to allocate test gRPC port", e);
        }
    }

    @AfterAll
    static void deleteRuntimeVolume() throws IOException {
        if (runtimeVolume != null && Files.exists(runtimeVolume)) {
            try (var paths = Files.walk(runtimeVolume)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
        assertThat(runtimeVolume == null || Files.notExists(runtimeVolume)).isTrue();
    }
}
