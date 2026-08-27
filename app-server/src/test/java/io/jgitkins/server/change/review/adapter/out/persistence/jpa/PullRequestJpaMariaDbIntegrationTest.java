package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises {@code PullRequestJpaPersistenceAdapter} against the real MariaDB schema.
 *
 * <p>The drift round trip is the part worth having. {@code TargetDrift} is stored across three columns
 * — a flag and two heads — and the read gates the heads behind the flag. A test that only saved a
 * non-drifted pull request would pass against an adapter that had the gate inverted.
 */
class PullRequestJpaMariaDbIntegrationTest {

    private static final String PACKAGES = "io.jgitkins.server.change.review.adapter.out.persistence.jpa";

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private EntityManagerFactory emf;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private PullRequestJpaPersistenceAdapter adapter;
    private Long repositoryId;

    @BeforeEach
    void setUp() {
        assumeTrue(JpaMariaDbTestSupport.mariaDbReachable(),
                "MariaDB on 127.0.0.1:53306 is required for change-review JPA evidence");

        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(
                dataSource, "change-review-jpa-integration", PACKAGES);
        emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbc = new JdbcTemplate(dataSource);
        adapter = new PullRequestJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, PullRequestJpaRepository.class));

        // Scoped by repository id rather than a name, because PULL_REQUEST has no natural text key.
        // A per-run id keeps the cleanup predicate exact.
        repositoryId = 7_500_000_000L + (System.nanoTime() % 1_000_000L);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && repositoryId != null) {
            jdbc.update("delete from PULL_REQUEST where REPOSITORY_ID = ?", repositoryId);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void persistsPullRequestAgainstMariaDb() {
        PullRequest created = transactions.execute(status -> adapter.save(PullRequest.rehydrate(
                null,
                ReviewRepositoryId.of(repositoryId),
                BranchHeadSnapshot.of("feature", "a".repeat(40)),
                BranchHeadSnapshot.of("main", "b".repeat(40)),
                PullRequestStatus.OPEN,
                null,
                TargetDrift.none(),
                null,
                null)));

        assertThat(created.getId()).as("the generated id must come back to the caller").isNotNull();
        assertThat(created.getCreatedAt())
                .as("and so must the timestamps the adapter stamped, or the caller holds a half-built "
                        + "aggregate")
                .isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();

        PullRequestId id = created.getId();
        PullRequest loaded = transactions.execute(status -> adapter.findById(id).orElseThrow());
        assertThat(loaded.getSource().branchName().getValue()).isEqualTo("feature");
        assertThat(loaded.getTarget().commitHash().getValue()).isEqualTo("b".repeat(40));
        assertThat(loaded.getStatus()).isEqualTo(PullRequestStatus.OPEN);
        assertThat(loaded.getTargetDrift().drifted()).isFalse();
        assertThat(loaded.getLastAssessmentSnapshot())
                .as("the assessment is recomputed per read and is not stored; rehydrating anything but "
                        + "null would be inventing state the table does not hold")
                .isNull();

        // Now drift it, and read it back through the flag gate.
        PullRequest drifted = transactions.execute(status -> adapter.save(PullRequest.rehydrate(
                id,
                ReviewRepositoryId.of(repositoryId),
                BranchHeadSnapshot.of("feature", "a".repeat(40)),
                BranchHeadSnapshot.of("main", "c".repeat(40)),
                PullRequestStatus.OPEN,
                null,
                TargetDrift.detected(CommitHash.of("b".repeat(40)), CommitHash.of("c".repeat(40))),
                loaded.getCreatedAt(),
                loaded.getUpdatedAt())));

        assertThat(drifted.getId()).isEqualTo(id);
        assertThat(jdbc.queryForObject(
                "select count(*) from PULL_REQUEST where REPOSITORY_ID = ?", Integer.class, repositoryId))
                .as("the update must not insert a second row")
                .isEqualTo(1);

        PullRequest reloaded = transactions.execute(status -> adapter.findById(id).orElseThrow());
        assertThat(reloaded.getTargetDrift().drifted()).isTrue();
        assertThat(reloaded.getTargetDrift().previousTargetHead().getValue()).isEqualTo("b".repeat(40));
        assertThat(reloaded.getTargetDrift().currentTargetHead().getValue()).isEqualTo("c".repeat(40));

        // And clear the drift again: the flag is the gate, so the heads must stop being reported even
        // if a row somewhere still carries them.
        transactions.executeWithoutResult(status -> adapter.save(PullRequest.rehydrate(
                id,
                ReviewRepositoryId.of(repositoryId),
                BranchHeadSnapshot.of("feature", "a".repeat(40)),
                BranchHeadSnapshot.of("main", "c".repeat(40)),
                PullRequestStatus.OPEN,
                null,
                TargetDrift.none(),
                loaded.getCreatedAt(),
                loaded.getUpdatedAt())));

        PullRequest cleared = transactions.execute(status -> adapter.findById(id).orElseThrow());
        assertThat(cleared.getTargetDrift().drifted())
                .as("clearing the flag must clear the reported drift")
                .isFalse();

        Optional<PullRequest> missing = transactions.execute(status ->
                adapter.findById(PullRequestId.of(Long.MAX_VALUE)));
        assertThat(missing).as("a missing id is empty, not an exception").isEmpty();
    }
}
