#!/usr/bin/env bash
# End-to-end smoke test against a running fraud-service.
#
#   scripts/smoke.sh <base-url> [username] [password]
#   scripts/smoke.sh http://localhost:8080 demo demo123
#
# Proves the whole chain Java -> Python model -> MySQL, not just that the app is up:
#   1. /actuator/health reports db and modelService UP
#   2. a single prediction is scored and stored (returns an id)
#   3. that id is visible in the caller's history
#   4. the bundled sample CSV is scored as a batch (8 rows, 4 flagged)
# Used by CI (docker compose), by local verification of an AWS deployment, and by the
# demo-tier deploy workflow. Exit code 0 = all checks passed.
set -euo pipefail

BASE="${1:?usage: smoke.sh <base-url> [user] [pass]}"
USER="${2:-demo}"
PASS="${3:-demo123}"
BASE="${BASE%/}"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass() { echo "  ok   $1"; }
fail() { echo "  FAIL $1"; echo "----- response -----"; cat "$2" 2>/dev/null; echo; exit 1; }
json() { python3 -c "import sys, json; d = json.load(sys.stdin); print($1)" < "$2"; }

echo "smoke test against $BASE as $USER"

# 1. health: every component we depend on must be UP
curl -fsS "$BASE/actuator/health" -o "$TMP/health.json" || fail "health endpoint unreachable" "$TMP/health.json"
for comp in db modelService; do
  status="$(json "d['components']['$comp']['status']" "$TMP/health.json")"
  [ "$status" = "UP" ] || fail "health component $comp is $status" "$TMP/health.json"
done
pass "health: db and modelService UP"

# 2. single prediction (known CASH_OUT fraud pattern)
code="$(curl -sS -o "$TMP/single.json" -w '%{http_code}' -u "$USER:$PASS" \
  -H 'Content-Type: application/json' "$BASE/api/v1/predictions" \
  -d '{"step":100,"typeCode":1,"amount":10000,"oldbalanceOrg":10000,"newbalanceOrig":0,"oldbalanceDest":0,"newbalanceDest":10000}')"
[ "$code" = "200" ] || fail "single prediction returned HTTP $code" "$TMP/single.json"
id="$(json "d['id']" "$TMP/single.json")"
fraud="$(json "d['fraud']" "$TMP/single.json")"
[ "$fraud" = "True" ] || fail "expected fraud=true for the CASH_OUT example" "$TMP/single.json"
pass "single prediction stored as id=$id, fraud=true"

# 3. the stored record is readable back through the history endpoint
curl -fsS -u "$USER:$PASS" "$BASE/api/v1/predictions?size=50" -o "$TMP/history.json" || fail "history unreachable" "$TMP/history.json"
found="$(json "any(p['id'] == $id for p in d['content'])" "$TMP/history.json")"
[ "$found" = "True" ] || fail "id=$id not found in history" "$TMP/history.json"
pass "history contains id=$id (database round-trip)"

# 4. batch: fetch the sample CSV the app itself serves, then upload it
curl -fsS "$BASE/samples/sample_transactions.csv" -o "$TMP/sample.csv" || fail "sample CSV not served" "$TMP/sample.csv"
code="$(curl -sS -o "$TMP/batch.json" -w '%{http_code}' -u "$USER:$PASS" \
  -F "file=@$TMP/sample.csv" "$BASE/api/v1/predictions/batch")"
[ "$code" = "200" ] || fail "batch returned HTTP $code" "$TMP/batch.json"
total="$(json "d['total']" "$TMP/batch.json")"
flagged="$(json "d['fraudCount']" "$TMP/batch.json")"
[ "$total" = "8" ] && [ "$flagged" = "4" ] || fail "expected 8 rows / 4 flagged, got $total / $flagged" "$TMP/batch.json"
pass "batch: 8 rows scored, 4 flagged"

echo "all smoke checks passed"
