# Mindustry Tools

One mod for **Mindustry Steam 160.7** with a compact **Tools** submenu. It combines five previously separate tools:

| Tool | What it does |
| --- | --- |
| Auto-drill | Queues drills over an ore patch with a connected router border; defaults to pneumatic drills. |
| Solar-pack | Packs 3×3 large solar panels in a dragged rectangle, then fills legal gaps with small batteries. |
| Battery-pack | Packs 3×3 large batteries, then fills legal gaps with small batteries. |
| Wall-pack | Packs 2×2 large walls of the selected material, then fills legal gaps with small walls. |
| Mega priority | Opts selected Megas into vanilla Rebuild → Repair → Mine commands. |

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

Click **Tools** beside the bottom-right build menu. Its submenu contains **Mega priority**, **Pack**, and **Auto-drill**. Click **Pack** to expand **Wall-pack**, **Battery-pack**, and **Solar-pack** above it. Click either parent button again to collapse its submenu. An armed tool remains armed if the menu is collapsed; select it again to cancel. Choosing another construction tool disarms the previous one.

* **Auto-drill:** Select a drill first (or use the unlocked pneumatic fallback), select Auto-drill, then click an ore patch.
* **Solar-pack / Battery-pack / Wall-pack:** Select the tool, then drag out a rectangular area. Select a wall type first if you want a material other than copper.
* **Mega priority:** Select friendly Megas in command mode and choose Mega priority. Choosing a vanilla command disables automation for those selected Megas. This opt-in resets when a new map loads.

The construction tools avoid existing buildings and your queued plans, but can build over removable natural props. Solar-pack and Battery-pack have a 200×200-tile selection limit; Wall-pack has a 60-tile-per-side, 2,500-tile-area limit. Tools do not instantly construct blocks. Success and cancellation are silent; errors show toasts.

## Build and test

Install a Java 17+ JDK and obtain the Mindustry 160.7 desktop JAR, then run:

```sh
cd mindustry-tools
MINDUSTRY_JAR=/path/to/desktop.jar ./build.sh
```

On the Steam macOS installation above, `./build.sh` finds `desktop.jar` automatically. It compiles the combined mod and runs all five existing planner/priority test suites. The output is `mindustry-tools/dist/mindustry-tools.jar`. **The combined submenu has not yet been manually tested in-game.**

The former standalone projects remain in this repository under their respective directories as legacy source/history; they should **not** be installed alongside Mindustry Tools. See [combined mod details](mindustry-tools/README.md).
