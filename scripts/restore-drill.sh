#!/usr/bin/env bash
# Monthly restore drill (docs/08-devops/deployment.md): "an untested backup is not a backup."
# Restores the latest dump into a scratch container, separate from the real database,
# and checks it. Intended to run on the production host or a matching environment.
set -euo pipefail

cd "$(dirname "$0")/.."

SCRATCH_CONTAINER="finance-restore-drill"
SCRATCH_PORT=55432

echo "Fetching latest backup from s3://${STORAGE_BUCKET}/backups/ ..."
LATEST_KEY=$(aws s3 ls "s3://${STORAGE_BUCKET}/backups/" --endpoint-url "${STORAGE_ENDPOINT}" \
  | sort | tail -n1 | awk '{print $4}')

if [ -z "${LATEST_KEY}" ]; then
  echo "No backup found. Failing the drill." >&2
  exit 1
fi

DUMP_FILE="/tmp/${LATEST_KEY}"
aws s3 cp "s3://${STORAGE_BUCKET}/backups/${LATEST_KEY}" "${DUMP_FILE}" --endpoint-url "${STORAGE_ENDPOINT}"

echo "Starting scratch PostgreSQL container ..."
docker rm -f "${SCRATCH_CONTAINER}" >/dev/null 2>&1 || true
docker run -d --name "${SCRATCH_CONTAINER}" \
  -e POSTGRES_DB="${POSTGRES_DB}" -e POSTGRES_USER="${POSTGRES_USER}" -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  -p "${SCRATCH_PORT}:5432" postgres:16-alpine >/dev/null

echo "Waiting for scratch database to accept connections ..."
until docker exec "${SCRATCH_CONTAINER}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; do
  sleep 1
done

echo "Restoring dump ..."
gunzip -c "${DUMP_FILE}" | docker exec -i "${SCRATCH_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

echo "Row counts:"
docker exec "${SCRATCH_CONTAINER}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "
  SELECT schemaname, relname, n_live_tup
  FROM pg_stat_user_tables
  ORDER BY relname;
"

# TODO (Phase 4): once the reconciliation invariant tests exist
# (docs/02-architecture/analytics-specification.md), point a test run at this scratch
# database - e.g. DATABASE_URL=jdbc:postgresql://localhost:${SCRATCH_PORT}/${POSTGRES_DB}
# mvn -f backend/pom.xml test -Dtest=ReconciliationInvariantsTest -Dspring.profiles.active=restore-drill
# and fail this script if that run fails.

docker rm -f "${SCRATCH_CONTAINER}" >/dev/null
rm -f "${DUMP_FILE}"

echo "Restore drill complete."
