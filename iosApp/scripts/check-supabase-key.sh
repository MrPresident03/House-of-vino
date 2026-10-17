#!/bin/sh
# Fails the Xcode build when SUPABASE_ANON_KEY is not a publishable key (mirrors core's KeyGuard).
set -eu
KEY="${SUPABASE_ANON_KEY:-}"
[ -z "$KEY" ] && exit 0
case "$KEY" in
  sb_publishable_*) exit 0 ;;
  sb_secret_*) echo "error: SUPABASE_ANON_KEY is a secret key. Never ship it in the app."; exit 1 ;;
esac
PARTS=$(printf '%s' "$KEY" | awk -F. '{print NF}')
if [ "$PARTS" != "3" ]; then
  echo "error: SUPABASE_ANON_KEY has an unrecognised format; refusing to embed it."; exit 1
fi
PAYLOAD=$(printf '%s' "$KEY" | cut -d. -f2 | tr '_-' '/+')
while [ $(( ${#PAYLOAD} % 4 )) -ne 0 ]; do PAYLOAD="${PAYLOAD}="; done
ROLE=$(printf '%s' "$PAYLOAD" | base64 -d 2>/dev/null | sed -n 's/.*"role"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
case "$ROLE" in
  anon|authenticated) exit 0 ;;
  *) echo "error: SUPABASE_ANON_KEY has role '${ROLE:-unknown}'. Only the anon/publishable key may be embedded."; exit 1 ;;
esac
