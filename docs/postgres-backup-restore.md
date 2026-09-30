# PostgreSQL Backup and Restore

Use PostgreSQL client tools matching the production server's major version. Store credentials in a protected `.pgpass`/`pgpass.conf`; do not place passwords in commands or repository files.

## Backup

Create a compressed, restorable backup:

```powershell
$pgUrl = $env:DB_URL -replace '^jdbc:', ''
pg_dump `
  --dbname="$pgUrl" `
  --username="$env:DB_USERNAME" `
  --format=custom `
  --no-owner `
  --no-privileges `
  --file="supplog_$(Get-Date -Format yyyyMMdd_HHmmss).dump"
```

Keep daily backups outside the application host, encrypt them at rest, define retention, and monitor job failures. Record the generated file size and SHA-256 checksum.

## Restore Drill

Always restore into a new, empty verification database first. `RESTORE_DB_URL`
must use libpq format (for example `postgresql://localhost:5432/supplog_restore_check`),
must not point to production, and should be created before this command:

```powershell
pg_restore `
  --exit-on-error `
  --clean `
  --if-exists `
  --no-owner `
  --no-privileges `
  --username="$env:DB_USERNAME" `
  --dbname="$env:RESTORE_DB_URL" `
  "<backup-file.dump>"
```

After restoration, verify Flyway history, core row counts, the final active admin, and application startup using the restored database. Never run `--clean` against the production database.

## Release Requirement

Before V1 release, complete one successful restore drill, record its duration, and confirm that the restored application can authenticate and read routines/execution history.
