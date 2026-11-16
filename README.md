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
| Water (Pack submenu) | Click a water patch to queue Mechanical Pumps and a connected Liquid Router border. |
| Clear props | Queues removal of auto-replaceable natural map props across the whole map. |
| Auto-wave | Automatically sends each next wave when Mindustry enables its wave button. |
| Redir pads | Redirects all exporting sectors on this planet to the current campaign sector in one click. |

## Install

1. Download **`mindustry-tools.jar`** from the [latest release](https://github.com/aaronik/Mindustry-mods/releases/latest). Don't use the GitHub “Source code” ZIP.
2. **Remove or disable all five old standalone mods** first: `auto-drill-routers`, `solar-pack`, `battery-pack`, `wall-pack`, and `mega-priority`. If both the old and combined mods are loaded, duplicate UI/input behavior will result. The old JARs may be kept as backups outside the active mods folder.
3. In Mindustry, use **Mods → Import Mod → Import File** to select the JAR, or copy it into the game's active `mods` directory. On the Steam macOS installation used to develop this mod, that directory is:

   ```text
   ~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/
   ```

4. **Fully quit and restart Mindustry after installation** to load the update. The local `install.sh` workflow can atomically replace the JAR while the game is running; this is not hot reload. Avoid in-place writes to a loaded JAR. If the running session encounters class-loading errors, fully restart.

If building/installing locally on the above Steam macOS installation, `cd mindustry-tools && ./install.sh` builds and verifies the combined JAR, moves the five old JARs to `mods/legacy-mindustry-mods-backup/`, and installs the new JAR atomically.

## Use

**Power readout (no button):** A compact panel beside the core resources lists each distinct power network on your team in the current map, largest first. Each network occupies exactly one line: Net power per second and Battery stored/capacity. No header, IDs, building counts, generation/demand breakdown, or locations. Click a line to highlight that network's on-screen buildings with selection outlines, click again to clear, or another line to switch. No camera movement. Selection clears on map load/reset or if its graph disappears after reconnection. Inert/node-only graphs with no producers, consumers, or batteries are omitted; genuinely disconnected consumers remain visible even when idle. Surplus is green, deficit red. Refreshes twice per real second, including at accelerated speed; rates use vanilla scaled statistics rather than multiplying by the speed preset. Separate networks are never summed together. Scroll when many groups exist; placement falls back beside/below resources if space is tight. Graph IDs may change when networks merge/split. Hides with core resources, HUD, or dialogs. Live layout still needs in-game verification.

**Enemy hover info (no button):** Hover a currently visible enemy unit or building for a click-through tooltip beside the cursor, drawn above HUD buttons. Shows name/team, current and maximum health with percentage, armor, unit shields when present, and weapon range in tiles where applicable. Hovered enemy turrets also draw a vanilla-style dashed range circle in their team color, using the same range as the numeric readout. Non-turret buildings do not get a weapon-range circle. Turrets also show ammo readiness/type and powered buildings show power satisfaction. Units take priority over buildings underneath. Friendly/derelict targets, fog-hidden enemies, menus, and UI controls are excluded. The overlay is always available; no toggle or extra mod is needed. Live HUD behavior still needs in-game verification.

Attached to the left edge of Mindustry’s bottom-right build/command menu, the **2 / 5 / 100** speed row, **Auto-wave**, **Sectors**, **Redir pads**, **Clear props**, **Unit HP**, **Mega priority**, **Surge**, and **Pack** are always visible (top to bottom). **Pack** is the bottom button and starts closed; click it to show **Wall**, **Battery**, **Solar**, **Mine**, **Water**, and **Drill** in a single row to its left, and click again to hide them. The main buttons stay in place. An armed packing tool stays armed if Pack is closed; open Pack and select it again to cancel. Choosing another construction tool disarms the previous one.

* **2 / 5 / 100:** Three adjacent buttons without an “x”, together occupying the original speed button's exact 175×45 UI-unit area and position (widths 45/45/85, no gaps). Select 2×, 5×, or 100× simulation speed in local single-player; selecting another switches directly, and clicking the selected speed restores normal speed with no button highlighted. It starts off and resets on map load, reset, or multiplayer entry. Normal pause still works; UI timing remains normal. Enemies and wave timers accelerate too. This is experimental: per-frame limits mean production/combat may not run at exactly 100×, and large time steps can behave poorly in combat. Back up important saves before using it. It does not accelerate the entire offline campaign clock.
* **Auto-wave:** Toggle on to send the next wave each time Mindustry permits it (no remaining enemies or active spawns). Toggle off to stop; it resets to off on map load. It waits at least one second between requests and respects server permissions.
* **Sectors:** Opens a read-only list of bases on the current planet, sorted by recorded total exports/min (highest first). Search by name or sector number, select an exported item to rank by that item's rate, switch to name sorting, or Refresh the snapshot. Each row shows the overall total plus every exported item's icon, name, and recorded rate/min, highest first, along with the destination and under-attack/current-sector marker. Item filtering changes the ranking but keeps the full breakdown visible. Click a row to locate/select that sector on the planet map without launching or loading it. Zero-export bases remain listed at the bottom. Rates are recorded campaign estimates, not guaranteed sustainable/live throughput. Requires a campaign and is unavailable to multiplayer clients; live dialog/navigation still needs in-game verification.
* **Redir pads:** In a local campaign sector, click once to redirect all other exporting sectors on the current planet to send their exports here. It saves their destinations and refreshes import rates. Not available from a multiplayer client or when legacy launch pads are enabled.
* **Clear props:** Click to queue removal plans for breakable natural boulders, clumps, and similar auto-replaceable props across the map. The scan is batched and capped at 10,000 new plans per run; click again during scanning to cancel. Existing buildings, black map walls, and your queued construction footprints are protected.
* **Drill (Pack submenu):** Select a drill first (or use the unlocked pneumatic fallback), select Drill, then click an ore patch.
* **Surge:** Click Surge, then click a destination on the map. It searches across the whole map in batches, places towers up to about 38 tiles apart with explicit power links, strongly favors black map-wall edges and prunes redundant towers when a direct laser hop is possible, and stops at the first reachable existing power source along the route rather than extending past a powered node. It side-touches a Surge Tower or links from up to about 38 tiles away to a friendly power node, solar panel, or battery. Thousands-tile routes may take several seconds to plan.
* **Solar / Battery / Wall / Mine:** Choose an option under Pack, then drag out a rectangular area. Mine queues Shock Mines; select a wall type first if you want a material other than copper.
* **Water:** Choose it under Pack, then click a water tile. It queues Mechanical Pumps in repeating two-pump-row / one-Liquid-Router-row strips (or rotated columns), with a connected router border. Irregular terrain and obstacles may reduce coverage.
* **Unit HP:** Toggle health bars above visible friendly and enemy units, including your own unit, with a separate shield bar for shielded units. Fog-hidden enemies are excluded. Starts off and resets off on map load/reset. Force-field capacity uses the unit ability's maximum; externally granted shields use the highest value observed while the overlay is visible, since the game exposes no universal maximum. Shield depletion/cooldown shows empty. No changes to gameplay; live rendering still needs in-game verification.
* **Mega priority:** Select friendly Megas in command mode and choose Mega priority. Choosing a vanilla command disables automation for those selected Megas. This opt-in resets when a new map loads.

All six Pack modes avoid existing buildings and your queued plans, but can build over removable natural props. Solar-pack, Battery-pack, and Wall-pack each allow up to 200×200 tiles; Mine remains limited to 60 tiles per side and 2,500 tiles total. A completely open 200×200 Wall selection queues 10,000 large walls, so construction may take time. Tools do not instantly construct blocks. Success and cancellation are silent; errors show toasts.

## Build and test

Install a Java 17+ JDK and obtain the Mindustry 160.7 desktop JAR, then run:

```sh
cd mindustry-tools
MINDUSTRY_JAR=/path/to/desktop.jar ./build.sh
```

On the Steam macOS installation above, `./build.sh` finds `desktop.jar` automatically. It compiles the combined mod and runs the planner/priority tests, including map-like Surge route scenarios for cliffs, props, buildings, insulated links, competing sources, and map edges. The output is `mindustry-tools/dist/mindustry-tools.jar`. **The combined submenu has not yet been manually tested in-game.**

The former standalone projects remain in this repository under their respective directories as legacy source/history; they should **not** be installed alongside Mindustry Tools. See [combined mod details](mindustry-tools/README.md).
