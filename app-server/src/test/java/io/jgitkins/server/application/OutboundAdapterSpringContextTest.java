package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.JGitkinsServerApplication;
import io.jgitkins.server.identity.access.adapter.in.security.PatAuthenticationProvider;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.execution.application.service.JobDispatchService;
import io.jgitkins.server.repository.application.service.RepositoryOverviewService;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
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
        assertThat(applicationContext.getBean(OrganizePersistenceAdapter.class)).isNotNull();
        assertThat(applicationContext.getBean(OrganizationMembershipAclAdapter.class)).isNotNull();

        assertThat(applicationContext.getBean(RepositoryCloneUrlAclAdapter.class)).isNotNull();
        assertThat(applicationContext.getBeansOfType(JwtAuthenticationFilter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtAuthService.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtTokenIssuerAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtTokenVerifierAdapter.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(JwtProperties.class)).hasSize(1);
        assertThat(environment.getProperty("jgitkins.security.jwt.secret")).isEqualTo("test-jwt-secret-test-jwt-secret");
        assertThat(environment.getProperty("jgitkins.security.jwt.ttl-seconds", Long.class)).isEqualTo(900L);
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
