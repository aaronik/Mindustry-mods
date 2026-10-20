# Mindustry-mods

Four independent **desktop Mindustry** mods: three construction planners and a Mega command-switching tool. Tested by compiling against the Steam **160.7** game JAR and running automated tests. The planners queue normal construction plans; they do not instantly build anything. In-game UI and behavior are not covered by automated tests.

| Mod | What it does |
| --- | --- |
| [Auto Drill Routers](auto-drill-routers/) | Click an ore deposit to queue drills and a connected router border; defaults to the pneumatic drill. |
| [Solar-pack](solar-pack/) | Drag a rectangle to pack large solar panels, then fill legal gaps with small batteries. Supports up to 200×200 tiles. |
| [Wall-pack](wall-pack/) | Drag a rectangle to pack large walls, then fill legal gaps with small walls of the selected material. |
| [Mega Priority](mega-priority/) | Opt selected Megas into vanilla Rebuild → Repair → Mine command switching. |

## Install (no compilation needed)

1. Download the three construction mods from the [latest stable release](https://github.com/Aaronik/Mindustry-mods/releases/latest). Mega Priority is an [experimental prerelease](https://github.com/Aaronik/Mindustry-mods/releases/tag/mega-priority-v0.1.4). **Download individual JAR assets**, not the GitHub “Source code” ZIP.
2. In Mindustry, open **Mods → Import Mod → Import File** and choose each JAR. Alternatively, copy each JAR into your active Mindustry `mods` folder. On the Steam macOS installation used to develop these mods, it is:

   ```text
   ~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/
   ```

   Other installations may use a different data directory; using the game's import option is safer.
3. **Fully quit and restart Mindustry** after installing or updating. Do not replace a mod JAR while Mindustry is running: the game may fail to load helper classes later.
4. Load a map. The three construction buttons appear at the bottom, just left of the build menu, aligned on their right edges: **Wall-pack**, **Solar-pack**, **Auto-drill** (top to bottom).

For Mega Priority, select friendly Megas using the unit command controls and click **Mega priority** above Wall-pack. Opt-in resets when you change maps.

Select a drill in the build menu before Auto-drill if you want something other than pneumatic. For Wall-pack, select a small or large wall to choose its material (default copper). Solar-pack requires large solar panels and small batteries to be unlocked. Click a button to arm its tool; click ore for Auto-drill, or drag a map rectangle for the other two. Click an armed button again to cancel.

**Update:** Quit the game completely, replace/import the newer JAR(s), and restart. Uninstall by disabling the mod in the Mods menu or deleting its JAR and restarting.

## Build from source

Install a Java 17+ JDK and Mindustry desktop 160.7. Run:

```sh
MINDUSTRY_JAR=/path/to/desktop.jar ./build-all.sh
```

If you have the same Steam macOS installation path shown above, `./build-all.sh` finds the game JAR automatically. Tests run as part of each build. Output files are `auto-drill-routers/dist/auto-drill-routers.jar`, `solar-pack/dist/solar-pack.jar`, `wall-pack/dist/wall-pack.jar`, and `mega-priority/dist/mega-priority.jar`. Each mod has its own instructions: [Auto Drill](auto-drill-routers/README.md), [Solar-pack](solar-pack/README.md), [Wall-pack](wall-pack/README.md), and [Mega Priority](mega-priority/README.md).

These are separate mods in one repository; install whichever ones you want. They are intended for the Steam desktop build and have not been validated on Android or other game versions.
