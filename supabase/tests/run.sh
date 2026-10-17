#!/usr/bin/env bash
# Applies the Supabase migrations to a scratch PostgreSQL database (with a minimal local stand-in
# for Supabase's auth/storage schemas) and runs the server contract tests.
#
# Usage: supabase/tests/run.sh [psql connection args]
#   e.g. supabase/tests/run.sh -h localhost -p 5432 -U postgres
set -euo pipefail
cd "$(dirname "$0")/.."
DB="hov_test_$$"
PSQL=(psql -X -q -v ON_ERROR_STOP=1 "$@")

"${PSQL[@]}" -d postgres -c "DROP DATABASE IF EXISTS $DB" -c "CREATE DATABASE $DB"
trap '"${PSQL[@]}" -d postgres -c "DROP DATABASE IF EXISTS $DB" >/dev/null 2>&1 || true' EXIT

"${PSQL[@]}" -d "$DB" -f tests/00_local_supabase_stub.sql
for m in migrations/*.sql; do
  echo "applying $m"
  "${PSQL[@]}" -d "$DB" -f "$m"
done
"${PSQL[@]}" -d "$DB" -f tests/10_security_and_sync.sql 2>&1 | tee /tmp/hov-server-tests.log
grep -q "ALL SERVER TESTS PASSED" /tmp/hov-server-tests.log
echo "passed: $(grep -c 'ok:' /tmp/hov-server-tests.log) assertions"
