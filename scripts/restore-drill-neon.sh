#!/usr/bin/env bash
# Restore drill for the real setup (Neon Postgres): "an untested backup is not a backup."
#
# Takes a dump of a source database, restores it into a throwaway local PostgreSQL container, and checks that what came
# back is what went in: every table's row count, the migration history, and - the part that matters most for a multi-user
# product - that row-level security policies and FORCE ROW LEVEL SECURITY survived, because a restore that silently drops
# them would show every user everyone's data.
#
#   SOURCE_URL='postgresql://USER:PASSWORD@HOST/DB?sslmode=require' ./scripts/restore-drill-neon.sh
#
# Use a Neon branch made from a past point in time (Neon console -> Branches -> Create branch -> "Past data") as the source
# to rehearse a real point-in-time recovery, or the live branch to prove a fresh dump restores. The source is only read.
# To try it against a PostgreSQL on this machine, use host.docker.internal as the host (the tools run inside Docker).
# Needs only Docker. Nothing is written to the source; the scratch container and the dump are removed afterwards.
set -euo pipefail

: "${SOURCE_URL:?Set SOURCE_URL to the connection string of the database to dump (read-only use).}"

IMAGE="${PG_IMAGE:-postgres:16-alpine}"
SCRATCH="spendwise-restore-drill"
DUMP="$(mktemp -t spendwise-drill.XXXXXX)"
FAILURES=0

cleanup() {
  docker rm -f "${SCRATCH}" >/dev/null 2>&1 || true
  rm -f "${DUMP}"
}
trap cleanup EXIT

source_psql() { docker run --rm "${IMAGE}" psql "${SOURCE_URL}" -At -c "$1"; }
scratch_psql() { docker exec "${SCRATCH}" psql -U postgres -d drill -At -c "$1"; }

echo "1/5 Dumping the source (custom format) ..."
docker run --rm "${IMAGE}" pg_dump "${SOURCE_URL}" --no-owner --no-privileges --format=custom > "${DUMP}"
echo "    dump size: $(wc -c < "${DUMP}" | tr -d ' ') bytes"

echo "2/5 Starting a scratch PostgreSQL ..."
docker rm -f "${SCRATCH}" >/dev/null 2>&1 || true
docker run -d --name "${SCRATCH}" -e POSTGRES_PASSWORD=drill "${IMAGE}" >/dev/null
until docker exec "${SCRATCH}" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done
sleep 2
docker exec "${SCRATCH}" psql -U postgres -c "CREATE DATABASE drill" >/dev/null

echo "3/5 Restoring ..."
docker exec -i "${SCRATCH}" pg_restore -U postgres -d drill --no-owner --no-privileges --exit-on-error < "${DUMP}"

echo "4/5 Comparing row counts, table by table ..."
TABLES="$(scratch_psql "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY 1")"
for table in ${TABLES}; do
  restored="$(scratch_psql "SELECT count(*) FROM \"${table}\"")"
  original="$(source_psql "SELECT count(*) FROM \"${table}\"")"
  if [ "${restored}" = "${original}" ]; then
    printf "    ok       %-28s %s rows\n" "${table}" "${restored}"
  elif [ "${restored}" -lt "${original}" ] 2>/dev/null; then
    # The source kept receiving writes after the dump was taken, so slightly more rows there is expected on a live database.
    printf "    note     %-28s restored %s, source now %s (written since the dump)\n" "${table}" "${restored}" "${original}"
  else
    printf "    MISMATCH %-28s restored %s, source %s\n" "${table}" "${restored}" "${original}"
    FAILURES=$((FAILURES + 1))
  fi
done

echo "5/5 Checking the safety properties survived ..."
check() { # name, source value, restored value
  if [ "$2" = "$3" ]; then echo "    ok       $1 ($3)"; else echo "    MISMATCH $1: source $2, restored $3"; FAILURES=$((FAILURES + 1)); fi
}
check "migrations applied" "$(source_psql "SELECT count(*) FROM flyway_schema_history WHERE success")" "$(scratch_psql "SELECT count(*) FROM flyway_schema_history WHERE success")"
check "row-level security policies" "$(source_psql "SELECT count(*) FROM pg_policies WHERE schemaname='public'")" "$(scratch_psql "SELECT count(*) FROM pg_policies WHERE schemaname='public'")"
check "tables with row security forced" "$(source_psql "SELECT count(*) FROM pg_class WHERE relkind='r' AND relnamespace='public'::regnamespace AND relforcerowsecurity")" "$(scratch_psql "SELECT count(*) FROM pg_class WHERE relkind='r' AND relnamespace='public'::regnamespace AND relforcerowsecurity")"
check "foreign keys" "$(source_psql "SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace")" "$(scratch_psql "SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace")"

echo
if [ "${FAILURES}" -eq 0 ]; then
  echo "RESTORE DRILL PASSED"
else
  echo "RESTORE DRILL FAILED (${FAILURES} problem(s))" >&2
  exit 1
fi
