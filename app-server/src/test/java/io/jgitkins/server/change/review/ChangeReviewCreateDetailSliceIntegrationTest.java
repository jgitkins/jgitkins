package io.jgitkins.server.change.review;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.JGitkinsServerApplication;
import io.jgitkins.server.change.review.application.contract.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.contract.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.contract.PullRequestResult;
import io.jgitkins.server.change.review.application.service.PullRequestCreateService;
import io.jgitkins.server.change.review.application.service.PullRequestQueryService;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = JGitkinsServerApplication.class, properties = {
                "server.port=0", "grpc.server.port=0", "REST_PORT=0", "GRPC_PORT=0",
                "SERVICE_HOST=localhost", "REST_SCHEME=http", "JGITKINS_JWT_SECRET=integration-test-secret",
                "spring.security.oauth2.client.registration.google.client-id=integration-client",
                "spring.security.oauth2.client.registration.google.client-secret=integration-secret",
                "spring.autoconfigure.exclude=net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration,net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration",
                "spring.datasource.hikari.jdbc-url=jdbc:h2:mem:change_review;MODE=MariaDB;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.hikari.driver-class-name=org.h2.Driver",
                "spring.datasource.hikari.username=sa", "spring.datasource.hikari.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:change-review-integration-schema.sql"
        })
