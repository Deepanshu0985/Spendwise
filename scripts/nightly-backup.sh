#!/usr/bin/env bash
# Nightly PostgreSQL backup to S3-compatible object storage (docs/08-devops/deployment.md).
# Intended to run via cron/systemd timer on the production host, not locally.
# Requires: docker compose (to reach the `db` service), aws-cli or s3cmd configured
# for the storage endpoint (STORAGE_ENDPOINT/STORAGE_BUCKET/STORAGE_ACCESS_KEY/STORAGE_SECRET_KEY).
set -euo pipefail

cd "$(dirname "$0")/.."

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DUMP_FILE="/tmp/finance-backup-${TIMESTAMP}.sql.gz"

docker compose exec -T db pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "${DUMP_FILE}"

aws s3 cp "${DUMP_FILE}" "s3://${STORAGE_BUCKET}/backups/$(basename "${DUMP_FILE}")" \
  --endpoint-url "${STORAGE_ENDPOINT}"

rm -f "${DUMP_FILE}"

echo "Backup uploaded: backups/$(basename "${DUMP_FILE}")"

# Bucket lifecycle policy (30-day expiry per deployment.md) is configured on the
# bucket itself, not here - this script only produces and uploads the dump.
