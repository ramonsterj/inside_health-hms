# Database Migrations Guide

How Flyway is wired up in this repo, where things live, and the non-obvious
rules that have bitten us. For Flyway's general semantics (versioning,
checksums, repeatable migrations) refer to the official
[Flyway documentation](https://documentation.red-gate.com/fd) — this guide
only covers what's specific to this codebase.

---

## Two locations, two purposes

```
api/src/main/resources/db/
├── migration/   ← versioned migrations (Vxxx__*.sql / .kt) — run in EVERY profile
└── seed/        ← repeatable seeds   (R__seed_*.sql)       — run in dev/acceptance ONLY
```

| Profile      | Loads `db/migration` | Loads `db/seed` | Notes |
|--------------|----------------------|-----------------|-------|
| _(default)_ / `prod` | yes                  | **no**          | `application.yml` sets `flyway.locations: classpath:db/migration` only. |
| `dev`        | yes                  | yes             | Adds `db/seed`, `out-of-order: true`, `ignore-migration-patterns: "*:missing"`. Also runs `DevFlywayConfig.flywayMigrationStrategy` which calls `flyway.repair()` before `migrate()` — auto-clears FAILED entries. |
| `acceptance` | yes                  | yes             | Same as dev minus the auto-repair. |

**Rule of thumb:** if it must exist in production, it goes under `db/migration`.
If it's test data, dev fixtures, or anything destructive (truncates), it goes
under `db/seed`.

---

## Versioned migrations (`db/migration/Vxxx__*`)

### Naming

`V{nnn}__{snake_case_description}.sql` (or `.kt` for `BaseJavaMigration`
subclasses in `api/src/main/kotlin/db/migration/`).

- `nnn` is the next free integer (see the current high-water mark in
  `CLAUDE.md`'s "Current migrations" line — at time of writing, V113).
- Numbers can have gaps; we currently skip V081–V083 and V095. Don't reuse
  numbers, and don't renumber existing files.

### SQL vs Java — prefer SQL

Use a Java migration **only** when you need procedural logic (parsing,
hashing, classpath I/O, calling out to a Kotlin service). Examples in this
repo: V105 (calls `MedicationNameParser`).

Everything else should be SQL, including bulk data loads — V111 originally
shipped as a Kotlin loader that read a CSV from the classpath; the CSV was
never committed and the loader failed startup on every fresh DB until we
flattened it into a pure SQL migration. The data was already in SQL form in
`R__seed_02b`. **If the data is checkable into SQL, write it in SQL.**

### Schema conventions

Every new domain table inherits the `BaseEntity` contract:

```sql
CREATE TABLE foo (
    id BIGSERIAL PRIMARY KEY,
    -- your columns…
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_foo_deleted_at ON foo(deleted_at);
```

Also index every FK and every frequently-queried column. See CLAUDE.md
section 7 for the full entity-side contract.

### Modifying an already-applied migration

Don't. Flyway records the checksum at apply time; changing the file body
will fail subsequent migrate runs with a checksum mismatch on every
environment that already ran it. If you need to alter prior behavior, write
a follow-up versioned migration (V099 → V100, etc.). The one exception is
when the original migration has **never** successfully run anywhere — V111
is a recent example where we replaced the file body in place because every
prior attempt rolled back.

V110 is a one-off recovery edit, not a precedent. It was rewritten because
the demo DB hit real clinical references before the original abort-loudly
script could succeed. Any environment that already recorded the old V110 as
successful must run `flyway repair` once against that database after deploying
the rewritten script so Flyway updates the stored checksum; environments with
a failed V110 row should clear that row as described in "Recovering from a
failed migration" and then rerun migrate.

---

## Repeatable seeds (`db/seed/R__seed_*`)

These run only in dev/acceptance and rerun whenever **the file's own
checksum** changes.

### The seed-bundle versioning rule (READ THIS)

`R__seed_01_reset_and_base.sql` TRUNCATEs almost everything. The sibling
files `R__seed_02..07` re-insert the data that 01 wiped. Flyway re-runs a
repeatable migration only when **its own** checksum changes — so editing
only `R__seed_01` wipes data without re-running the siblings that
repopulate it (we hit this in PR #53).

The authoritative rule lives in `R__seed_01`'s header. The convention:
each seed file has a line like

```sql
-- SEED-BUNDLE-VERSION: 2026-05-19a
```

When you edit **any** `R__seed_*.sql`, bump this line in **all eight**
files (01, 02, 02b, 03, 04, 05, 06, 07) to the same value. That forces
the whole bundle to re-run together.

### dev vs prod data divergence

`R__seed_02b_pharmacy_from_workbook.sql` mirrors V111 but with dev-only
overrides (`quantity = 50` instead of 0, synthetic seed lots). The
production catalog stays empty; the dev catalog ships pre-stocked for QA.
Pattern: when prod and dev need different seed values, V… writes the prod
shape and R__seed_… writes the dev override on top, with a comment in both
files cross-referencing the other.

---

## Backfills

When a schema change requires data migration on existing rows:

- **Pure SQL backfill** → put it in the same `Vxxx__*.sql` file that adds
  the column. Example: V100 backfills `kind` from category names in the
  same file that adds the column.
- **Procedural backfill** (parsing, hashing) → a follow-up Java migration.
  Example: V105 parses legacy medication name strings via
  `MedicationNameParser` to populate `medication_details`.

Backfills must be **idempotent enough that re-running the bundle on a
half-migrated DB doesn't break.** When in doubt, guard with `WHERE NOT
EXISTS (…)` or `ON CONFLICT DO NOTHING`.

---

## Before-you-add checklist

Lessons from real incidents, in order of how often they bite:

1. **Run the app first.** If you're about to modify a migration chain,
   confirm `./gradlew bootRun` actually starts cleanly before your change.
   A failure after your edit is ambiguous; a failure before it is a fact.
2. **Grep for references.** Java migrations are referenced by class name
   (e.g. from integration tests that re-invoke the loader). SQL migrations
   referenced by classpath path are harder to find but still possible —
   `grep -r 'Vnnn__'`.
3. **Check the bundle version.** If you touched any `R__seed_*.sql`, bump
   `SEED-BUNDLE-VERSION` in all eight files.
4. **Test on a fresh DB.** `docker compose down -v && docker compose up`
   (or your equivalent). Many migration bugs only surface on first apply.
5. **Update CLAUDE.md.** The "Current migrations" line in CLAUDE.md is the
   index of record. Append a short clause for your new Vxxx.

---

## Recovering from a failed migration

### In dev

`DevFlywayConfig` calls `flyway.repair()` before every `migrate()` — FAILED
entries are wiped automatically. Just fix the script and restart.

### In acceptance / prod

`flyway.repair()` does not run automatically. If a versioned migration
failed and left a `success=false` row, you have to clear it manually:

```sql
SELECT version, success, type, script
FROM flyway_schema_history
WHERE success = FALSE;

-- if the row is the one you expect:
DELETE FROM flyway_schema_history
WHERE version = '<nnn>' AND success = FALSE;
```

Failed Java migrations whose body threw inside a transaction usually leave
**no** row (PostgreSQL rolls back the history insert with everything else).
The `DELETE` is only needed for non-transactional DDL failures.

---

## Related files

- `api/src/main/resources/application.yml` — default Flyway config.
- `api/src/main/resources/application-dev.yml` — dev profile overrides.
- `api/src/main/resources/application-acceptance.yml` — acceptance profile.
- `api/src/main/kotlin/com/insidehealthgt/hms/config/DevFlywayConfig.kt` —
  dev auto-repair strategy.
- `api/src/main/resources/db/seed/R__seed_01_reset_and_base.sql` —
  authoritative seed-bundle rule in the header.
- `CLAUDE.md` § 7 "Migration Files (Flyway)" — entity-side contract and
  the rolling roster of every applied migration.
