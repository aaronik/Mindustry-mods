#!/bin/sh
set -eu
cd "$(dirname "$0")"
JAR="${MINDUSTRY_JAR:-$HOME/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/desktop.jar}"
if [ ! -f "$JAR" ]; then echo "Mindustry JAR not found: $JAR" >&2; exit 1; fi
mkdir -p build/classes build/test-classes dist
javac --release 17 -cp "$JAR" -d build/classes src/main/java/megapriority/*.java
javac --release 17 -cp "build/classes:$JAR" -d build/test-classes src/test/java/megapriority/*.java
java -ea -cp "build/classes:build/test-classes:$JAR" megapriority.PriorityTest
jar cf dist/mega-priority.jar -C build/classes . mod.hjson
printf 'Built %s\n' "$PWD/dist/mega-priority.jar"
