#!/usr/bin/env bash
# Prints the version declared in the source tree: the <revision> property of the
# root pom.xml, which every module uses as its version. Run from the repository root.
set -euo pipefail

version=$(sed -n 's:.*<revision>\(.*\)</revision>.*:\1:p' pom.xml | head -n 1)
if [[ -z "$version" ]]; then
  echo "No <revision> property found in pom.xml" >&2
  exit 1
fi
echo "$version"