@ActiveProfiles("change-review-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ChangeReviewCreateDetailSliceIntegrationTest {
    private static final String NAMESPACE = "alice";
    private static final String REPOSITORY = "demo";
    /** The slice writes as a member; the write gate is exercised in PullRequestCreateServiceTest. */
    private static final Long REQUESTER = 7L;
    private static final Path BARE_PATH;
    static {
        try { BARE_PATH = Files.createTempDirectory("change-review-integration-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private Path barePath;
    private ObjectId base;
    private ObjectId feature;

    /**
     * The two repository gates are stubbed, not exercised.
     *
     * <p>This slice is about the pull request lifecycle: create, persist, observe branch heads,
     * detect target drift. Task P0a added a write gate to create and a read gate to detail, and both
     * resolve repository membership -- which this slice's schema
     * ({@code change-review-integration-schema.sql}) does not carry. Adding those tables would turn a
     * lifecycle test into an authorization test that duplicates the dedicated ones and breaks
     * whenever the permission model moves.
     *
     * <p>The gates being wired at all is proven by {@code PullRequestCreateServiceTest} and
     * {@code PullRequestQueryServiceTest}, which assert the ports are called before any work runs.
     */
    @org.springframework.boot.test.mock.mockito.MockBean
    private io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort writeAccessPort;

    @org.springframework.boot.test.mock.mockito.MockBean
    private io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort readAccessPort;

    @Autowired private PullRequestCreateService createService;
    @Autowired private PullRequestQueryService queryService;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void fixtureProperties(DynamicPropertyRegistry registry) {
        registry.add("jgitkins.server.runtime.volume", () -> BARE_PATH.toString());
    }

    @BeforeAll
    void createGitFixture() throws Exception {
        barePath = BARE_PATH;
        Path gitDir = barePath.resolve(NAMESPACE).resolve(REPOSITORY + ".git");
        Files.createDirectories(gitDir.getParent());
        try (Git ignored = Git.init().setBare(true).setDirectory(gitDir.toFile()).call()) {
            // The bare repository is created by JGit; refs are installed below.
        }
        try (Repository repository = openRepository()) {
            base = commit(repository, null, "README.md", "base");
            feature = commit(repository, base, "feature.txt", "feature");
            update(repository, "main", base);
            update(repository, "feature", feature);
        }
    }

    @BeforeEach
    void seedDatabase() {
        jdbc.update("DELETE FROM PULL_REQUEST");
        jdbc.update("DELETE FROM REPOSITORY");
        jdbc.update("DELETE FROM USER");
        jdbc.update("INSERT INTO USER (ID, USERNAME) VALUES (1, 'alice')");
        jdbc.update("INSERT INTO REPOSITORY (ID, NAME, PATH, REPOSITORY_TYPE, OWNER_TYPE, OWNER_ID, CLONE_PATH, DEFAULT_BRANCH, VISIBILITY, STATUS) "
                + "VALUES (1, 'demo', 'demo', 'GIT', 'USER', 1, 'alice/demo.git', 'main', 'PUBLIC', 'REGISTERED')");
    }

    @AfterAll
    void removeFixture() throws IOException {
        if (barePath != null) {
            try (var paths = Files.walk(barePath)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }

    @Test
    void realSpringCreatePersistDetailAndTargetDriftFixture() throws Exception {
        PullRequestResult created = runWithTimeout(() -> createService.createPullRequest(
                new PullRequestCreateCommand(NAMESPACE, REPOSITORY, "feature", "main"), REQUESTER));
        assertThat(created.getId()).isNotNull();
        assertThat(created.getRepositoryId()).isEqualTo(1L);
        assertThat(created.getSource().commitHash().getValue()).isEqualTo(feature.name());
        assertThat(created.getTarget().commitHash().getValue()).isEqualTo(base.name());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PULL_REQUEST", Integer.class)).isEqualTo(1);

        PullRequestDetailResult equal = runWithTimeout(() -> queryService.getPullRequestDetail(
                PullRequestId.of(created.getId()), REQUESTER));
        assertThat(equal.getStoredTarget().commitHash().getValue()).isEqualTo(base.name());
        assertThat(equal.getCurrentTarget().commitHash().getValue()).isEqualTo(base.name());
        assertThat(equal.getTargetDrift().drifted()).isFalse();
        assertThat(equal.getMergeability()).isNotNull();
        assertThat(equal.getMergeability().status()).isEqualTo(MergeabilityStatus.MERGEABLE);

        try (Repository repository = openRepository()) {
            ObjectId target = commit(repository, base, "target.txt", "target");
            update(repository, "main", target);
        }
        PullRequestDetailResult drifted = runWithTimeout(() -> queryService.getPullRequestDetail(
                PullRequestId.of(created.getId()), REQUESTER));
        assertThat(drifted.getCurrentTarget().commitHash()).isNotEqualTo(base.name());
        assertThat(drifted.getTargetDrift().drifted()).isTrue();
        assertThat(drifted.getTargetDrift().previousTargetHead().getValue()).isEqualTo(base.name());
    }

    private Repository openRepository() throws IOException {
        return new FileRepositoryBuilder().setGitDir(barePath.resolve(NAMESPACE).resolve(REPOSITORY + ".git").toFile())
                .setMustExist(true).build();
    }

    private ObjectId commit(Repository repository, ObjectId parent, String path, String content) throws IOException {
        try (ObjectInserter inserter = repository.newObjectInserter()) {
            ObjectId blob = inserter.insert(Constants.OBJ_BLOB, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            TreeFormatter tree = new TreeFormatter();
            if (parent != null) {
                ObjectId baseBlob = inserter.insert(Constants.OBJ_BLOB, "base".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                tree.append("README.md", FileMode.REGULAR_FILE, baseBlob);
            }
            tree.append(path, FileMode.REGULAR_FILE, blob);
            ObjectId treeId = inserter.insert(tree);
            CommitBuilder builder = new CommitBuilder();
            builder.setTreeId(treeId);
            if (parent != null) builder.setParentId(parent);
            builder.setAuthor(new PersonIdent("alice", "alice@example.test", java.util.Date.from(Instant.parse("2026-01-01T00:00:00Z")), java.util.TimeZone.getTimeZone("UTC")));
            builder.setCommitter(builder.getAuthor());
            builder.setMessage(path + "=" + content);
            ObjectId commit = inserter.insert(builder);
            inserter.flush();
            return commit;
        }
    }

    private void update(Repository repository, String branch, ObjectId commit) throws IOException {
        RefUpdate update = repository.updateRef(Constants.R_HEADS + branch);
        update.setNewObjectId(commit);
        update.setForceUpdate(true);
        if (update.update() == RefUpdate.Result.REJECTED) throw new IOException("Unable to update " + branch);
    }

    private <T> T runWithTimeout(CheckedSupplier<T> operation) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(operation::get);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError("fixture operation exceeded 10 seconds", e);
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
}
