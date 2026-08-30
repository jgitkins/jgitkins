package io.jgitkins.server.persistence.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Proves the suite's MariaDB is a real MariaDB carrying the whole checked-in schema.
 *
 * <p>Every other test profile in this module is {@code jdbc:h2:mem:...;MODE=MariaDB}, which is
 * emulation. Tasks 2.69-2.77 reject H2-only evidence, so something has to establish that the
 * database the JPA slices talk to is the real engine and has all thirteen tables — otherwise each
 * slice test independently assumes it.
 *
 * <pre>
 *   app-server/data/ddl.sql            (13 CREATE TABLE statements)
 *            |
 *            v
 *   mariadb:11.4 via Testcontainers    &lt;- JpaMariaDbTestSupport, one per JVM
 *            |
 *            +--&gt; server is really MariaDB, not H2 in MariaDB mode
 *            +--&gt; all 13 MBG-referenced tables exist
 *            +--&gt; org.mariadb.jdbc.Driver connects and round-trips a query
 * </pre>
 *
 * <p><strong>What task 2.103 changed about why this test exists.</strong> It used to be a
 * <em>precondition gate</em>: it pointed at {@code 127.0.0.1:53306}, and when nothing was listening
 * it skipped, which was the documented BLOCKED state. That reading depended on someone looking at
 * the skip. Nobody did — no verify workflow declared a {@code services:} block, so in CI this and
 * twenty-one others skipped on every run and Gradle printed BUILD SUCCESSFUL anyway.
 *
 * <p>Now the container is started by {@link JpaMariaDbTestSupport} and this test can no longer be
 * skipped, so it stops being a gate and becomes an assertion about the container: that
 * {@code ddl.sql} really was executed by the entrypoint, and executed completely. That is a claim
 * worth its own test, because the failure it catches is silent — a dump the entrypoint refuses
 * halfway leaves a container that starts, accepts connections, and is missing tables. Every JPA
 * slice would then fail somewhere deep in Hibernate; this one says which tables are absent.
 */
class MariaDbEvidenceConnectivityTest {

    private static final String URL = JpaMariaDbTestSupport.URL;
    private static final String USER = JpaMariaDbTestSupport.USER;
    private static final String PASSWORD = JpaMariaDbTestSupport.PASSWORD;

    /** Tables every app-server MBG mapper XML resolves against. */
    private static final List<String> REQUIRED_TABLES = List.of(
            "BRANCH",
            "JOB",
            "JOB_HISTORY",
            "ORGANIZE",
            "ORGANIZE_MEMBER",
            "PULL_REQUEST",
            "REPOSITORY",
            "REPOSITORY_MEMBER",
            "RUNNER",
            "RUNNER_ASSIGNMENT",
            "USER",
            "USER_CREDENTIALS",
            "USER_IDENTITIES");

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void realMariaDbIsReachableAndCarriesTheFullAppServerSchema() throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.getDatabaseProductName())
                    .as("evidence must come from a real MariaDB server, not H2 in MariaDB mode")
                    .isEqualTo("MariaDB");

            Set<String> present = new TreeSet<>();
            try (Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery(
                            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'JGITKINS'")) {
                while (rs.next()) {
                    present.add(rs.getString(1).toUpperCase());
                }
            }
            assertThat(present)
                    .as("app-server/data/ddl.sql must be loaded before any MariaDB evidence run")
                    .containsAll(REQUIRED_TABLES);

            try (Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }
}
