# Mindustry Tools

One mod for **Mindustry Steam 160.7** with a compact **Pack** submenu. It combines five previously separate tools plus Mine packing:

| Tool | What it does |
| --- | --- |
| Auto-drill | Queues drills over an ore patch with a connected router border; defaults to pneumatic drills. |
| Surge | Click a destination to queue Surge Towers roughly 38 tiles apart, connected with long-range links and preferably beside black map terrain. |
| Solar-pack | Packs 3×3 large solar panels in a dragged rectangle, then fills legal gaps with small batteries. |
| Battery-pack | Packs 3×3 large batteries, then fills legal gaps with small batteries. |
| Wall-pack | Packs 2×2 large walls of the selected material, then fills legal gaps with small walls. |
| Mega priority | Opts selected Megas into vanilla Rebuild → Repair → Mine commands. |
| Mine (Pack submenu) | Densely fills a selected rectangle with 1×1 Shock Mines. |
| Clear props | Queues removal of auto-replaceable natural map props across the whole map. |

## Install

1. Download **`mindustry-tools.jar`** from the [latest release](https://github.com/aaronik/Mindustry-mods/releases/latest). Don't use the GitHub “Source code” ZIP.
2. **Remove or disable all five old standalone mods** first: `auto-drill-routers`, `solar-pack`, `battery-pack`, `wall-pack`, and `mega-priority`. If both the old and combined mods are loaded, duplicate UI/input behavior will result. The old JARs may be kept as backups outside the active mods folder.
3. In Mindustry, use **Mods → Import Mod → Import File** to select the JAR, or copy it into the game's active `mods` directory. On the Steam macOS installation used to develop this mod, that directory is:

   ```text
   ~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/
   ```

4. **Fully quit and restart Mindustry.** Don't overwrite mod JARs while the game is running; delayed Java class loading may fail.

If building/installing locally on the above Steam macOS installation, `cd mindustry-tools && ./install.sh` builds and verifies the combined JAR, moves the five old JARs to `mods/legacy-mindustry-mods-backup/`, and installs the new JAR atomically.

## Use

Attached to the left edge of Mindustry’s bottom-right build/command menu, **Clear props**, **Mega priority**, **Auto-drill**, **Surge**, and **Pack** are always visible (top to bottom). **Pack** is the bottom button and starts closed; click it to show **Wall**, **Battery**, **Solar**, and **Mine** in a single row to its left, and click again to hide them. The main buttons stay in place. An armed packing tool stays armed if Pack is closed; open Pack and select it again to cancel. Choosing another construction tool disarms the previous one.

* **Clear props:** Click to queue removal plans for breakable natural boulders, clumps, and similar auto-replaceable props across the map. The scan is batched and capped at 10,000 new plans per run; click again during scanning to cancel. Existing buildings, black map walls, and your queued construction footprints are protected.
* **Auto-drill:** Select a drill first (or use the unlocked pneumatic fallback), select Auto-drill, then click an ore patch.
* **Surge:** Click Surge, then click a destination on the map. It searches across the whole map in batches, places towers up to about 38 tiles apart with explicit power links, strongly favors black map-wall edges and prunes redundant towers when a direct laser hop is possible, and stops at the first reachable existing power source along the route rather than extending past a powered node. It side-touches a Surge Tower or links from up to about 38 tiles away to a friendly power node, solar panel, or battery. Thousands-tile routes may take several seconds to plan.
* **Solar / Battery / Wall / Mine:** Choose an option under Pack, then drag out a rectangular area. Mine queues Shock Mines; select a wall type first if you want a material other than copper.
* **Mega priority:** Select friendly Megas in command mode and choose Mega priority. Choosing a vanilla command disables automation for those selected Megas. This opt-in resets when a new map loads.

All four Pack modes (and Auto-drill) avoid existing buildings and your queued plans, but can build over removable natural props. Solar-pack, Battery-pack, and Wall-pack each allow up to 200×200 tiles; Mine remains limited to 60 tiles per side and 2,500 tiles total. A completely open 200×200 Wall selection queues 10,000 large walls, so construction may take time. Tools do not instantly construct blocks. Success and cancellation are silent; errors show toasts.

## Build and test

Install a Java 17+ JDK and obtain the Mindustry 160.7 desktop JAR, then run:

```sh
cd mindustry-tools
MINDUSTRY_JAR=/path/to/desktop.jar ./build.sh
```

On the Steam macOS installation above, `./build.sh` finds `desktop.jar` automatically. It compiles the combined mod and runs the planner/priority tests, including map-like Surge route scenarios for cliffs, props, buildings, insulated links, competing sources, and map edges. The output is `mindustry-tools/dist/mindustry-tools.jar`. **The combined submenu has not yet been manually tested in-game.**

The former standalone projects remain in this repository under their respective directories as legacy source/history; they should **not** be installed alongside Mindustry Tools. See [combined mod details](mindustry-tools/README.md).
