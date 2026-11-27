#!/bin/sh
# Usage: check-subtree-commits.sh <base> <head>
# Fails when a non-merge commit between <base> and <head> changes an OSS subtree directly.
# The subtree commits of "git subtree add/pull/merge" carry a "git-subtree-dir:" line and are allowed.
# Used by the PR check workflow (the pre-commit hook checks the staged files instead).

BASE="$1"
HEAD="$2"

if [ -z "$BASE" ] || [ -z "$HEAD" ]; then
  echo "Usage: $0 <base> <head>"
  exit 2
fi

PROTECTED_PATHS="mago-common mago-io"

FAILED=0

for COMMIT in $(git rev-list --no-merges "$BASE..$HEAD" -- $PROTECTED_PATHS); do
  if git log -1 --format=%B "$COMMIT" | grep -q '^git-subtree-dir:'; then
    continue
  fi
  FILES=$(git diff-tree --no-commit-id --name-only --no-renames -r "$COMMIT" -- $PROTECTED_PATHS)
  echo "ERROR: $(git log -1 --format='%h %s' "$COMMIT") changes an OSS subtree directly ($(echo "$FILES" | wc -l | tr -d ' ') files):"
  echo "$FILES" | head -n 10 | sed 's/^/  /'
  FAILED=1
done

if [ "$FAILED" = "1" ]; then
  echo ""
  echo "Make changes in the standalone mago-common / mago-io repository, then sync with git subtree pull."
  echo "Only for an intentional subtree change (e.g. removing a subtree),"
  echo "add a \"git-subtree-dir: <directory>\" line to the commit message."
  exit 1
fi

exit 0
