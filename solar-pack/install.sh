#!/bin/sh
set -eu
cd "$(dirname "$0")"
./build.sh
MODS="$HOME/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods"
mkdir -p "$MODS"
DEST="$MODS/solar-pack.jar"
TMP="$MODS/.solar-pack.jar.$$"
trap 'rm -f "$TMP"' EXIT INT TERM
cp dist/solar-pack.jar "$TMP"
for entry in solarpack/SolarPackMod.class solarpack/SolarPlanner.class 'solarpack/SolarPlanner$Grid.class' 'solarpack/SolarPlanner$Layout.class' 'solarpack/SolarPlanner$Point.class'; do
    unzip -Z1 "$TMP" | grep -Fx "$entry" >/dev/null || { echo "Missing: $entry" >&2; exit 1; }
done
unzip -t "$TMP" | tail -1
mv -f "$TMP" "$DEST" # atomic rename on the same volume
trap - EXIT INT TERM
printf 'Installed %s. Fully quit and restart Mindustry before using.\n' "$DEST"
