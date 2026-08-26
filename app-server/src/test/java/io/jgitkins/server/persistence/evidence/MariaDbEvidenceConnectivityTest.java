package io.jgitkins.server.persistence.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * Precondition gate for the MariaDB-compatible evidence that Tasks 2.69-2.77 require.
 *
 * <p>Those tasks reject H2-only evidence, but every other test profile in this module is
 * {@code jdbc:h2:mem:...;MODE=MariaDB}, which is emulation. This test proves the three things a
 * real-database evidence run depends on, against an actual MariaDB server:
 *
 * <pre>
 *   app-server/data/ddl.sql            (13 CREATE TABLE statements)
 *            |
 *            v
 *   MariaDB @ 127.0.0.1:53306          <- docker-compose.local.yml override
 *            |
 *            +--> server is really MariaDB, not H2 in MariaDB mode
 *            +--> all 13 MBG-referenced tables exist
 *            +--> org.mariadb.jdbc.Driver connects and round-trips a query
 * </pre>
 *
 * <p>When the database is not running the test is skipped, not failed: {@code ./gradlew test} must
 * stay green on a machine that has not brought the container up. A skip therefore means "no MariaDB
 * evidence was produced", which is exactly the BLOCKED state those tasks describe. Only a pass may
 * be recorded as evidence.
 */
class MariaDbEvidenceConnectivityTest {

    private static final String URL = "jdbc:mariadb://127.0.0.1:53306/JGITKINS";
    private static final String USER = "root";
    private static final String PASSWORD = "root1234";

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

    private static Connection open() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            return null;
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void realMariaDbIsReachableAndCarriesTheFullAppServerSchema() throws SQLException {
        try (Connection connection = open()) {
            assumeTrue(
                    connection != null,
                    "MariaDB is not reachable at " + URL + " -- MariaDB evidence is BLOCKED, not produced. "
                            + "Bring it up with the docker-compose.local.yml override, then load "
                            + "app-server/data/ddl.sql.");

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
