# data/migrations

Hand-applied schema and data changes. There is no migration tool in this repository: no Flyway, no
Liquibase, and `spring.jpa.hibernate.ddl-auto: none`. `app-server/data/ddl.sql` is a `mariadb-dump`
that Testcontainers loads (`jgitkins.test.ddl`, `app-server/build.gradle:122`), so editing it reaches
fresh test databases and nothing else. Every existing database is updated by running these files by
hand.

## Rules

- **Filename is `YYYY-MM-DD-short-description.sql`, and the date orders application.** Two files with
  the same date are independent; if they are not, merge them.
- **Every file must be idempotent, or say plainly that it is not** and what happens on a second run.
- **Every file states its own rollback**, or says that none is needed and why.
- **Every file names the schema it expects.** `ddl.sql` declares `USE JGITKINS`, uppercase, so:
  `mariadb -u<user> -p JGITKINS < <file>.sql`. A lookup scoped by `DATABASE()` will succeed silently
  against any other schema that happens to have the same table.
- **A file that changes data rather than schema explains why it cannot be automatic**, if it cannot.

## Applied state is not tracked

Nothing records which files have run against which database. To answer "is this applied to staging?"
you query the schema — each file says what to look for. If this directory outgrows that, the fix is a
`SCHEMA_MIGRATION(FILENAME PRIMARY KEY, APPLIED_AT)` table in `ddl.sql` that each file inserts into,
not a convention nobody follows.

## Files

| File | What | Idempotent |
|---|---|---|
| `2026-09-01-runner-assignment-index.sql` | Index for the runner-scope read path | Yes, serially |
| `2026-09-01-runner-scope-global-audit.sql` | Reports runners wrongly recorded GLOBAL; correction is manual | Read-only (step 1) |
