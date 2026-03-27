#!/usr/bin/env bash

set -euo pipefail

resolve_base_sha() {
  local before_sha="${EVENT_BEFORE:-}"

  if [[ -n "${before_sha}" && "${before_sha}" != "0000000000000000000000000000000000000000" ]] \
    && git cat-file -e "${before_sha}^{commit}" 2>/dev/null; then
    printf '%s\n' "${before_sha}"
  elif git rev-parse HEAD^ >/dev/null 2>&1; then
    # Force-push/reset 이후 before SHA가 사라진 경우 마지막 커밋 기준으로 축소 비교한다.
    git rev-parse HEAD^
  else
    git rev-list --max-parents=0 HEAD
  fi
}

mark() {
  local name="$1"
  local pattern="$2"

  if grep -Eq "${pattern}" changed_files.txt; then
    echo "${name}=true" >> "${GITHUB_OUTPUT}"
  else
    echo "${name}=false" >> "${GITHUB_OUTPUT}"
  fi
}

base_sha="$(resolve_base_sha)"
git diff --name-only "${base_sha}" "${GITHUB_SHA}" > changed_files.txt

mark server "${SERVER_PATTERN}"
mark web "${WEB_PATTERN}"
mark runner "${RUNNER_PATTERN}"
