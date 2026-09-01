-- Index for the runner assignment read path.
--
-- RunnerPersistenceAdapter#fetchAssignment and RunnerAssignmentJpaRepository both resolve a runner's
-- effective scope with `where RUNNER_ID = ? order by ASSIGNED_AT desc, ID desc limit 1`, and
-- RUNNER_ASSIGNMENT carried only PRIMARY KEY (ID). The table did not grow while the update branch
-- wrote no assignment row; it does now, one row per real scope change, so the sort needs an index
-- rather than a table scan. InnoDB appends the primary key to a secondary index, so (RUNNER_ID,
-- ASSIGNED_AT) covers the ID tiebreak too.
--
-- Why this file exists at all: this repository has no Flyway or Liquibase and runs with
-- spring.jpa.hibernate.ddl-auto: none. app-server/data/ddl.sql is a mariadb-dump that Testcontainers
-- loads (app-server/build.gradle, jgitkins.test.ddl), so editing it reaches fresh databases and the
-- test suite and nothing else. An existing database needs this run by hand.
--
-- Idempotent: safe to run twice. MariaDB has no CREATE INDEX IF NOT EXISTS, hence the lookup.
--
-- Rollback: DROP INDEX IX_RUNNER_ASSIGNMENT_RUNNER ON RUNNER_ASSIGNMENT;
-- Independent of the code -- it runs correctly with or without this index, only slower.

SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'RUNNER_ASSIGNMENT'
      AND INDEX_NAME = 'IX_RUNNER_ASSIGNMENT_RUNNER');

SET @sql := IF(@exists = 0,
    'CREATE INDEX IX_RUNNER_ASSIGNMENT_RUNNER ON RUNNER_ASSIGNMENT (RUNNER_ID, ASSIGNED_AT)',
    'DO 0');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
