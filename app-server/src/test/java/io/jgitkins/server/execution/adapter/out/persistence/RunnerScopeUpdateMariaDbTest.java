package io.jgitkins.server.execution.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.adapter.out.persistence.mapper.RunnerAssignmentEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.mapper.RunnerEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerAssignmentDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerDomainMapper;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The MyBatis half of the scope-update fix, against the real MariaDB schema.
 *
 * <p>{@code RunnerJpaMariaDbIntegrationTest} covers the JPA adapter, and its method was named
 * "...UnderBothProviders" while constructing only the JPA one. Reverting the MyBatis adapter alone
 * left the suite green -- measured. That is the exact drift 2.126 warned about: two implementations
 * of one security-relevant decision, with a test that only watches one of them.
 *
 * <p>The decision here is which scope a runner actually dispatches under. The MyBatis adapter used to
 * call {@code updateByPrimaryKeySelective} with an assignment entity whose id
 * {@code RunnerAssignmentDomainMapper} never populates, so the statement resolved to
 * {@code where ID = null} and changed nothing. Narrowing a runner's scope for isolation looked
 * applied and was not.
 *
 * <p>Uses the standalone MyBatis wiring {@code OrganizeLockContractMariaDbTest} established rather
 * than a Spring context: the adapter needs two generated mappers and nothing else, and a full context
 * would put this test behind the schema-less H2 that the rest of the suite runs on.
 */
class RunnerScopeUpdateMariaDbTest {

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private RunnerPersistenceAdapter adapter;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        // The container that owns data/ddl.sql, not a hand-built DataSource. Task 2.103 moved the test
        // database into this singleton precisely so no test carries its own connection details; the
        // jgitkins.test.mariadb.* properties this used to read are defined nowhere -- build.gradle:122
        // sets only jgitkins.test.ddl, which is the schema JpaMariaDbTestSupport mounts.
        dataSource = JpaMariaDbTestSupport.dataSource();
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        SqlSessionTemplate session = new SqlSessionTemplate(sqlSessionFactory(dataSource));
        adapter = new RunnerPersistenceAdapter(
                session.getMapper(RunnerEntityMbgMapper.class),
                session.getMapper(RunnerAssignmentEntityMbgMapper.class),
                org.mapstruct.factory.Mappers.getMapper(RunnerDomainMapper.class),
                org.mapstruct.factory.Mappers.getMapper(RunnerAssignmentDomainMapper.class));

        token = "mybatis-scope-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete a from RUNNER_ASSIGNMENT a join RUNNER r on r.ID = a.RUNNER_ID"
                + " where r.TOKEN like ?", token + "%");
        jdbc.update("delete from RUNNER where TOKEN like ?", token + "%");
    }

    @Test
    void creatingARunnerRecordsTheScopeItWasAskedFor() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token, "scope at creation", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 920L, null, null, now)));

        // Read the row, not the return value. The create branch returns the scope the caller passed in,
        // so a wrong row is invisible from the aggregate -- which is how it survived: the branch mapped
        // the assignment from restoreRunner(entity), and that reads a database with no assignment row
        // yet and answers GLOBAL. Every runner created through MyBatis was recorded as GLOBAL.
        assertThat(jdbc.queryForMap(
                "select TARGET_TYPE, TARGET_ID from RUNNER_ASSIGNMENT where RUNNER_ID = ?",
                created.getId()))
                .as("a runner scoped to one repository must not be recorded as GLOBAL, which dispatches "
                        + "everything to it")
                .containsEntry("TARGET_TYPE", "REPOSITORY")
                .containsEntry("TARGET_ID", 920L);
    }

    @Test
    void scopeUpdateTakesEffect() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token, "scope defect", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 900L, null, null, now)));
        Long runnerId = created.getId();

        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token, "scope defect", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 901L, null, null, now)));

        Runner reloaded = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(reloaded.getScopeTargetId())
                .as("the MyBatis adapter must honour a scope change, not answer 900 because its "
                        + "update resolved to `where ID = null`")
                .isEqualTo(901L);

        // The same two guards the JPA counterpart carries. Without the first, an implementation that
        // overwrote in place would pass; without the second, the id tiebreak stops being exercised the
        // moment the two writes straddle a second boundary, and nothing says so.
        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("one row for the create, one for the change")
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(distinct ASSIGNED_AT) from RUNNER_ASSIGNMENT where RUNNER_ID = ?",
                Integer.class, runnerId))
                .as("if these ever land in different seconds the tiebreak stops being exercised here "
                        + "and this test quietly weakens")
                .isEqualTo(1);
    }

    @Test
    void reSavingTheSameScopeWritesNoRow() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token, "same scope twice", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 910L, null, null, now)));
        Long runnerId = created.getId();

        // What activate does on every runner restart.
        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token, "same scope twice", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 910L, null, null, now)));

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("a save that changes no scope must write no assignment row, or this table becomes "
                        + "a restart log")
                .isEqualTo(1);
    }

    private static SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/mbg/RunnerEntityMbgMapper.xml"),
                new ClassPathResource("mapper/mbg/RunnerAssignmentEntityMbgMapper.xml")});
        factoryBean.setConfiguration(new org.apache.ibatis.session.Configuration());
        return factoryBean.getObject();
    }
}
