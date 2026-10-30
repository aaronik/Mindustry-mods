#!/bin/sh
set -eu
cd "$(dirname "$0")"
JAR="${MINDUSTRY_JAR:-$HOME/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/desktop.jar}"
if [ ! -f "$JAR" ]; then echo "Mindustry JAR not found: $JAR" >&2; exit 1; fi
rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes dist
javac --release 17 -cp "$JAR" -d build/classes src/main/java/*/*.java
javac --release 17 -cp "build/classes:$JAR" -d build/test-classes src/test/java/*/*.java
for test in autodrill.PlannerTest solarpack.SolarPlannerTest batterypack.BatteryPlannerTest wallpack.WallPlannerTest minepack.MinePlannerTest surgeroute.SurgePlannerTest surgeroute.SurgeScenariosTest propcleanup.PropCleanupTest megapriority.PriorityTest; do
    java -ea -cp "build/classes:build/test-classes:$JAR" "$test"
done
jar cf dist/mindustry-tools.jar -C build/classes . mod.hjson
printf 'Built %s\n' "$PWD/dist/mindustry-tools.jar"
