#!/bin/sh
# Reads file paths (one per line) from stdin and fails when one of them is a local, IDE or AI agent file.
# Used by the pre-commit hook and the PR check workflow.

FORBIDDEN_PATTERN='(^|/)(\.claude|\.agents?|\.cursor|\.codex|\.gemini|\.aider[^/]*|\.continue|\.copilot|\.idea|\.vscode|\.gradle|\.gradle-home)(/|$)|(^|/)(CLAUDE|CLAUDE\.local|AGENTS|GEMINI)\.md$|\.local\.json$|\.stackdump$'

FORBIDDEN_FILES=$(grep -E "$FORBIDDEN_PATTERN")

if [ -n "$FORBIDDEN_FILES" ]; then
  echo ""
  echo "ERROR: Local, IDE or AI agent files cannot be committed:"
  echo ""
  echo "$FORBIDDEN_FILES" | sed 's/^/  /'
  echo ""
  echo "Remove them from the index with:"
  echo ""
  echo "  git rm --cached <file>"
  echo ""
  exit 1
fi

exit 0
