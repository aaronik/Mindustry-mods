#!/bin/sh
set -eu
cd "$(dirname "$0")"
for mod in auto-drill-routers solar-pack battery-pack wall-pack mega-priority; do
    printf '\n== %s ==\n' "$mod"
    (cd "$mod" && ./build.sh)
done
