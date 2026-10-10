#!/usr/bin/env bash
# Keeps a sleeping free-tier backend awake by calling its health endpoint on a timer.
#
# Usage:
#   scripts/keep-awake.sh [URL] [INTERVAL_SECONDS]
#
# Defaults: the live (main) backend, every 600 seconds (10 minutes).
#
# Why not every 10 seconds: Render's free tier sleeps a service after about 15 minutes with no request, so anything
# under ~14 minutes keeps it awake. Pinging more often changes nothing except the logs. A 5-second floor is enforced.
#
# Two limits to know about:
#   1. This only runs while this terminal is open and the computer is awake. For "always", use a hosted pinger.
#   2. Render gives free services 750 hours a month for the whole workspace. One service awake all month uses ~720.
#      Keeping BOTH staging and main awake would go over and Render would suspend free services until next month.
#      Keep only the one you need awake (main), and let staging sleep.
#
# Stop with Ctrl-C.

set -u

URL="${1:-https://spendwise-backend-6e3g.onrender.com/api/v1/health}"
INTERVAL="${2:-600}"
MIN_INTERVAL=5

if ! [[ "$INTERVAL" =~ ^[0-9]+$ ]] || [ "$INTERVAL" -lt "$MIN_INTERVAL" ]; then
  echo "Interval must be a whole number of seconds, at least $MIN_INTERVAL." >&2
  exit 1
fi

RESULT_FILE="$(mktemp)"
CHILD=""

# bash only runs a trap between foreground commands, so the slow commands run in the background and are waited on:
# that lets a stop request take effect at once instead of after a 2-minute request finishes.
cleanup() {
  [ -n "$CHILD" ] && kill "$CHILD" 2>/dev/null
  rm -f "$RESULT_FILE"
  echo
  echo "Stopped."
  exit 0
}
trap cleanup INT TERM

echo "Calling $URL every ${INTERVAL}s. Press Ctrl-C to stop."
while true; do
  # A sleeping service can take 60-90 seconds to answer its first request, hence the long timeout.
  curl -s -o /dev/null --max-time 120 -w "%{http_code} %{time_total}" "$URL" > "$RESULT_FILE" 2>/dev/null &
  CHILD=$!
  wait "$CHILD" || true
  result="$(cat "$RESULT_FILE" 2>/dev/null)"
  [ -n "$result" ] || result="000 0"
  code="${result%% *}"
  seconds="${result##* }"
  if [ "$code" = "200" ]; then
    status="ok"
  else
    status="NOT OK"
  fi
  printf '%s  HTTP %s  %.1fs  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$code" "$seconds" "$status"
  sleep "$INTERVAL" &
  CHILD=$!
  wait "$CHILD" || true
done
