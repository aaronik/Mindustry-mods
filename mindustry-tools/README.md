# Mindustry Tools

One desktop mod combining Auto-drill, Surge routing, Solar-pack, Battery-pack, Wall-pack, Mine-pack, and Mega Priority for Mindustry Steam 160.7.

## Install

Download `mindustry-tools.jar` from the [latest release](https://github.com/Aaronik/Mindustry-mods/releases/latest). **Remove or disable the old standalone mods first** (`auto-drill-routers.jar`, `solar-pack.jar`, `battery-pack.jar`, `wall-pack.jar`, `mega-priority.jar`) or you'll get duplicate controls and input handlers. Then import the JAR from Mindustry's Mods screen or copy it to the game's `mods` directory. Fully quit and restart Mindustry before using it. On the Steam macOS install used for testing, mods are stored at:

```text
~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/
```

If building locally on that machine, `./install.sh` compiles, verifies the JAR, moves standalone JARs into `mods/legacy-mindustry-mods-backup/`, and installs the combined JAR atomically. Set `MINDUSTRY_JAR=/path/to/desktop.jar` or `MINDUSTRY_MODS=/path/to/mods` if needed. Do not update JARs while Mindustry is running; fully restart it afterward.

## Use

**Clear props**, **Mega priority**, **Auto-drill**, **Surge**, and **Pack** (top to bottom) are always visible just left of the build/command menu, following its live width. **Pack** is the bottom button, the only submenu, and starts closed; click it to reveal **Wall**, **Battery**, **Solar**, and **Mine** in a single row to its left, aligned with Pack, without moving the main buttons. Click Pack again to close it. An active tool is highlighted. Choosing a different construction tool disarms the previous one. Closing Pack does not cancel an armed tool; reopen it and click that tool again to cancel.

* **Clear props:** Click to scan the entire map and queue normal removal plans for breakable auto-replaceable natural props. Buildings, black/static walls, and already queued construction footprints are skipped. Up to 10,000 new removal plans per run, queued in batches; click again while scanning to stop adding plans. Previously queued removals are not undone.
* **Auto-drill:** Select a drill first (defaults to unlocked pneumatic drill), arm it, then click an ore patch. Queues drills plus a connected router border.
* **Surge:** Click Surge and then a map destination. It searches incrementally across the map (including routes thousands of tiles long) and queues a long-range linked chain of 2×2 Surge Towers. When connecting to an existing Surge Tower, the first new tower touches the side facing your destination when reachable (adjacency works even if its laser links are full); the wall preference applies to the remainder of the route. Otherwise the first new tower may start up to about 38 tiles from a friendly power node, solar panel, or battery, using a laser link; subsequent towers typically use long-range hops. Valid placements beside black/static map terrain are strongly preferred: a longer wall-hugging route can beat a shorter open-field one, but walls are not required. Insulated buildings that block laser links are avoided. It never replaces buildings or queued plans, and allows removable map props. When both endpoints are constructed, the mod requests links to the predecessor even if builders finish out of order (except the first adjacency connection to an existing Surge Tower); it does not connect to other power-block types. Very long routes may take several seconds to plan; plans appear when the search finishes. Click Surge again while a search is running to cancel it. Builders must still reach and construct the queued towers.
* **Solar-pack:** Arm, then drag a rectangle. Queues large solar panels, filling remaining legal tiles with small batteries.
* **Battery-pack:** Arm, then drag a rectangle. Queues large batteries, filling gaps with small batteries.
* **Wall-pack:** Select either size of wall for the material (defaults to copper), arm, then drag a rectangle. Queues large walls followed by small-wall infill.
* **Mine:** Arm and drag a rectangle. Queues 1×1 Shock Mines on every valid tile (up to 60 tiles per side and 2,500 tiles per drag).
* **Mega priority:** Select friendly Megas in command mode, then toggle. Uses vanilla Rebuild → Repair → Mine commands based on available work. Selecting a vanilla command opts those Megas out.

All four Pack modes may build over natural props that the game automatically replaces; buildings and your queued plans are not overwritten. Construction modes queue normal build plans. The mod is quiet on successful actions/cancellation; invalid actions show toasts. Mega Priority is opt-in and resets on world load.

## Build/test

Run `./build.sh` with Java 17+ and a Mindustry 160.7 desktop JAR. It compiles and runs all planner/priority suites, including deterministic Surge route fixtures for map edges, cliffs, removable props, buildings, insulated links, competing sources, blocked destinations, long routes, and seeded irregular maps. These are synthetic map-like fixtures, not actual save files. Combined UI and live construction/power-link synchronization still need in-game verification.
