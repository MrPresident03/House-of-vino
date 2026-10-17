#!/usr/bin/env bash
# Commits a readable summary of this CI job (failed tests, compiler errors, log tail) to the
# `ci-results` branch, so results can be reviewed with plain git as well as in the Actions UI.
# Usage: publish-results.sh <job-name> <job-status> <log-file>
set -uo pipefail
JOB="$1"; STATUS="$2"; LOG="${3:-ci.log}"
[ -f "$LOG" ] || echo "(no log produced)" > "$LOG"
SHORT="${GITHUB_SHA:0:7}"
DIR="runs/${GITHUB_RUN_NUMBER}-${SHORT}/${JOB}"
WORK="$(mktemp -d)"
REMOTE="${CI_RESULTS_REMOTE:-https://x-access-token:${GH_TOKEN:-}@github.com/${GITHUB_REPOSITORY:-}.git}"

git -C "$WORK" init -q
git -C "$WORK" config user.name "ci-results"
git -C "$WORK" config user.email "ci-results@users.noreply.github.com"
git -C "$WORK" remote add origin "$REMOTE"
if git -C "$WORK" fetch -q --depth 20 origin ci-results 2>/dev/null; then
  git -C "$WORK" checkout -q -b ci-results FETCH_HEAD
else
  git -C "$WORK" checkout -q --orphan ci-results
fi
mkdir -p "$WORK/$DIR"

python3 - "$LOG" "$WORK/$DIR" "$JOB" "$STATUS" <<'PY'
import glob, os, re, sys, xml.etree.ElementTree as ET
log, out, job, status = sys.argv[1:5]
text = open(log, errors="replace").read()
lines = text.splitlines()
total = failed = skipped = 0
failures = []
for path in glob.glob("**/build/test-results/**/*.xml", recursive=True):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for case in root.iter("testcase"):
        total += 1
        if case.find("skipped") is not None:
            skipped += 1
        bad = case.find("failure") if case.find("failure") is not None else case.find("error")
        if bad is not None:
            failed += 1
            detail = (bad.get("message") or "") + "\n" + "\n".join((bad.text or "").splitlines()[:25])
            failures.append(f"### {case.get('classname')}.{case.get('name')}\n```\n{detail.strip()}\n```")
compile_errors = [l for l in lines if re.match(r"^e: ", l) or " error: " in l or l.startswith("error:")]
gradle_failures = []
for i, l in enumerate(lines):
    if "What went wrong" in l:
        gradle_failures.append("\n".join(lines[i:i + 8]))
with open(os.path.join(out, "summary.md"), "w") as f:
    f.write(f"# {job}: {status}\n\n")
    f.write(f"- commit: {os.environ.get('GITHUB_SHA')}\n")
    f.write(f"- run: {os.environ.get('GITHUB_SERVER_URL')}/{os.environ.get('GITHUB_REPOSITORY')}/actions/runs/{os.environ.get('GITHUB_RUN_ID')}\n")
    f.write(f"- tests: {total} run, {failed} failed, {skipped} skipped\n\n")
    if compile_errors:
        f.write(f"## Compiler errors ({len(compile_errors)})\n```\n" + "\n".join(compile_errors[:400]) + "\n```\n\n")
    if gradle_failures:
        f.write("## Gradle failures\n```\n" + "\n---\n".join(gradle_failures[:20]) + "\n```\n\n")
    if failures:
        f.write(f"## Failed tests ({failed})\n\n" + "\n\n".join(failures[:100]) + "\n")
with open(os.path.join(out, "log-tail.txt"), "w") as f:
    f.write("\n".join(lines[-800:]) + "\n")
PY
gzip -c "$LOG" > "$WORK/$DIR/full-log.txt.gz"
cp "$WORK/$DIR/summary.md" "$WORK/latest-${JOB}.md"

git -C "$WORK" add -A
git -C "$WORK" commit -q -m "CI ${JOB} ${STATUS} for ${SHORT} (run ${GITHUB_RUN_NUMBER})"
for attempt in 1 2 3 4 5; do
  if git -C "$WORK" push -q origin HEAD:ci-results; then echo "Published to ci-results:$DIR"; exit 0; fi
  sleep $((attempt * 3))
  git -C "$WORK" fetch -q --depth 20 origin ci-results && git -C "$WORK" rebase -q FETCH_HEAD || true
done
echo "Couldn't publish results (the job result itself is unaffected)."
exit 0
