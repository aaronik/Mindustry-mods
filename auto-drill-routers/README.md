# Auto Drill Routers (Mindustry Steam 160.7)

Desktop client mod: queue drills over a connected ore patch and build **one connected, bidirectional router border** along the queued drills. It does not instant-build or route to the core.

## Install

The JAR is `dist/auto-drill-routers.jar`. Copy it into your active Mindustry `mods` directory, or import the JAR through Mindustry's Mods menu, then restart. On this Mac's Steam install, the active directory is:

`~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/`

## Use

1. Select an unlocked drill in the construction menu. If no drill is selected, the tool uses the pneumatic drill (which must be unlocked).
2. Click **Auto-drill** at the bottom of the screen, just left of the build menu. The button highlights, and a toast tells you the chosen drill.
3. Click an ore tile. The mod queues drills and a connected router border around the drills, including lanes for interior drills. Add your own conveyors along any reachable outer edge.
4. Click **Auto-drill** again before clicking ore to cancel. There is no keyboard shortcut.

Does not replace existing blocks or the local player's queued builds. It skips ore that can't be mined, isn't placeable, or would produce mostly another ore. To reach interior ore, it tries horizontal/vertical router-lane reservations at different offsets, chooses the layout with greatest drill-covered ore (then fewest routers), and skips drills that cannot connect to the shared network. Deposits are scanned up to 1,024 tiles; lane search has an approximate 350 ms click-time budget, so large/irregular layouts may be suboptimal. Every reachable, placeable side **and corner** tile bordering a queued drill becomes a router, with cardinally connected corner bridges and paths where needed. Existing buildings or impassable terrain can prevent a complete ring; isolated border pockets are omitted. This intentionally uses more routers than a minimum backbone. The **maximum possible ore coverage is not guaranteed**. Router-to-router item transfer is bidirectional by game rules, though router loops and heavy throughput can cause congestion or backflow. This does not guarantee a builder can finish the queue. No preview or undo UI is provided; use Mindustry's normal plan-cancel controls if needed.

## Build and test

Run `./build.sh` with a Java 17+ compiler. The script compiles against this Mac's installed Mindustry desktop JAR and runs tile-planner assertions before packaging. Override the dependency path with `MINDUSTRY_JAR=/path/to/desktop.jar ./build.sh`.

Tests cover dense-field interior packing, occupied tiles, overlapping drills, connected router borders, drill access, blocked corridors, non-ore clicks, patch limits, and randomized obstacles. Actual in-game UI/building behavior has **not** been manually exercised.
