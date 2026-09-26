#!/usr/bin/env bash
# End-to-end smoke flow on top of spendwise-cli.sh: log in (registering first
# if the account doesn't exist yet), add a bank account, log a couple of
# transactions, then check this month's summary and the transaction list.
# Not part of the deployed product - a Phase 5 convenience for quickly
# verifying the whole loop still works, same spirit as spendwise-cli.sh
# (see DECISIONS.md, "Phase 5: dogfooding via direct API calls").
#
# Usage: ./spendwise-demo.sh <email> <password> [fullName]
#
# Re-running this creates a fresh "Demo Bank" account and two more
# transactions each time - it does not try to dedupe against a previous run.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLI="$SCRIPT_DIR/spendwise-cli.sh"

EMAIL="${1:?Usage: $0 <email> <password> [fullName]}"
PASSWORD="${2:?Usage: $0 <email> <password> [fullName]}"
FULL_NAME="${3:-Demo User}"

json_field() {
    python3 -c "import sys,json; print(json.load(sys.stdin)['data']['$1'])"
}

echo "== priming CSRF cookie =="
"$CLI" prime

echo "== logging in =="
LOGIN_RESULT=$("$CLI" login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
if echo "$LOGIN_RESULT" | grep -q '"code":"UNAUTHORIZED"'; then
    echo "   no account yet - registering $EMAIL"
    "$CLI" register "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"$FULL_NAME\"}"
    LOGIN_RESULT=$("$CLI" login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
fi
echo "$LOGIN_RESULT"

echo "== adding an account =="
ACCOUNT=$("$CLI" post /accounts '{"name":"Demo Bank","accountType":"BANK","currency":"INR"}')
echo "$ACCOUNT"
ACCOUNT_ID=$(echo "$ACCOUNT" | json_field id)

TODAY="$(date +%Y-%m-%d)"

echo "== logging a couple of transactions =="
"$CLI" post /transactions "{\"accountId\":\"$ACCOUNT_ID\",\"transactionDate\":\"$TODAY\",\"amount\":250,\"currency\":\"INR\",\"description\":\"Lunch\",\"transactionType\":\"EXPENSE\"}"
"$CLI" post /transactions "{\"accountId\":\"$ACCOUNT_ID\",\"transactionDate\":\"$TODAY\",\"amount\":50000,\"currency\":\"INR\",\"description\":\"Salary\",\"transactionType\":\"INCOME\"}"

FROM="$(date -v1d +%Y-%m-%d 2>/dev/null || date -d "$(date +%Y-%m-01)" +%Y-%m-%d)"
TO="$(date -v1d -v+1m -v-1d +%Y-%m-%d 2>/dev/null || date -d "$(date +%Y-%m-01) +1 month -1 day" +%Y-%m-%d)"

echo "== checking this month's summary ($FROM to $TO) =="
"$CLI" get "/analytics/monthly?from=$FROM&to=$TO"

echo "== listing transactions on the new account =="
"$CLI" get "/transactions?accountId=$ACCOUNT_ID"
