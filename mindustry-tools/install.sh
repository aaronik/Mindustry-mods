#!/bin/sh
set -eu
cd "$(dirname "$0")"
./build.sh
MODS="${MINDUSTRY_MODS:-$HOME/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods}"
mkdir -p "$MODS"
TMP="$MODS/.mindustry-tools.$$.jar"
trap 'rm -f "$TMP"' EXIT INT TERM
cp dist/mindustry-tools.jar "$TMP"
unzip -t "$TMP" | tail -1
for entry in mindustrytools/MindustryToolsMod.class autodrill/Planner.class solarpack/SolarPlanner.class batterypack/BatteryPlanner.class wallpack/WallPlanner.class megapriority/Priority.class; do
    unzip -Z1 "$TMP" | grep -Fx "$entry" >/dev/null || { echo "Missing class: $entry" >&2; exit 1; }
done
# Legacy JARs cannot coexist with this mod: they would duplicate the UI and inputs.
BACKUP="$MODS/legacy-mindustry-mods-backup"
mkdir -p "$BACKUP"
for legacy in auto-drill-routers solar-pack battery-pack wall-pack mega-priority; do
    if [ -f "$MODS/$legacy.jar" ]; then
        mv "$MODS/$legacy.jar" "$BACKUP/$legacy.jar"
    fi
done
mv -f "$TMP" "$MODS/mindustry-tools.jar"
trap - EXIT INT TERM
printf 'Installed %s; prior JARs, if present, moved to %s\n' "$MODS/mindustry-tools.jar" "$BACKUP"
printf 'Fully quit and restart Mindustry before using.\n'
