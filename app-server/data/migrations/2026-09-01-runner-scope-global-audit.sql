-- Runner scope audit. Read this before running anything in it.
--
-- WHAT HAPPENED
--
-- Until b5a7fcb, RunnerPersistenceAdapter's create branch built the RUNNER_ASSIGNMENT row from
-- restoreRunner(entity) -- an aggregate reconstructed by reading a database that has no assignment row
-- yet, so restoreRunner returned its GLOBAL fallback. Every runner created through the MyBatis provider
-- was therefore recorded as TARGET_TYPE='GLOBAL', TARGET_ID=NULL no matter which organization or
-- repository the caller scoped it to. The method returned the scope the caller asked for, so nothing
-- upstream could see the discrepancy, and it could not be observed downstream either: the update branch
-- was a no-op (`where ID = null`), so a runner created GLOBAL stayed GLOBAL.
--
-- The read path now honours the stored row. It always did -- restoreRunner/RunnerDispatchContextResolver
-- feed TARGET_TYPE straight into JobDispatchScope -- which means those rows have been deciding dispatch
-- all along. A runner recorded GLOBAL accepts jobs for every repository in the installation.
--
-- WHY THERE IS NO AUTOMATIC BACKFILL
--
-- The intended scope is not recoverable from the database. RUNNER has no scope columns (ID, TOKEN,
-- DESCRIPTION, STATUS, IP_ADDRESS, LAST_HEARTBEAT_AT, CREATED_AT), so the requested TARGET_TYPE and
-- TARGET_ID were discarded at insert time and exist nowhere -- not in RUNNER, not in a log
-- (RunnerManagementService#register logs only the id). A script cannot infer what a runner was meant
-- to be scoped to. Correcting a row is an operator decision informed by whatever record exists outside
-- this database: the registration request, a ticket, or the person who created the runner.
--
-- A runner legitimately created as GLOBAL is indistinguishable from one wrongly recorded as GLOBAL.
-- That is the honest state of the data and this file does not pretend otherwise.

-- STEP 1 -- see the exposure. Read-only.
--
-- Every runner whose current effective scope is GLOBAL, plus the ones with no assignment row at all
-- (also GLOBAL: that is restoreRunner's documented fallback, load-bearing for runners created before
-- assignments existed). DESCRIPTION is included because it is usually the only clue to intent.
SELECT DATABASE()                                        AS schema_checked,
       r.ID                                              AS runner_id,
       r.DESCRIPTION                                     AS description,
       r.STATUS                                          AS status,
       r.CREATED_AT                                      AS created_at,
       COALESCE(a.TARGET_TYPE, 'GLOBAL (no assignment row)') AS effective_scope,
       a.TARGET_ID                                       AS target_id
FROM RUNNER r
LEFT JOIN RUNNER_ASSIGNMENT a
       ON a.ID = (SELECT a2.ID FROM RUNNER_ASSIGNMENT a2
                   WHERE a2.RUNNER_ID = r.ID
                   ORDER BY a2.ASSIGNED_AT DESC, a2.ID DESC
                   LIMIT 1)
WHERE a.TARGET_TYPE = 'GLOBAL' OR a.ID IS NULL
ORDER BY r.ID;

-- STEP 2 -- correct one runner, once you know what it should be. NOT run by this file.
--
-- Append; do not update in place. The read path takes the newest row by (ASSIGNED_AT, ID), both
-- providers append, and appending leaves the wrong row visible as history instead of erasing it.
-- Uncomment, fill in, run one statement per runner:
--
--   INSERT INTO RUNNER_ASSIGNMENT (RUNNER_ID, TARGET_TYPE, TARGET_ID, ASSIGNED_AT)
--   VALUES (<runner_id>, 'REPOSITORY', <repository_id>, current_timestamp());
--
--   INSERT INTO RUNNER_ASSIGNMENT (RUNNER_ID, TARGET_TYPE, TARGET_ID, ASSIGNED_AT)
--   VALUES (<runner_id>, 'ORGANIZE', <organize_id>, current_timestamp());
--
-- TARGET_TYPE must be one of GLOBAL, ORGANIZE, REPOSITORY (RunnerScopeType). TARGET_ID must be NULL for
-- GLOBAL and non-NULL for the other two -- both adapters normalize it that way on write, and a GLOBAL
-- row carrying a target id would make the change-detection compare against a value the write path would
-- never produce, so the runner would append a row on every restart.
--
-- Verify with STEP 1 again: the runner should drop out of the result.

-- STEP 3 -- why this cannot be fixed through the API today, and what that means for STEP 2.
--
-- There is no scope-update endpoint. RunnerController exposes POST /api/runners, GET /api/runners,
-- GET /api/runners/{id}, DELETE /api/runners/{id}, POST /api/runners/activate, and there is no
-- RunnerScopeUpdateUseCase -- runnerRepository.save is reached only by register (create branch) and
-- activate (which reads the scope back from the stored row and saves it unchanged, so the append never
-- fires). Task 2.89 covers exposing the missing path.
--
-- Consequence: hand-written SQL is the only correction, and delete-and-re-register is not an
-- alternative -- it mints a new token and the deployed runner stops authenticating.
--
-- Rollback: none needed. STEP 1 is read-only and STEP 2 appends rows whose effect is undone by
-- appending a further row. Nothing here drops or rewrites existing data.
