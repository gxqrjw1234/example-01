#!/bin/sh
set -eu

DIRECTORY="${1:-.}"
COUNT="$(find "$DIRECTORY" -type f | wc -l | tr -d ' ')"

echo "File count in $DIRECTORY: $COUNT"
echo "file-count=$COUNT" >> "$GITHUB_OUTPUT"
