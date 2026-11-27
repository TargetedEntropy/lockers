#!/usr/bin/env bash
# Print the CHANGELOG.md section for one version, for use as release notes.
#
#   scripts/release-notes.sh <mod_version> <owner/repo> > RELEASE_NOTES.md
#
# Prints from the "## [<mod_version>]" heading up to (not including) the next
# "## [" heading. If the version has no section, prints a one-line pointer to
# the full changelog instead, so a release never goes out with empty notes.
set -euo pipefail

version="${1:?usage: release-notes.sh <mod_version> <owner/repo>}"
repo="${2:?usage: release-notes.sh <mod_version> <owner/repo>}"
changelog="$(dirname "$0")/../CHANGELOG.md"

notes="$(awk -v marker="## [$version]" '
  in_section && /^## \[/ { exit }
  index($0, marker) == 1 { in_section = 1 }
  in_section { print }
' "$changelog")"

if [ -n "$notes" ]; then
  printf '%s\n' "$notes"
else
  printf '## %s\n\nAutomated release. See [CHANGELOG.md](https://github.com/%s/blob/main/CHANGELOG.md) for full notes.\n' \
    "$version" "$repo"
fi
