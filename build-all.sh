#!/bin/sh
set -eu
cd "$(dirname "$0")"
for mod in auto-drill-routers solar-pack wall-pack; do
    printf '\n== %s ==\n' "$mod"
    (cd "$mod" && ./build.sh)
done
