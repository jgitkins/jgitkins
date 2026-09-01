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
-- Idempotent when run serially: MariaDB has no CREATE INDEX IF NOT EXISTS, hence the lookup. The
-- lookup is check-then-act, so two concurrent runs both see 0 and the loser fails with ER_DUP_KEYNAME
-- (1061). That failure is safe to ignore; do not run this concurrently and it will not happen.
--
-- The final SELECT echoes the schema it ran against. The lookup is scoped by DATABASE(), and a schema
-- that also has a RUNNER_ASSIGNMENT table -- a staging copy on the same server, a restored snapshot --
-- would be indexed successfully and silently. Read the echo, do not assume.
--
-- Connect to the right schema first. The lookup is scoped by DATABASE(), so running this on a
-- connection with no database selected, or the wrong one, finds no index and then fails on CREATE
-- INDEX against a table that is not there. ddl.sql declares `USE JGITKINS`, uppercase, so:
--   mariadb -u<user> -p JGITKINS < 2026-09-01-runner-assignment-index.sql
-- It fails loudly rather than silently in that case, which is the behaviour to keep.
--
-- Verified against mariadb:11.4 on 2026-09-01, seeded with the pre-index ddl.sql: first run creates
-- the index, second and third exit 0 and leave it alone, DROP INDEX rolls it back, and a fourth run
-- creates it again. EXPLAIN on `where RUNNER_ID = ? order by ASSIGNED_AT desc, ID desc limit 1` then
-- reports type=ref key=IX_RUNNER_ASSIGNMENT_RUNNER rows=1 with no filesort, so the index serves the
-- sort and not just the lookup.
--
-- Rollback: DROP INDEX IX_RUNNER_ASSIGNMENT_RUNNER ON RUNNER_ASSIGNMENT;
-- Independent of the code -- it runs correctly with or without this index, only slower.
--
-- One hazard the index rollback does not carry but the CODE rollback does: reverting b5a7fcb after any
-- runner has two assignment rows drops the ID tiebreak, and two rows written in the same whole second
-- then order arbitrarily -- the effective scope becomes nondeterministic between reads rather than
-- merely stale. Collapse history first if you ever go back:
--   delete a from RUNNER_ASSIGNMENT a
--     join (select RUNNER_ID, max(ID) keep_id from RUNNER_ASSIGNMENT group by RUNNER_ID) k
--       on a.RUNNER_ID = k.RUNNER_ID and a.ID <> k.keep_id;

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

-- Say what was done and where. A silent success against the wrong schema is the one failure mode the
-- idempotency guard cannot distinguish from the real thing.
SELECT DATABASE() AS applied_to_schema,
       @exists     AS index_existed_before,
       (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'RUNNER_ASSIGNMENT'
           AND INDEX_NAME = 'IX_RUNNER_ASSIGNMENT_RUNNER') AS index_columns_now;
