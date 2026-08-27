package io.jgitkins.server.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.77 placement audit: every JPA mapping added by tasks 2.70-2.76 addresses a table and columns
 * that actually exist in the schema.
 *
 * <p>What this adds over the per-slice MariaDB tests, stated precisely rather than generously. It does
 * <em>not</em> catch columns those tests cannot see — Hibernate selects every mapped column, so any typo
 * breaks the first read and some per-slice test does read every entity today. Verified by negative
 * control: renaming one column made this test fail <em>and</em> three repository-slice tests fail.
 *
 * <p>Three things it does add:
 *
 * <ul>
 *   <li><strong>A column-precise diagnostic.</strong> This test says which table and which column. The
 *       slice tests say {@code Database operation failed during branch creation}, wrapped over a
 *       Hibernate error, in a stack that points at the adapter rather than the mapping.</li>
 *   <li><strong>Uniform coverage that does not depend on someone writing a slice test.</strong> The next
 *       entity added — task 2.67's placement work, or a context after it — is audited whether or not it
 *       arrives with its own MariaDB test. That is the case this exists for, since it is the one that
 *       will actually happen.</li>
 *   <li><strong>A check against the checked-in DDL, not the local database.</strong> A table that exists
 *       only in someone's MariaDB passes every per-slice test and breaks a fresh environment.
 *       {@code everyMappedTableIsDeclaredInTheCheckedInSchema} is the only assertion in the suite that
 *       looks at {@code app-server/data/ddl.sql}.</li>
 * </ul>
 *
 * <p>Discovered by walking the source, not from a list, for the same reason as
 * {@link FinalSelectorClosureTest}: a list would go stale on the next migrated context and the test
 * would keep passing while auditing less.
 */
class FinalMariaDbPlacementTest {

    private static final Path MAIN = Paths.get("src/main/java/io/jgitkins/server");
    private static final Path REPOSITORY_ROOT = Paths.get("..").toAbsolutePath().normalize();

    @Test
    void placementPassesAgainstMariaDb() throws Exception {
        assumeTrue(JpaMariaDbTestSupport.mariaDbReachable(),
                "MariaDB is not reachable at " + JpaMariaDbTestSupport.URL
                        + " -- final placement is UNVERIFIED, not satisfied.");

        Map<String, Set<String>> mappedColumnsByTable = mappedColumnsByTable();
        assertThat(mappedColumnsByTable)
                .as("no @Entity was discovered under app-server; tasks 2.70-2.76 added several, so the "
                        + "scan is broken rather than the code")
                .isNotEmpty();

        Map<String, Set<String>> missingColumns = new LinkedHashMap<>();
        List<String> missingTables = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(
                JpaMariaDbTestSupport.URL, JpaMariaDbTestSupport.USER, JpaMariaDbTestSupport.PASSWORD)) {

            for (Map.Entry<String, Set<String>> entry : mappedColumnsByTable.entrySet()) {
                Set<String> actual = columnsOf(connection, entry.getKey());
                if (actual.isEmpty()) {
                    missingTables.add(entry.getKey());
                    continue;
                }
                Set<String> absent = new TreeSet<>(entry.getValue());
                absent.removeAll(actual);
                if (!absent.isEmpty()) {
                    missingColumns.put(entry.getKey(), absent);
                }
            }
        }

        assertThat(missingTables)
                .as("these tables are mapped by a JPA entity and do not exist in the live schema. With "
                        + "ddl-auto set to none this surfaces as a context-initialisation failure at "
                        + "deploy time, not at build time.")
                .isEmpty();

        assertThat(missingColumns)
                .as("these columns are mapped by a JPA entity and do not exist in the live table. A "
                        + "per-slice test only covers the columns it reads or writes, so a typo in an "
                        + "untouched column stays invisible until some query selects it — in whichever "
                        + "environment runs that path first.")
                .isEmpty();
    }

    @Test
    void everyMappedTableIsDeclaredInTheCheckedInSchema() throws IOException {
        // Independent of the database: the DDL in the repository is what a fresh environment gets, so a
        // table that exists only in someone's local MariaDB would pass the test above and fail on a new
        // deployment.
        String ddl = Files.readString(REPOSITORY_ROOT.resolve("app-server/data/ddl.sql"))
                .toUpperCase(Locale.ROOT);

        List<String> undeclared = mappedColumnsByTable().keySet().stream()
                .filter(table -> !ddl.contains("CREATE TABLE `" + table + "`"))
                .sorted()
                .toList();

        assertThat(undeclared)
                .as("these tables are mapped by a JPA entity but are not created by "
                        + "app-server/data/ddl.sql. They may exist in a local database and would still "
                        + "break a fresh environment.")
                .isEmpty();
    }

    private static Map<String, Set<String>> mappedColumnsByTable() throws IOException {
        Map<String, Set<String>> byTable = new LinkedHashMap<>();
        for (Class<?> entity : entityClasses()) {
            Table table = entity.getAnnotation(Table.class);
            if (table == null || table.name().isBlank()) {
                continue;
            }
            Set<String> columns = byTable.computeIfAbsent(
                    table.name().toUpperCase(Locale.ROOT), key -> new LinkedHashSet<>());
            for (Field field : entity.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null && !column.name().isBlank()) {
                    columns.add(column.name().toUpperCase(Locale.ROOT));
                }
            }
        }
        return byTable;
    }

    private static List<Class<?>> entityClasses() throws IOException {
        List<Class<?>> entities = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("JpaEntity.java"))
                    .sorted()
                    .toList()) {
                String className = "io.jgitkins.server."
                        + MAIN.relativize(file).toString()
                                .replace(".java", "")
                                .replace('/', '.');
                try {
                    Class<?> loaded = Class.forName(className);
                    if (loaded.getAnnotation(jakarta.persistence.Entity.class) != null) {
                        entities.add(loaded);
                    }
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "found " + file + " but could not load " + className
                                    + "; the scan and the classpath disagree, which would silently "
                                    + "shrink this audit", e);
                }
            }
        }
        return entities;
    }

    private static Set<String> columnsOf(Connection connection, String table) throws Exception {
        Set<String> columns = new LinkedHashSet<>();
        String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS "
                + "where TABLE_SCHEMA = database() and TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    columns.add(rows.getString(1).toUpperCase(Locale.ROOT));
                }
            }
        }
        return columns;
    }
}
