# Mindustry Tools

One desktop mod combining Auto-drill, Surge routing, Solar-pack, Battery-pack, Wall-pack, Mine-pack, and Mega Priority for Mindustry Steam 160.7.

## Install

Download `mindustry-tools.jar` from the [latest release](https://github.com/Aaronik/Mindustry-mods/releases/latest). **Remove or disable the old standalone mods first** (`auto-drill-routers.jar`, `solar-pack.jar`, `battery-pack.jar`, `wall-pack.jar`, `mega-priority.jar`) or you'll get duplicate controls and input handlers. Then import the JAR from Mindustry's Mods screen or copy it to the game's `mods` directory. Fully quit and restart Mindustry before using it. On the Steam macOS install used for testing, mods are stored at:

```text
~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/
```

If building locally on that machine, `./install.sh` compiles, verifies the JAR, moves standalone JARs into `mods/legacy-mindustry-mods-backup/`, and installs the combined JAR atomically. Set `MINDUSTRY_JAR=/path/to/desktop.jar` or `MINDUSTRY_MODS=/path/to/mods` if needed. Do not update JARs while Mindustry is running; fully restart it afterward.

## Use

**Mega priority**, **Auto-drill**, **Surge**, and **Pack** (top to bottom) are always visible just left of the build menu. **Pack** is the bottom button, the only submenu, and starts closed; click it to reveal **Wall**, **Battery**, **Solar**, and **Mine** in a single row to its left, aligned with Pack, without moving the main buttons. Click Pack again to close it. An active tool is highlighted. Choosing a different construction tool disarms the previous one. Closing Pack does not cancel an armed tool; reopen it and click that tool again to cancel.

* **Auto-drill:** Select a drill first (defaults to unlocked pneumatic drill), arm it, then click an ore patch. Queues drills plus a connected router border.
* **Surge:** Click Surge and then a map destination. It searches incrementally across the map (including routes thousands of tiles long) and queues a side-connected chain of 2×2 Surge Towers from the nearest reachable existing friendly Surge Tower; if none is reachable, it starts beside a friendly power node, solar panel, or battery. Valid placements beside black/static map terrain are preferred, not required. It never replaces buildings or queued plans, and allows removable map props. Power connection uses adjacency rather than explicitly configured laser links; it does not connect to other power-block types. Very long routes may take several seconds to plan; plans appear when the search finishes. Click Surge again while a search is running to cancel it. Builders must still reach and construct the queued towers.
* **Solar-pack:** Arm, then drag a rectangle. Queues large solar panels, filling remaining legal tiles with small batteries.
* **Battery-pack:** Arm, then drag a rectangle. Queues large batteries, filling gaps with small batteries.
* **Wall-pack:** Select either size of wall for the material (defaults to copper), arm, then drag a rectangle. Queues large walls followed by small-wall infill.
* **Mine:** Arm and drag a rectangle. Queues 1×1 Shock Mines on every valid tile (up to 60 tiles per side and 2,500 tiles per drag).
* **Mega priority:** Select friendly Megas in command mode, then toggle. Uses vanilla Rebuild → Repair → Mine commands based on available work. Selecting a vanilla command opts those Megas out.

All four Pack modes may build over natural props that the game automatically replaces; buildings and your queued plans are not overwritten. Construction modes queue normal build plans. The mod is quiet on successful actions/cancellation; invalid actions show toasts. Mega Priority is opt-in and resets on world load.

## Build/test

Run `./build.sh` with Java 17+ and a Mindustry 160.7 desktop JAR. It compiles and runs all five existing decision/layout test suites. Combined menu behavior still needs in-game verification.
