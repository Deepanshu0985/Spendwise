#!/usr/bin/env bash
# Minimal authenticated API helper for Phase 5 dogfooding - not part of the
# deployed product, just a local convenience for calling the backend directly
# while the frontend is still a skeleton (see DECISIONS.md, "Phase 5:
# dogfooding via direct API calls"). Handles the session cookie + double-submit
# CSRF header (ADR-009) so you don't have to by hand on every call.
set -euo pipefail

BASE_URL="${SPENDWISE_BASE_URL:-http://localhost:8080/api/v1}"
COOKIE_JAR="${SPENDWISE_COOKIE_JAR:-$HOME/.spendwise-cookies}"

csrf_token() {
    grep -w csrf_token "$COOKIE_JAR" >/dev/null | awk '{print $7}'
}

api() {
    local method="$1" path="$2" body="${3:-}"
    curl -sS -c "$COOKIE_JAR" -b "$COOKIE_JAR" -X "$method" "$BASE_URL$path" \
        -H "Content-Type: application/json" \
        -H "X-CSRF-Token: $(csrf_token)" \
        ${body:+-d "$body"}
    echo
}

usage() {
    cat <<EOF
Usage: $0 <command> [args]

  prime                      Fetch a CSRF cookie (run once per fresh cookie jar)
  register '<json>'          POST /auth/register
  login '<json>'             POST /auth/login
  logout                     POST /auth/logout
  get <path>                 GET <path>   e.g. get /accounts
  post <path> '<json>'       POST <path>
  put <path> '<json>'        PUT <path>
  delete <path>               DELETE <path>

Env vars:
  SPENDWISE_BASE_URL    default http://localhost:8080/api/v1
  SPENDWISE_COOKIE_JAR  default ~/.spendwise-cookies
EOF
}

case "${1:-}" in
    prime)
        curl -sS -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE_URL/health" > /dev/null
        echo "primed"
        ;;
    register) api POST /auth/register "$2" ;;
    login) api POST /auth/login "$2" ;;
    logout) api POST /auth/logout ;;
    get) api GET "$2" ;;
    post) api POST "$2" "$3" ;;
    put) api PUT "$2" "$3" ;;
    delete) api DELETE "$2" ;;
    *) usage; exit 1 ;;
esac
